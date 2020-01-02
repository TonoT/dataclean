package com.epoint.cleaning.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.epoint.core.utils.config.ConfigUtil;
import com.epoint.core.utils.date.EpointDateUtil;

public class RuleFunctions
{
    public static final String FUNC_PREFIX = "func_";

    public static String execute(String str) {
        String[] strs = str.split(":");
        String fnname = strs[0];
        for (Method method : RuleFunctions.class.getMethods()) {
            String mname = method.getName();
            if (mname.startsWith(FUNC_PREFIX)) {
                if ((FUNC_PREFIX + fnname).toLowerCase().equals(mname.toLowerCase())) {
                    Object[] params = null;
                    if (strs.length > 1) {
                        params = strs[1].split(",");
                    }
                    try {
                        // 固定格式自动转为时间
                        return func_todate((String) method.invoke(null, params));
                    }
                    catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return str;
    }

    public static String func_config(String name) {
        return ConfigUtil.getConfigValue("datacleaning", name);
    }

    public static String func_todate(String str) {
        try {
            EpointDateUtil.convertString2Date(str);
            return "'" + str + "'";
        }
        catch (Exception e) {

        }
        return str;
    }
}
