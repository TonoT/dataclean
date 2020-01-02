package com.epoint.cleaning.maintools;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.epoint.cleaning.service.CleaningService;

public class GenerateInitSQLTool
{
    public static void main(String[] args) throws InterruptedException {
        Map<String, List<String>> sqls = CleaningService.generateInitSQL();
        System.out.println("####################################################");
        for (Entry<String, List<String>> entry : sqls.entrySet()) {
            System.out.println("####### " + entry.getKey());
            for (String sql : entry.getValue()) {
                System.out.println(sql + ";");
            }
            System.out.println();
        }
    }
}
