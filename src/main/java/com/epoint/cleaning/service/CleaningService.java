package com.epoint.cleaning.service;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.epoint.cleaning.params.CleanStatus;
import com.epoint.cleaning.params.ClsParams;
import com.epoint.cleaning.validate.Validate;
import com.epoint.core.dao.CommonDao;
import com.epoint.core.dao.ICommonDao;
import com.epoint.core.grammar.Record;
import com.epoint.core.utils.classpath.ClassPathUtil;
import com.epoint.core.utils.config.ConfigUtil;
import com.epoint.core.utils.container.ContainerFactory;
import com.epoint.core.utils.file.FileManagerUtil;
import com.epoint.core.utils.string.StringUtil;

public class CleaningService
{
    private final static Logger logger = Logger.getLogger(CleaningService.class);
    private ICommonDao dao;
    private String tablename;

    public CleaningService(ICommonDao dao, String tablename) {
        this.dao = dao;
        this.tablename = tablename;
    }

    public static boolean isExistsTempTable(String tablename) {
        ICommonDao dao = null;
        try {
            dao = CommonDao.getInstance();
            if (dao.queryInt("select count(1) from information_schema.tables where table_name=? and table_schema=?",
                    tablename + "_temp", dao.getDataSource().getDbName()) > 0) {
                return true;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public int init() {
        // 将未清洗的，更改为正在清洗，解决幻读风险。
        int cnt = dao.execute(
                "update " + tablename + "_temp set cleanstatus = ?, errorcolumns = null where cleanstatus in (?,?)",
                CleanStatus.正在清洗.getValue(), CleanStatus.未清理.getValue(), CleanStatus.正在清洗.getValue());
        if (cnt > 0) {
            // 给RowGuid为空的置RowGuid值
            dao.execute(
                    "update " + tablename
                            + "_temp set rowguid = uuid() where cleanstatus = ? and rowguid is null or rowguid = ''",
                    CleanStatus.正在清洗.getValue());
        }
        return cnt;
    }

    public void clean(Element columnEle) {
        List<Element> rules = columnEle.elements();
        String columnname = columnEle.getName();
        Set<String> rowguids = new HashSet<String>();
        for (Element rule : rules) {
            // 执行验证，将不符合规则的都标记为异常。
            Validate validate = ContainerFactory.getContainInfo().getComponent("validate_" + rule.getName());
            rowguids.addAll(validate.validate(dao, tablename, columnname, rule));
        }
        int size = rowguids.size();
        logger.debug(tablename + " | " + columnname + " | 共检测出出 " + size + " 条异常数据。");
        if (size > 0) {
            ExecutorService fixedThreadPool = Executors.newFixedThreadPool(getThreadCount());
            CountDownLatch countDownLatch = new CountDownLatch((int) Math.ceil(size / (500 * 1.0)));

            List<String> rrowguids = new LinkedList<String>();
            // in值列表限制在500以内
            int index = 0;
            for (String rowguid : rowguids) {
                rrowguids.add(rowguid);
                index++;
                if (rrowguids.size() % 500 == 0 || index >= size) {
                    // 分批插入
                    final List<String> rrrowguids = rrowguids;
                    fixedThreadPool.execute(new Runnable()
                    {
                        @Override
                        public void run() {
                            ICommonDao dao = null;
                            try {
                                dao = CommonDao.getInstance();
                                dao.execute("update " + tablename
                                        + "_temp set errorcolumns = concat(ifnull(errorcolumns, ''), ?, ';') where rowguid in ('"
                                        + StringUtil.join(rrrowguids, "','") + "')", columnname);
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

                    rrowguids = new LinkedList<String>();
                }
            }
            try {
                countDownLatch.await();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 最后，置状态值。
     * 
     * @return [异常数据数，正式数据数]
     */
    public int[] finish() {
        int errorcnt = 0;
        int formalcnt = 0;
        try {
            dao.beginTransaction();
            errorcnt = dao.execute(
                    "update " + tablename
                            + "_temp set cleanstatus = ? where cleanstatus = ? and errorcolumns is not null",
                    CleanStatus.异常数据.getValue(), CleanStatus.正在清洗.getValue());
            formalcnt = dao.execute(
                    "update " + tablename + "_temp set cleanstatus = ? where cleanstatus = ? and errorcolumns is null",
                    CleanStatus.正式数据.getValue(), CleanStatus.正在清洗.getValue());
            dao.commitTransaction();
        }
        catch (Exception e) {
            dao.rollBackTransaction();
            throw e;
        }
        return new int[] {errorcnt, formalcnt };
    }

    private static int threadcount = 0;

    public static int getThreadCount() {
        if (threadcount == 0) {
            String poolSizeStr = ConfigUtil.getConfigValue(ClsParams.PROP_NAME, "ThreadCount");
            threadcount = 10;
            if (StringUtil.isNotBlank(poolSizeStr)) {
                try {
                    threadcount = Integer.parseInt(poolSizeStr);
                }
                catch (Exception e) {
                }
            }
        }
        return threadcount;
    }

    private static int listsize = 0;

    public static int getListSize() {
        if (listsize == 0) {
            String listsizeStr = ConfigUtil.getConfigValue(ClsParams.PROP_NAME, "LimitSizeEveryTime");
            listsize = 5000;
            if (StringUtil.isNotBlank(listsizeStr)) {
                try {
                    listsize = Integer.parseInt(listsizeStr);
                }
                catch (Exception e) {
                }
            }
        }
        return listsize;
    }

    public static File[] getXMLFiles() {
        String xmldic = ConfigUtil.getConfigValue(ClsParams.PROP_NAME, "RuleXMLDic");
        if (StringUtil.isBlank(xmldic)) {
            xmldic = "Standard";
        }
        String path = ClassPathUtil.getClassesPath() + ClsParams.RuleXMLDic + File.separator + xmldic;
        File dir = new File(path);
        return dir.listFiles();
    }

    public static String getXMLContent(String tablename) {
        tablename = tablename.toLowerCase();
        String xmldic = ConfigUtil.getConfigValue(ClsParams.PROP_NAME, "RuleXMLDic");
        if (StringUtil.isBlank(xmldic)) {
            xmldic = "Standard";
        }
        String path = ClassPathUtil.getClassesPath() + ClsParams.RuleXMLDic + File.separator + xmldic + "/" + tablename
                + ".xml";
        String content = FileManagerUtil.getContentFromSystemByReader(path);
        if (StringUtil.isNotBlank(content)) {
            // 将内容为空的去掉增加解析效率，将空格符都改为单个空格
            content = content.replaceAll("\\s+", " ").trim();
        }
        return content;
    }

    public static List<String> getXMLColumns(Element root) {
        Element ele = root.element("rules");

        List<String> lst = new ArrayList<String>();
        for (Object obj : ele.elements()) {
            Element field = ((Element) obj);
            lst.add(field.getName());
        }
        return lst;
    }

    public static Element getRootElement(String tablename) throws UnsupportedEncodingException, DocumentException {
        String xml = CleaningService.getXMLContent(tablename);
        Element root = null;
        if (StringUtil.isNotBlank(xml)) {
            SAXReader sax = new SAXReader();
            Document document = sax.read(new ByteArrayInputStream(xml.getBytes("utf-8")));
            root = document.getRootElement();
        }
        return root;
    }

    public static String[] getFileName(String filename) {
        int index = filename.lastIndexOf(".");
        if (index > -1) {
            return new String[] {filename.substring(0, index), filename.substring(index) };
        }
        return new String[] {filename, "" };
    }

    public static boolean isNeedReport(Element root) {
        Element urlEle = root.element("report");
        if (urlEle != null) {
            return true;
        }
        return false;
    }

    public static boolean isNeedUrl(Element report) {
        Element urlEle = report.element("url");
        if (urlEle != null) {
            List<Element> eles = urlEle.elements();
            if (!eles.isEmpty() && StringUtil.isNotBlank(urlEle.getStringValue())) {
                return true;
            }
        }
        return false;
    }

    public static String getUrl(ICommonDao dao, Element report, Record record) {
        String url = null;
        try {
            Element urlEle = report.element("url");
            if (urlEle != null) {
                Attribute attr = urlEle.attribute("type");
                if (attr == null || StringUtil.isBlank(attr.getStringValue())) {
                    // 如果没有分组
                    url = urlEle.getStringValue();
                }
                else {
                    // 如果有
                    String type = null;
                    switch (attr.getStringValue()) {
                        case "danwei":
                            type = record.getStr("type");
                            if (StringUtil.isBlank(type) && StringUtil.isNotBlank(record.getStr("danweiguid"))) {
                                type = dao.queryString("select type from hy_danweiinfo_temp where danweiguid = ?",
                                        record.getStr("danweiguid"));
                            }
                            break;
                        case "biaoduan":
                            type = record.getStr("jiaoyitype");
                            if (StringUtil.isBlank(type) && StringUtil.isNotBlank(record.getStr("biaoduanguid"))) {
                                type = dao.queryString(
                                        "select jiaoyitype from yw_biaoduaninfo_temp where biaoduanguid = ?",
                                        record.getStr("biaoduanguid"));
                            }
                            break;
                    }
                    if (StringUtil.isNotBlank(type)) {
                        Element typeEle = urlEle.element(type.toLowerCase());
                        if (typeEle != null) {
                            url = typeEle.getStringValue();
                        }
                    }
                }
                if (StringUtil.isNotBlank(url)) {
                    url = url.trim();
                    Pattern pattern = Pattern.compile("\\{\\{(?<columnname>[^\\{\\{\\}\\}]+?)\\}\\}");
                    Matcher matcher = pattern.matcher(url);
                    while (matcher.find()) {
                        String columnname = matcher.group("columnname");
                        url = url.replaceAll("\\{\\{" + columnname + "\\}\\}", record.getStr(columnname));
                    }
                }
            }
        }
        catch (Exception e) {
            logger.debug("生成业务系统链接时报错！" + e.getMessage());
        }

        return url;
    }

    public static Map<String, List<String>> generateInitSQL() {
        File[] xmlFiles = CleaningService.getXMLFiles();

        Map<String, List<String>> sqls = new LinkedHashMap<String, List<String>>();
        ICommonDao dao = null;
        try {
            dao = CommonDao.getInstance();
            for (File xmlFile : xmlFiles) {
                List<String> sql = new LinkedList<String>();
                String tablename = getFileName(xmlFile.getName())[0];
                String tablename_temp = tablename + ClsParams.TEMP_SUFFIX;

                sql.add("alter table " + tablename + " rename " + tablename_temp);// 先将原来的正式表转为临时表
                sql.add("create table " + tablename + " like " + tablename_temp);// 创建正式表
                sql.add("alter table " + tablename_temp + " add CleanStatus int default " + CleanStatus.未清理.getValue()
                        + ", add ErrorColumns text");// 给临时表增加清洗状态字段

                List<String> indexnames = dao.findList(
                        "select distinct index_name from information_schema.statistics where table_schema = ? and table_name = ? and index_name != 'PRIMARY'",
                        String.class, dao.getDataSource().getDbName(), tablename_temp);
                for (String indexname : indexnames) {
                    sql.add("drop index " + indexname + " on " + tablename_temp);
                }
                sqls.put(tablename, sql);
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
        return sqls;
    }

    public static Map<String, List<String>> generateClearSQL() {
        File[] xmlFiles = CleaningService.getXMLFiles();
        Map<String, List<String>> sqls = new LinkedHashMap<String, List<String>>();
        for (File xmlFile : xmlFiles) {
            List<String> sql = new LinkedList<String>();
            String tablename = getFileName(xmlFile.getName())[0];
            sql.add("update " + tablename + "_temp set cleanstatus = " + CleanStatus.未清理.getValue());// 将临时表状态为置为未清洗
            sqls.put(tablename, sql);
        }
        return sqls;
    }

    public static String getJsonString(String jsonname) {
        StringBuilder json = new StringBuilder();
        try {
            ClassLoader classLoader = CleaningService.class.getClassLoader();
            URL urlObject = classLoader.getResource(jsonname + ".json");
            URLConnection uc = urlObject.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream(), "UTF-8"));
            String inputLine = null;
            while ((inputLine = in.readLine()) != null) {
                json.append(inputLine.trim());
            }
            in.close();
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return json.toString();
    }
}
