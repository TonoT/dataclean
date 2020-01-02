package com.epoint.cleaning.restfulservice;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSON;
import com.epoint.cleaning.service.CleaningService;

@RestController
@RequestMapping("/datacleaninginitservice")
public class DataCleaningInitService
{
    public static Object lock = new Object();

    @RequestMapping(value = "/generateinitsql", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public String generateInitSQL() {
        Map<String, List<String>> sqls = CleaningService.generateInitSQL();
        return JSON.toJSONString(sqls);
    }

    @RequestMapping(value = "/generateclearsql", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public String generateClearSQL() {
        Map<String, List<String>> sqls = CleaningService.generateClearSQL();
        return JSON.toJSONString(sqls);
    }

}
