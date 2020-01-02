package com.epoint.cleaning.thread;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.log4j.Logger;
import org.dom4j.Element;

import com.epoint.cleaning.service.CleaningService;
import com.epoint.core.dao.CommonDao;
import com.epoint.core.dao.ICommonDao;

public class DataCleaningThread extends BasicThread
{
    private Logger logger = Logger.getLogger(getClass());

    private String tablename;

    public DataCleaningThread(String tablename, CountDownLatch countDownLatch) {
        this.tablename = tablename;
        this.countDownLatch = countDownLatch;
    }

    @Override
    public void run() {
        if (CleaningService.isExistsTempTable(tablename)) {
            // 如果存在临时表，那就执行清洗。
            int[] cnt = new int[] {0, 0 };
            try {
                Element root = CleaningService.getRootElement(tablename);
                if (root != null) {
                    Element rulesElement = root.element("rules");
                    if (rulesElement != null) {
                        List<Element> listElement = rulesElement.elements();
                        logger.debug("清洗开始 ===== " + tablename);
                        Exception exp = null;
                        ICommonDao dao = null;
                        try {
                            dao = CommonDao.getInstance();
                            CleaningService service = new CleaningService(dao, tablename);
                            // 1.初始化
                            if (service.init() > 0) {
                                // 2.遍历所有需要验证的字段名
                                for (Element columnEle : listElement) {
                                    service.clean(columnEle);
                                }
                                // 3.完成。将没有错误字段的标记为正式数据，并导入正式表。有错误字段的置为异常数据。
                                cnt = service.finish();
                            }
                        }
                        catch (Exception e) {
                            exp = e;
                        }
                        finally {
                            if (dao != null) {
                                dao.close();
                            }
                            if (exp != null) {
                                throw exp;
                            }
                        }

                        // List<BasicThread> threads = new
                        // LinkedList<BasicThread>();
                        if (cnt[1] > 0) {
                            // 4.录入正式表
                            // threads.add(new
                            // DataSynchronizationThread(tablename));
                            new DataSynchronizationThread(tablename).run();
                        }
                        if (cnt[0] > 0) {
                            // 5.生成异常报告
                            // threads.add(new ErrorReportingThread(tablename));
                            new ErrorReportingThread(tablename).start();
                        }
                        /*
                         * if (!threads.isEmpty()) {
                         * CountDownLatch subCountDownLatch = new
                         * CountDownLatch(threads.size());
                         * for (BasicThread thread : threads) {
                         * thread.setCountDownLatch(subCountDownLatch);
                         * thread.start();
                         * }
                         * subCountDownLatch.await();
                         * }
                         */
                    }
                }
            }
            catch (Exception e) {
                logger.error(tablename + " | 清洗作业发生异常！", e);
            }
            logger.info(tablename + " | 清洗结束| 异常数据：" + cnt[0] + "，正式数据：" + cnt[1] + " | 剩余"
                    + (countDownLatch.getCount() - 1) + "张表。");
        }
        else {
            // 如果不存在临时表，那就不执行清洗。
            logger.warn(tablename + " | 不存在临时表！");
        }
        countDownLatch.countDown();
    }
}
