package com.baolian.utils.excel;

import com.baolian.utils.excel.factory.WorkbookFactory;
import com.baolian.utils.excel.factory.impl.HSSFWorkbookFactory;
import com.baolian.utils.excel.factory.impl.XSSFWorkbookFactory;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.ss.usermodel.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by tomxie on 2017/4/8 13:15.
 */
public class ExcelUtils {
    // 对应xls文件和xlsx的工厂类集合
    private static List<WorkbookFactory<? extends Workbook, InputStream>> workbookFactories = new ArrayList<>();

    static {
        workbookFactories.add(new HSSFWorkbookFactory());
        workbookFactories.add(new XSSFWorkbookFactory());
    }

    /**
     * 将excel文件转换成集合
     *
     * @param inputStream 输入文件流
     * @param type        输入文件类型
     * @param entity      实体类
     * @return 实体类集合
     * @throws IOException 异常
     */
    public static Collection readXls(InputStream inputStream, String type, Class entity) throws IOException {
        // InputStream is = new FileInputStream(path + fileName);//EXCEL_PATH存放路径
        Collection dist = new ArrayList<Object>();
        try {

            // 得到目标目标类的所有的字段列表
            Field fields[] = entity.getDeclaredFields();
            Map<String, Method> fieldSetMap = new HashMap<String, Method>();
            for (Field f : fields) {
                // 去除由序列化导致的serialVersionUID
                if (!isFieldFinal(f)) {
                    // 构造Setter方法
                    String fieldName = f.getName();
                    String setMethodName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                    // 构造调用的method
                    Method setMethod = entity.getMethod(setMethodName, f.getType());
                    // 将这个method以field名字的小写为key来存入
                    fieldSetMap.put(fieldName.toLowerCase(), setMethod);
                }
            }
            Workbook book;
            switch (type) {
                case "xls":
                    book = workbookFactories.get(0).create(inputStream);
                    break;
                case "xlsx":
                    book = workbookFactories.get(1).create(inputStream);
                    break;
                default:
                    return null;
            }
            // // 得到工作表
            // HSSFWorkbook book = new HSSFWorkbook(inputStream);
            // 得到第一页
            Sheet sheet = book.getSheetAt(0);
            // 得到第一面的所有行
            Iterator<Row> row = sheet.rowIterator();
            // 得到第一行，也就是标题行
            Row title = row.next();
            // 得到第一行的所有列
            Iterator<Cell> cellTitle = title.cellIterator();
            // 将标题的文字内容放入到一个map中。
            Map<Integer, String> titleMap = new HashMap<Integer, String>();
            // 从标题第一列开始
            int i = 0;
            // 循环标题所有的列
            while (cellTitle.hasNext()) {
                Cell cell = cellTitle.next();
                String value = cell.getStringCellValue();
                titleMap.put(i, value);
                i = i + 1;
            }
            while (row.hasNext()) {
                // 标题下的第一行
                Row rown = row.next();
                // 行的所有列
                Iterator<Cell> cellbody = rown.cellIterator();
                // 得到传入类的实例
                Constructor constructor = entity.getDeclaredConstructor();
                constructor.setAccessible(true);
                Object tObject = constructor.newInstance();
                int k = 0;
                // 遍历一行的列
                while (cellbody.hasNext()) {
                    Cell cell = cellbody.next();
                    // 这里得到此列的对应的标题
                    String titleString = (String) titleMap.get(k);
                    // 如果这一列的标题和类中的某一列的Annotation相同，那么则调用此类的的set方法，进行设值
                    if (fieldSetMap.containsKey(titleString)) {
                        Method setMethod = (Method) fieldSetMap.get(titleString);
                        // 得到setter方法的参数
                        Type[] ts = setMethod.getGenericParameterTypes();
                        // 只要一个参数
                        String xclass = ts[0].toString();
                        // 判断参数类型
                        switch (xclass) {
                            case "class java.lang.String":
                                // 先设置Cell的类型，然后就可以把纯数字作为String类型读进来了：
                                cell.setCellType(CellType.STRING);
                                setMethod.invoke(tObject, cell.getStringCellValue());
                                break;
                            case "class java.util.Date":
                                Date cellDate = null;
                                if (CellType.NUMERIC == cell.getCellTypeEnum()) {
                                    // 日期格式
                                    cellDate = cell.getDateCellValue();
                                } else {    // 全认为是  Cell.CELL_TYPE_STRING: 如果不是 yyyy-mm-dd hh:mm:ss 的格式就不对(wait to do:有局限性)
                                    cellDate = stringToDate(cell.getStringCellValue());
                                }
                                setMethod.invoke(tObject, cellDate);
                                //// --------------------------------------------------------------------------------------------
                                //String cellValue = cell.getStringCellValue();
                                //Date theDate = stringToDate(cellValue);
                                //setMethod.invoke(tObject, theDate);
                                //// --------------------------------------------------------------------------------------------
                                break;
                            case "class java.lang.Boolean":
                                boolean valBool;
                                if (CellType.BOOLEAN == cell.getCellTypeEnum()) {
                                    valBool = cell.getBooleanCellValue();
                                } else {// 全认为是  Cell.CELL_TYPE_STRING
                                    valBool = cell.getStringCellValue().equalsIgnoreCase("true")
                                            || (!cell.getStringCellValue().equals("0"));
                                }
                                setMethod.invoke(tObject, valBool);
                                break;
                            case "class java.lang.Integer":
                                Integer valInt;
                                if (CellType.NUMERIC == cell.getCellTypeEnum()) {
                                    valInt = (new Double(cell.getNumericCellValue())).intValue();
                                } else {// 全认为是  Cell.CELL_TYPE_STRING
                                    valInt = new Integer(cell.getStringCellValue());
                                }
                                setMethod.invoke(tObject, valInt);
                                break;
                            case "class java.lang.Long":
                                Long valLong;
                                if (CellType.NUMERIC == cell.getCellTypeEnum()) {
                                    valLong = (new Double(cell.getNumericCellValue())).longValue();
                                } else {// 全认为是  Cell.CELL_TYPE_STRING
                                    valLong = new Long(cell.getStringCellValue());
                                }
                                setMethod.invoke(tObject, valLong);
                                break;
                            case "class java.math.BigDecimal":
                                BigDecimal valDecimal;
                                if (CellType.NUMERIC == cell.getCellTypeEnum()) {
                                    valDecimal = new BigDecimal(cell.getNumericCellValue());
                                } else {// 全认为是  Cell.CELL_TYPE_STRING
                                    valDecimal = new BigDecimal(cell.getStringCellValue());
                                }
                                setMethod.invoke(tObject, valDecimal);
                                break;
                            case "class java.lang.Double":
                                Double valDouble;
                                valDouble = new Double(cell.getStringCellValue());
                                break;
                        }

                    }
                    // 下一列
                    k = k + 1;
                }
                dist.add(tObject);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return dist;
    }

    private static boolean isFieldFinal(Field field) {
        return (field.getModifiers() & java.lang.reflect.Modifier.FINAL) == java.lang.reflect.Modifier.FINAL;
    }

    /**
     * 字符串转换为Date类型数据（限定格式      YYYY-MM-DD hh:mm:ss）或（YYYY/MM/DD hh:mm:ss）
     *
     * @param cellValue : 字符串类型的日期数据
     * @return 日期类型对象
     */
    private static Date stringToDate(String cellValue) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy/M/d  H:m:s");
        format.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
        try {
            return format.parse(cellValue);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }
}