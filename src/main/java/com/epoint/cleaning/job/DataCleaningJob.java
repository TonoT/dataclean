package com.epoint.cleaning.job;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import org.apache.log4j.Logger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.epoint.cleaning.service.CleaningService;
import com.epoint.cleaning.thread.DataCleaningThread;

/**
 * 数据清洗作业
 * 
 * @author hjl19
 *
 */
@DisallowConcurrentExecution
public class DataCleaningJob implements Job
{
    private Logger logger = Logger.getLogger(getClass());

    public static volatile boolean isRunning = false;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        if (isRunning) {
            logger.warn("清洗作业正在运行，无法重复操作！");
            return;
        }

        isRunning = true;
        try {
            logger.info("开始清洗。。。");
            long time1 = System.currentTimeMillis();

            File[] xmlfiles = CleaningService.getXMLFiles();
            CountDownLatch countDownLatch = new CountDownLatch(xmlfiles.length);
            for (File xmlFile : xmlfiles) {
                // 每张表一条线程，定义计数器，当所有表清洗完成时，主线程继续。
                String xmlfilename = xmlFile.getName();
                String tablename = xmlfilename.substring(0, xmlfilename.lastIndexOf("."));

                DataCleaningThread thread = new DataCleaningThread(tablename, countDownLatch);
                thread.start();
            }
            countDownLatch.await();
            long time2 = System.currentTimeMillis();
            logger.info("已完成清洗 | 花费时间：" + (time2 - time1) + "ms。");
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        isRunning = false;
    }
}
