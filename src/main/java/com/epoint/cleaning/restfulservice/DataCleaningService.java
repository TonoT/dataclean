package com.epoint.cleaning.restfulservice;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.quartz.JobExecutionException;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSON;
import com.epoint.cleaning.job.DataCleaningJob;
import com.epoint.cleaning.service.CleaningService;
import com.epoint.core.dao.CommonDao;
import com.epoint.core.dao.ICommonDao;
import com.epoint.core.grammar.Record;
import com.epoint.core.utils.string.StringUtil;

@RestController
public class DataCleaningService
{
    @RequestMapping(value = "/querytablenames", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public String queryTableNames() {
        List<String> tablenames = new ArrayList<String>();
        for (File file : CleaningService.getXMLFiles()) {
            tablenames.add(CleaningService.getFileName(file.getName())[0]);
        }
        return JSON.toJSONString(tablenames);
    }

    @RequestMapping(value = "/queryxmlcontent", method = RequestMethod.POST, consumes = "text/html;charset=UTF-8", produces = "application/xml;charset=UTF-8")
    public String queryXMLContent(@RequestBody String tablename) {
        return CleaningService.getXMLContent(tablename);
    }

    /**
     * 请求格式{tablename:'', record:{column1:'',column2:''}}
     * 
     * @param json
     * @return
     * @throws DocumentException
     * @throws UnsupportedEncodingException
     */
    @RequestMapping(value = "/queryduplications", method = RequestMethod.POST, consumes = "application/json;charset=UTF-8", produces = "application/json;charset=UTF-8")
    public String queryDuplications(@RequestBody String json) {
        ICommonDao dao = null;
        List<Record> duplications = new ArrayList<Record>(0);
        try {
            Record param = JSON.parseObject(json, Record.class);
            String tablename = param.getStr("tablename");
            Record record = param.get("record", Record.class);

            // 获取用来确定唯一性的字段名
            StringBuffer where = new StringBuffer();
            List<Object> values = new LinkedList<Object>();
            Element root = CleaningService.getRootElement(tablename);
            Element unique = root.element("unique");
            if (unique != null) {
                List<Element> columns = root.elements("column");
                for (Element column : columns) {
                    String columnname = column.getStringValue();
                    if (StringUtil.isNotBlank(columnname)) {
                        where.append(" and ").append(columnname).append(" = ").append("?");
                        values.add(record.get(columnname));
                    }
                }
            }

            dao = CommonDao.getInstance();
            duplications = dao.findList("select * from " + tablename + "_temp where 1=1" + where, Record.class,
                    values.toArray());

        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (dao != null) {
                dao.close();
            }
        }

        return JSON.toJSONString(duplications);
    }

    @RequestMapping(value = "/cleanimmediately_asyn", method = RequestMethod.POST, produces = "text/plain;charset=UTF-8")
    public String cleanImmediately_Asyn() {
        if (DataCleaningJob.isRunning) {
            return "清洗作业正在运行，无法重复操作！";
        }
        else {
            new Thread(new Runnable()
            {
                @Override
                public void run() {
                    try {
                        new DataCleaningJob().execute(null);
                    }
                    catch (JobExecutionException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            return "启动成功！";
        }
    }

    @RequestMapping(value = "/cleanimmediately", method = RequestMethod.POST)
    public void cleanImmediately() throws JobExecutionException {
        // 如果正在进行，那就先等待。
        while (DataCleaningJob.isRunning)
            ;
        new DataCleaningJob().execute(null);
    }
}
