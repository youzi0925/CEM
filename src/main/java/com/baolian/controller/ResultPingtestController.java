package com.baolian.controller;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.stereotype.Controller;

import com.baolian.entity.ResultPingtestEntity;
import com.baolian.service.ResultPingtestService;
import com.baolian.utils.PageUtils;
import com.baolian.utils.R;


/**
 * ping测试结果表
 *
 * @date 2017-03-31 14:49:21
 */
@Controller
@RequestMapping("resultpingtest")
public class ResultPingtestController {
    @Autowired
    private ResultPingtestService resultPingtestService;

    @RequestMapping("/resultpingtest.html")
    public String list() {
        return "resultpingtest/resultpingtest.html";
    }

    /*areapingtest*/
    @ResponseBody
    @RequestMapping("/areaping")
    @RequiresPermissions("resultpingtest:areaping")
    public R areaping(String area,String starttime,String endtime){
        HashMap<String, Object> map = new HashMap<>();
        map.put("area",area);
        map.put("starttime",starttime);
        map.put("endtime",endtime);
        System.out.print(area+"时间:"+starttime+"to"+endtime);
        //查询列表数据
        List<ResultPingtestEntity> resultPingtestList = resultPingtestService.queryAreaList(map);



        return  R.ok().put("getdatalist",resultPingtestList);


    }

    /**
     * 列表
     */
    @ResponseBody
    @RequestMapping("/list")
    @RequiresPermissions("resultpingtest:list")
    public R list(Integer page, Integer limit) {
        Map<String, Object> map = new HashMap<>();
        map.put("offset", (page - 1) * limit);
        map.put("limit", limit);

        //查询列表数据
        List<ResultPingtestEntity> resultPingtestList = resultPingtestService.queryList(map);
        int total = resultPingtestService.queryTotal(map);

        PageUtils pageUtil = new PageUtils(resultPingtestList, total, limit, page);

        return R.ok().put("page", pageUtil);
    }


    /**
     * 信息
     */
    @ResponseBody
    @RequestMapping("/info/{id}")
    @RequiresPermissions("resultpingtest:info")
    public R info(@PathVariable("id") Integer id) {
        ResultPingtestEntity resultPingtest = resultPingtestService.queryObject(id);

        return R.ok().put("resultPingtest", resultPingtest);
    }

    /**
     * 保存
     */
    @ResponseBody
    @RequestMapping("/save")
    @RequiresPermissions("resultpingtest:save")
    public R save(@RequestBody ResultPingtestEntity resultPingtest) {
        resultPingtestService.save(resultPingtest);

        return R.ok();
    }

    /**
     * 修改
     */
    @ResponseBody
    @RequestMapping("/update")
    @RequiresPermissions("resultpingtest:update")
    public R update(@RequestBody ResultPingtestEntity resultPingtest) {
        resultPingtestService.update(resultPingtest);

        return R.ok();
    }

    /**
     * 删除
     */
    @ResponseBody
    @RequestMapping("/delete")
    @RequiresPermissions("resultpingtest:delete")
    public R delete(@RequestBody Integer[] ids) {
        resultPingtestService.deleteBatch(ids);

        return R.ok();
    }

}