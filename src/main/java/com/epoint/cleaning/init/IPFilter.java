package com.epoint.cleaning.init;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.epoint.core.utils.config.ConfigUtil;
import com.epoint.core.utils.string.StringUtil;
import com.epoint.core.utils.web.WebUtil;

public class IPFilter implements Filter
{
    private Logger logger = Logger.getLogger(getClass());

    @Override
    public void init(FilterConfig config) throws ServletException {
        logger.debug("IPFilter init...");
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        String ip = WebUtil.getClientIP((HttpServletRequest) req);
        String allowipsStr = ConfigUtil.getConfigValue("datacleaning", "AllowIPs");
        if (StringUtil.isNotBlank(allowipsStr)) {
            String[] allowips = allowipsStr.split(";");
            for (String allowip : allowips) {
                if (ip.contains(allowip)) {
                    chain.doFilter(req, resp);
                    return;
                }
            }
        }
        logger.warn("非法的访问ip======" + ip);

        resp.setCharacterEncoding("utf-8");
        resp.setContentType("text/html; charset=utf-8");
        PrintWriter writer = resp.getWriter();
        writer.write("无权限！");
    }

    @Override
    public void destroy() {
        logger.debug("IPFilter destroy...");
    }

}
