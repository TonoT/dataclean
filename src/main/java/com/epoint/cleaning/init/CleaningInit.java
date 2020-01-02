package com.epoint.cleaning.init;

import org.apache.log4j.Logger;
import org.quartz.JobExecutionException;

import com.epoint.cleaning.job.DataCleaningJob;
import com.epoint.cleaning.params.ClsParams;
import com.epoint.cleaning.service.QuartzManager;
import com.epoint.core.utils.config.ConfigUtil;
import com.epoint.core.utils.string.StringUtil;

public class CleaningInit
{
    private Logger logger = Logger.getLogger(getClass());

    public void init() throws JobExecutionException, InstantiationException, IllegalAccessException {
        Class<DataCleaningJob> jobcls = DataCleaningJob.class;
        String cron = ConfigUtil.getConfigValue(ClsParams.PROP_NAME, "CleaningJobCron");
        if (StringUtil.isNotBlank(cron)) {
            logger.info("添加定时任务 | " + jobcls.getName() + " | " + cron);
            QuartzManager.addJob(jobcls.getSimpleName(), jobcls, cron, null);
        }
        else {
            logger.info("配置文件[" + ClsParams.PROP_NAME + "]中未配置cron表达式！");
        }
    }
}
