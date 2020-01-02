package com.epoint.cleaning.thread;

import java.util.List;

import org.apache.log4j.Logger;

import com.epoint.cleaning.params.CleanStatus;
import com.epoint.core.dao.CommonDao;
import com.epoint.core.dao.ICommonDao;
import com.epoint.core.utils.string.StringUtil;

public class DataSynchronizationThread extends BasicThread
{
    private Logger logger = Logger.getLogger(getClass());

    private String tablename;

    public DataSynchronizationThread(String tablename) {
        this.tablename = tablename;
    }

    @Override
    public void run() {
        ICommonDao dao = null;
        // 把正确数据导入正式表
        try {
            dao = CommonDao.getInstance();
            List<String> columnnames = dao.findList(
                    "select column_name from information_schema.columns where table_name=? and table_schema=?",
                    String.class, tablename, dao.getDataSource().getDbName());
            if (!columnnames.isEmpty()) {
                String sqlColumnStr = StringUtil.join(columnnames, ",");
                dao.execute("truncate table " + tablename);
                dao.execute("insert into " + tablename + "(" + sqlColumnStr + ") select " + sqlColumnStr + " from "
                        + tablename + "_temp where cleanstatus = ?", CleanStatus.正式数据.getValue());
            }
            logger.info(tablename + " | 成功同步正式表数据。");
        }
        catch (Exception e) {
            logger.error(tablename + " | 同步数据发生异常！", e);
        }
        finally {
            if (dao != null) {
                dao.close();
            }
        }
        //countDownLatch.countDown();
    }
}
