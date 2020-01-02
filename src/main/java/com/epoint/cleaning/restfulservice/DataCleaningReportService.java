package com.epoint.cleaning.restfulservice;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.epoint.cleaning.params.ClsParams;
import com.epoint.cleaning.service.CleaningService;
import com.epoint.cleaning.service.ReportService;
import com.epoint.cleaning.thread.ErrorReportingThread;
import com.epoint.core.dao.CommonDao;
import com.epoint.core.dao.ICommonDao;
import com.epoint.core.utils.string.StringUtil;

@RestController
@RequestMapping("/datacleaningreportservice")
public class DataCleaningReportService
{
    @RequestMapping(value = "/exportexcel", method = {RequestMethod.GET,
            RequestMethod.POST }, produces = "application/octet-stream;charset=UTF-8")
    public ResponseEntity<byte[]> exportExcel(@RequestParam(value = "tablename", required = false) String tablename)
            throws UnsupportedEncodingException, FileNotFoundException {
        String filename = null;
        byte[] byt = null;
        if (StringUtil.isBlank(tablename)) {
            // 批量打包拿报告，不需要重新生成。直接返回报告。
            List<File> files = ReportService.getAttachFiles(ClsParams.Reporting_Error);
            Map<String, InputStream> filemap = new HashMap<String, InputStream>();
            for (File f : files) {
                String[] tname = CleaningService.getFileName(f.getName());
                String tablechinesename = getTableChineseName(tname[0]);
                filemap.put((StringUtil.isNotBlank(tablechinesename) ? tablechinesename : tname[0]) + tname[1],
                        new FileInputStream(f));
            }

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                ReportService.toZip(filemap, out);
                filename = "异常数据报告.zip";
                byt = out.toByteArray();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            // 单个的重新生成一遍后，返回报告
            new ErrorReportingThread(tablename).run();

            String tablechinesename = getTableChineseName(tablename);
            filename = (StringUtil.isNotBlank(tablechinesename) ? tablechinesename : tablename) + ".zip";
            byt = ReportService.getAttachFile(ClsParams.Reporting_Error, tablename + ".zip");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment", new String(filename.getBytes("utf-8"), "ISO8859-1"));
        return new ResponseEntity<byte[]>(byt, headers, HttpStatus.OK);

    }

    public String getTableChineseName(String tablename) {
        String tablechinesename = null;
        ICommonDao dao = null;
        try {
            dao = CommonDao.getInstance();
            tablechinesename = dao.queryString("select tablename from table_basicinfo where sql_tablename = ?",
                    tablename);
        }
        catch (Exception e) {

        }
        finally {
            if (dao != null) {
                dao.close();
            }
        }
        return tablechinesename;
    }
}
