package com.epoint.cleaning.validate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.dom4j.Element;
import org.springframework.stereotype.Component;

import com.epoint.cleaning.iface.ICleaning;
import com.epoint.cleaning.params.CleanStatus;
import com.epoint.cleaning.service.CleaningService;
import com.epoint.core.dao.CommonDao;
import com.epoint.core.dao.ICommonDao;
import com.epoint.core.grammar.Record;
import com.epoint.core.utils.container.ContainerFactory;
import com.epoint.core.utils.string.StringUtil;

@Component("validate_cleanimpl")
public class Validate_CleanImpl extends Validate
{
    private int listsize = CleaningService.getListSize();
    private int threadcount = CleaningService.getThreadCount();

    @Override
    public List<String> getErrorDataRowguids(ICommonDao dao, String tablename, String columnname, Element ruleElement) {
        List<String> rowguids = new ArrayList<String>();
        String ifValue = ruleElement.attributeValue("if");
        String ruleValue = ruleElement.getStringValue();
        if (StringUtil.isNotBlank(ruleValue)) {
            ICleaning clean = ContainerFactory.getContainInfo().getComponent(ruleValue);
            String paramsStr = ruleElement.attributeValue("params");
            String[] keys = StringUtil.isNotBlank(paramsStr) ? paramsStr.split(",") : new String[0];

            int listcnt = getRecordCount(dao, tablename, ifValue);
            ExecutorService fixedThreadPool = Executors.newFixedThreadPool(threadcount);
            CountDownLatch countDownLatch = new CountDownLatch((int) Math.ceil(listcnt / (listsize * 1.0)));
            for (int i = 0; i < listcnt; i += listsize) {
                final int first = i;
                fixedThreadPool.execute(new Runnable()
                {
                    @Override
                    public void run() {
                        ICommonDao dao = null;
                        try {
                            dao = CommonDao.getInstance();
                            List<Record> lst = getRecordList(dao, first, tablename, ifValue);
                            for (Record rec : lst) {
                                Record params = new Record();
                                for (String key : keys) {
                                    params.set(key, rec.get(key));
                                }
                                if (!clean.clean(dao, params, rec.get(columnname))) {
                                    rowguids.add(rec.getStr("rowguid"));
                                }
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
                        countDownLatch.countDown();
                    }
                });
            }
            try {
                countDownLatch.await();
            }
            catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return rowguids;

    }

    private List<Record> getRecordList(ICommonDao dao, int first, String tablename, String where) {
        if (where == null) {
            where = "";
        }
        if (!"".equals(where)) {
            where = " and " + where;
        }
        return dao.findList(
                "select * from " + tablename + "_temp where cleanstatus = " + CleanStatus.正在清洗.getValue() + where, first,
                listsize, Record.class);
    }

    public int getRecordCount(ICommonDao dao, String tablename, String where) {
        if (where == null) {
            where = "";
        }
        if (!"".equals(where)) {
            where = " and " + where;
        }
        return dao.queryInt("select count(1) from " + tablename + "_temp where cleanstatus = "
                + CleanStatus.正在清洗.getValue() + where);
    }
}
