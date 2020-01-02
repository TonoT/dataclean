package com.epoint.cleaning.service;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.dom4j.Element;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.epoint.cleaning.params.CleanStatus;
import com.epoint.core.dao.CommonDao;
import com.epoint.core.dao.ICommonDao;
import com.epoint.core.grammar.Record;
import com.epoint.core.utils.code.Base64Util;
import com.epoint.core.utils.container.ContainerFactory;
import com.epoint.core.utils.container.IContainerInfo;
import com.epoint.core.utils.file.FileManagerUtil;
import com.epoint.core.utils.string.StringUtil;

public class ReportService
{

    public static byte[] exportExcel(String tablename) {
        byte[] excel = new byte[] {};
        ICommonDao dao = null;
        try {
            dao = CommonDao.getInstance();
            Element root = CleaningService.getRootElement(tablename);

            // 在xml中配置了<report>标签则说明需要导出报告。
            if (CleaningService.isNeedReport(root)) {

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                Element report = root.element("report");

                Element groupEle = report.element("group");
                String groupname = groupEle == null ? null : groupEle.getStringValue();
                List<String> groups = new ArrayList<String>();
                if (StringUtil.isNotBlank(groupname)) {
                    groups = dao.findList("select distinct ifnull(" + groupname + ", '') from " + tablename
                            + "_temp where cleanstatus = ?", String.class, CleanStatus.异常数据.getValue());
                }

                if (groups.isEmpty()) {
                    // 如果没有分类那就是
                    groupname = "''";
                    groups.add("");
                }

                Map<String, InputStream> excels = new HashMap<String, InputStream>();
                // 按组分成几份Excel。
                for (String group : groups) {
                    // 查询异常数据
                    String where = StringUtil.isBlank(group)
                            ? " and (" + groupname + " = '' or " + groupname + " is null)"
                            : " and " + groupname + " = ?";
                    List<Record> datalist = dao.findList(
                            "select * from " + tablename + "_temp where cleanstatus = ?" + where, Record.class,
                            CleanStatus.异常数据.getValue(), group);
                    if (datalist.isEmpty()) {
                        // 未找到异常数据，则直接跳过。
                        continue;
                    }

                    // 创建工作簿
                    XSSFWorkbook workBook = new XSSFWorkbook();

                    // 定义表头的单元格样式
                    XSSFCellStyle headstyle = workBook.createCellStyle();
                    XSSFFont headfont = workBook.createFont();
                    headfont.setBoldweight(XSSFFont.BOLDWEIGHT_BOLD);
                    headstyle.setFont(headfont);
                    headstyle.setAlignment(XSSFCellStyle.ALIGN_CENTER);
                    headstyle.setLocked(true);

                    // 定义异常字段的单元格样式
                    XSSFCellStyle errorstyle = workBook.createCellStyle();
                    XSSFFont errorfont = workBook.createFont();
                    errorfont.setColor(new XSSFColor(new Color(255, 0, 0)));
                    errorstyle.setFont(errorfont);
                    errorstyle.setFillForegroundColor(new XSSFColor(new Color(255, 255, 204)));
                    errorstyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
                    errorstyle.setLocked(false);

                    // 定义正确字段的单元格样式
                    XSSFCellStyle formalstyle = workBook.createCellStyle();
                    formalstyle.setLocked(true);

                    // 定义超链接字段的单元格样式
                    XSSFCellStyle linkstyle = workBook.createCellStyle();
                    XSSFFont linkfont = workBook.createFont();
                    linkfont.setUnderline((byte) 1);
                    linkfont.setColor(new XSSFColor(new Color(0, 0, 255)));
                    linkstyle.setFont(linkfont);
                    linkstyle.setAlignment(XSSFCellStyle.ALIGN_CENTER);
                    linkstyle.setLocked(false);

                    // 得到需要展示的列
                    List<String> xmlcolumns = CleaningService.getXMLColumns(root);
                    xmlcolumns.add(0, "rowguid");

                    // 创建一个表格
                    XSSFSheet sheet = workBook.createSheet();
                    // 设置表名
                    workBook.setSheetName(0, tablename);

                    // 先给第一行设置表头，创建第一行
                    XSSFRow titleRow = sheet.createRow(0);

                    Map<String, String> typemap = new HashMap<String, String>();

                    int index_title = 0;
                    for (String xmlcolumn : xmlcolumns) {
                        // 获取列名
                        Record columninfo = dao.find(
                                "select fieldchinesename,fieldtype from table_struct where fieldname = ? and tableid in (select tableid from table_basicinfo where sql_tablename = ?)",
                                Record.class, xmlcolumn, tablename);

                        XSSFCell cell = titleRow.createCell(index_title);

                        // 赋予表头样式
                        cell.setCellStyle(headstyle);
                        cell.setCellValue(StringUtil.isBlank(columninfo.getStr("fieldchinesename")) ? xmlcolumn
                                : columninfo.getStr("fieldchinesename") + "(" + xmlcolumn + ")");

                        typemap.put(xmlcolumn, columninfo.getStr("fieldtype").toLowerCase());

                        index_title++;
                    }

                    boolean isNeedUrl = CleaningService.isNeedUrl(report);

                    if (isNeedUrl) {
                        XSSFCell cell = titleRow.createCell(index_title);
                        cell.setCellStyle(headstyle);
                        cell.setCellValue("查看");
                    }

                    int index_date = 1;
                    for (Record data : datalist) {
                        XSSFRow dataRow = sheet.createRow(index_date);
                        int index_cell = 0;
                        for (String xmlcolumn : xmlcolumns) {
                            XSSFCell cell = dataRow.createCell(index_cell);
                            if ((";" + data.getStr("errorcolumns")).contains(";" + xmlcolumn + ";")) {
                                // 赋予异常字段样式
                                cell.setCellStyle(errorstyle);
                            }
                            else {
                                // 赋予正常字段样式
                                cell.setCellStyle(formalstyle);
                            }

                            if (data.get(xmlcolumn) != null) {
                                switch (typemap.get(xmlcolumn)) {
                                    case "datetime":
                                        cell.setCellValue(sdf.format(data.getDate(xmlcolumn)));
                                        break;
                                    case "integer":
                                        cell.setCellValue(data.getInt(xmlcolumn));
                                        break;
                                    case "numeric":
                                        cell.setCellValue(data.getDouble(xmlcolumn));
                                        break;
                                    case "image":
                                        cell.setCellValue(Base64Util.encode(data.getBytes(xmlcolumn)));
                                        break;
                                    case "ntext":
                                        cell.setCellValue(new XSSFRichTextString(data.getStr(xmlcolumn)));
                                        break;
                                    default:
                                        cell.setCellValue(data.getStr(xmlcolumn));
                                        break;
                                }
                            }
                            index_cell++;
                        }

                        if (isNeedUrl) {
                            // 增加超链接
                            String url = CleaningService.getUrl(dao, report, data);
                            if (StringUtil.isNotBlank(url)) {
                                XSSFCell cell = dataRow.createCell(index_cell);
                                cell.setCellType(XSSFCell.CELL_TYPE_FORMULA);
                                cell.setCellFormula("HYPERLINK(\"" + url + "\",\"查看详情\")");
                                cell.setCellStyle(linkstyle);
                            }
                        }

                        index_date++;
                    }

                    for (int i = 0; i < xmlcolumns.size(); i++) {
                        try {
                            sheet.autoSizeColumn(i, true);
                        }
                        catch (Exception e) {

                        }
                    }

                    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                        workBook.write(out);
                        workBook.close();
                        String xlsxname = group;
                        if (StringUtil.isNotBlank(xlsxname)) {
                            String type = groupEle == null ? null : groupEle.attributeValue("type");
                            if (StringUtil.isNotBlank(type)) {
                                String typevalue = null;
                                switch (type) {
                                    // 暂时只有xiaqu类型
                                    case "xiaqu":
                                        typevalue = new CityService(dao).getFullCityName(xlsxname);
                                        break;
                                }
                                if (StringUtil.isNotBlank(typevalue)) {
                                    xlsxname = xlsxname + "_" + typevalue;
                                }
                            }
                        }
                        else {
                            xlsxname = "未分类";
                        }
                        excels.put(xlsxname + ".xlsx", new ByteArrayInputStream(out.toByteArray()));
                    }
                }
                if (!excels.isEmpty()) {
                    // 不为空，打包为zip。
                    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                        toZip(excels, out);
                        excel = out.toByteArray();
                    }
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
        return excel;
    }

    public static String saveAttachFile(String category, String filename, byte[] byt) {
        String doc = ReportService.getAttachStoragePath() + category + File.separator;
        FileManagerUtil.writeContentToFile(doc, filename, byt);
        return doc + filename;
    }

    public static List<File> getAttachFiles(String category) {
        return FileManagerUtil.getFileListByDirectory(ReportService.getAttachStoragePath() + category + File.separator,
                null, null);
    }

    public static byte[] getAttachFile(String category, String filename) {
        return FileManagerUtil
                .getContentFromSystem(ReportService.getAttachStoragePath() + category + File.separator + filename);
    }

    public static String getAttachStoragePath() {
        return getWebRootPath() + "WEB-INF" + File.separator + "AttachStorage" + File.separator;
    }

    private static String webRootPath;

    public static String getWebRootPath() {
        if (webRootPath == null) {
            String p = System.getProperty("webapp.root");
            if (StringUtil.isNotBlank(p)) {
                webRootPath = p;
            }
            else {
                IContainerInfo containerInfo = ContainerFactory.getContainInfo();
                if (containerInfo != null) {
                    HttpServletRequest request = null;
                    if (containerInfo.getCurrentRequest() == null) {
                        request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
                    }
                    else {
                        request = containerInfo.getCurrentRequest();
                    }
                    if (request != null) {
                        webRootPath = request.getSession().getServletContext().getRealPath("/");
                    }
                }
                else {
                    try {
                        String path = CleaningService.class.getResource("/").getFile();
                        webRootPath = new File(path).getParentFile().getParentFile().getCanonicalPath() + File.separator
                                + "src" + File.separator + "main" + File.separator + "webapp" + File.separator;
                    }
                    catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return webRootPath;
    }

    private static final int BUFFER_SIZE = 1024;

    public static void toZip(Map<String, InputStream> files, OutputStream out) {
        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(out);
            for (Entry<String, InputStream> entry : files.entrySet()) {
                byte[] buf = new byte[BUFFER_SIZE];
                zos.putNextEntry(new ZipEntry(entry.getKey()));
                int len;
                while ((len = entry.getValue().read(buf)) != -1) {
                    zos.write(buf, 0, len);
                }
                zos.closeEntry();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (zos != null) {
                try {
                    zos.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }

            for (Entry<String, InputStream> entry : files.entrySet()) {
                try {
                    entry.getValue().close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
