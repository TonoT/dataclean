package com.epoint.cleaning.thread;

import org.apache.log4j.Logger;

import com.epoint.cleaning.params.ClsParams;
import com.epoint.cleaning.service.ReportService;

public class ErrorReportingThread extends BasicThread
{
    private Logger logger = Logger.getLogger(getClass());

    private String tablename;

    public ErrorReportingThread(String tablename) {
        this.tablename = tablename;
    }

    @Override
    public void run() {
        byte[] byt = ReportService.exportExcel(tablename);
        if (byt.length > 0) {
            ReportService.saveAttachFile(ClsParams.Reporting_Error, tablename + ".zip", byt);
            logger.info(tablename + " | 成功生成异常报告。");
        }
        //countDownLatch.countDown();
    }
}
