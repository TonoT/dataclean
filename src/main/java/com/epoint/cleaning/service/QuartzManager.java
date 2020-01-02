package com.epoint.cleaning.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.DateBuilder;
import org.quartz.DateBuilder.IntervalUnit;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * quartz定时器管理
 * 
 * @author hjliang
 * @version 2018年10月23日
 */
public class QuartzManager
{
    @Autowired
    private static Scheduler scheduler;

    static {
        if (scheduler == null) {
            try {
                Properties properties = new Properties();
                // 使用ClassLoader加载properties配置文件生成对应的输入流
                InputStream in = QuartzManager.class.getClassLoader().getResourceAsStream("quartz.properties");
                // 使用properties对象加载输入流
                properties.load(in);
                StdSchedulerFactory factory = new StdSchedulerFactory(properties);
                scheduler = factory.getScheduler();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 添加任务
     * 
     * @param jobName
     * @param jobClass
     * @param cron
     * @param params
     */
    public static void addJob(String jobName, Class<? extends Job> jobClass, String cron, Map<String, Object> params) {
        try {
            // 创建jobDetail实例，绑定Job实现类
            // 指明job的名称，所在组的名称，以及绑定job类
            JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(jobName)// 任务名称和组构成任务key
                    .build();
            if (params != null) {
                jobDetail.getJobDataMap().putAll(params);
            }
            // 定义调度触发规则
            // 使用cornTrigger规则
            Trigger trigger = TriggerBuilder.newTrigger().withIdentity(jobName + "Trigger")// 触发器key
                    .startAt(DateBuilder.futureDate(1, IntervalUnit.SECOND))
                    .withSchedule(CronScheduleBuilder.cronSchedule(cron)).startNow().build();
            // 把作业和触发器注册到任务调度中
            scheduler.scheduleJob(jobDetail, trigger);
            // 启动
            if (!scheduler.isShutdown()) {
                scheduler.start();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取所有计划中的任务列表
     * 
     * @return
     * @throws SchedulerException
     */
    public static List<String> getAllJob() throws SchedulerException {
        GroupMatcher<JobKey> matcher = GroupMatcher.anyJobGroup();
        Set<JobKey> jobKeys = scheduler.getJobKeys(matcher);
        List<String> jobList = new ArrayList<String>();
        for (JobKey jobKey : jobKeys) {
            jobList.add(jobKey.getName());
        }
        return jobList;
    }

    /**
     * 获取所有计划中的触发器
     * 
     * @return
     * @throws SchedulerException
     */
    public static List<String> getAllTrigger() throws SchedulerException {
        GroupMatcher<TriggerKey> matcher = GroupMatcher.anyTriggerGroup();
        Set<TriggerKey> triggerKeys = scheduler.getTriggerKeys(matcher);
        List<String> triggerList = new ArrayList<String>();
        for (TriggerKey triggerKey : triggerKeys) {
            triggerList.add(triggerKey.getName());
        }
        return triggerList;
    }

    /**
     * 获取所有正在运行的job
     * 
     * @return
     * @throws SchedulerException
     */
    public static List<String> getRunningJob() throws SchedulerException {
        List<JobExecutionContext> executingJobs = scheduler.getCurrentlyExecutingJobs();
        List<String> jobList = new ArrayList<String>(executingJobs.size());
        for (JobExecutionContext executingJob : executingJobs) {
            JobDetail jobDetail = executingJob.getJobDetail();
            JobKey jobKey = jobDetail.getKey();
            jobList.add(jobKey.getName());
        }
        return jobList;
    }

    /**
     * 暂停一个job
     * 
     * @param jobName
     * @throws SchedulerException
     */
    public static void pauseJob(String jobName) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(jobName);
        scheduler.pauseJob(jobKey);
    }

    /**
     * 恢复一个job
     * 
     * @param jobName
     * @throws SchedulerException
     */
    public static void resumeJob(String jobName) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(jobName);
        scheduler.resumeJob(jobKey);
    }

    /**
     * 删除一个job
     * 
     * @param jobName
     * @throws SchedulerException
     */
    public static void deleteJob(String jobName) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(jobName);
        scheduler.deleteJob(jobKey);
    }

    /**
     * 立即执行job
     * 
     * @param jobName
     * @throws SchedulerException
     */
    public static void runJobNow(String jobName) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(jobName);
        scheduler.triggerJob(jobKey);
    }

    /**
     * 更新job时间表达式，返回false标识不存在该job
     * 
     * @param jobName
     * @param cron
     * @return
     * @throws SchedulerException
     */
    public static void updateJobCron(String jobName, String cron) throws SchedulerException {
        TriggerKey triggerKey = TriggerKey.triggerKey(jobName + "Trigger");
        CronTrigger trigger = (CronTrigger) scheduler.getTrigger(triggerKey);

        String oldTime = trigger.getCronExpression();
        if (!oldTime.equalsIgnoreCase(cron)) {
            CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(cron);
            trigger = trigger.getTriggerBuilder().withIdentity(triggerKey).withSchedule(scheduleBuilder).build();
            scheduler.rescheduleJob(triggerKey, trigger);
        }
    }
}
