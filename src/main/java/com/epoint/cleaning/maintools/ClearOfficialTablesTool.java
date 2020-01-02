package com.epoint.cleaning.maintools;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.epoint.cleaning.service.CleaningService;
import com.epoint.core.dao.CommonDao;
import com.epoint.core.dao.ICommonDao;

public class ClearOfficialTablesTool
{
    public static void main(String[] args) throws InterruptedException {
        Map<String, List<String>> sqls = CleaningService.generateClearSQL();
        System.out.println("######## 清空正式数据");
        ICommonDao dao = null;
        try {
            dao = CommonDao.getInstance();
            for (Entry<String, List<String>> entry : sqls.entrySet()) {
                System.out.println("######### " + entry.getKey());
                for (String sql : entry.getValue()) {
                    System.out.println(sql);
                    dao.execute(sql);
                }
                System.out.println();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (dao != null) {
                dao.close();
            }
        }
    }
}
