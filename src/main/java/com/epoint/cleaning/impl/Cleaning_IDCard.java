package com.epoint.cleaning.impl;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.epoint.cleaning.iface.ICleaning;
import com.epoint.core.dao.ICommonDao;
import com.epoint.core.grammar.Record;
import com.epoint.core.utils.string.StringUtil;

@Component("cleaning_idcard")
public class Cleaning_IDCard implements ICleaning
{

    @Override
    public boolean clean(ICommonDao dao, Record params, Object value) {
        if (StringUtil.isNotBlank(value)) {
            String idcard = value.toString();
            // 必须是18位。
            if (idcard.length() == 18) {
                // 判断地区
                String areacode = idcard.substring(0, 6);
                if (validateArea(areacode)) {
                    // 判断出生日期
                    String birthyear = idcard.substring(6, 10);
                    String birthmonth = idcard.substring(10, 12);
                    String birthday = idcard.substring(12, 14);
                    if (validateDate(birthyear, birthmonth, birthday)) {
                        // 判断最后几位的格式
                        String extra = idcard.substring(14, 18);
                        if (extra.matches("^[0-9]{3}[0-9xX]$")) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean validateArea(String areacode) {
        try {
            int provincecode = Integer.parseInt(areacode.substring(0, 2));
            if (getProvinceCodes().containsKey(provincecode)) {
                return true;
            }
        }
        catch (Exception e) {

        }
        return false;
    }

    private static Map<Integer, String> provincecodes = null;

    private Map<Integer, String> getProvinceCodes() {
        if (provincecodes == null) {
            provincecodes = new HashMap<Integer, String>();
            provincecodes.put(11, "北京");
            provincecodes.put(12, "天津");
            provincecodes.put(13, "河北");
            provincecodes.put(14, "山西");
            provincecodes.put(15, "内蒙古");
            provincecodes.put(21, "辽宁");
            provincecodes.put(22, "吉林");
            provincecodes.put(23, "黑龙江");
            provincecodes.put(31, "上海");
            provincecodes.put(32, "江苏");
            provincecodes.put(33, "浙江");
            provincecodes.put(34, "安徽");
            provincecodes.put(35, "福建");
            provincecodes.put(36, "江西");
            provincecodes.put(37, "山东");
            provincecodes.put(41, "河南");
            provincecodes.put(42, "湖北");
            provincecodes.put(43, "湖南");
            provincecodes.put(44, "广东");
            provincecodes.put(45, "广西");
            provincecodes.put(46, "海南");
            provincecodes.put(50, "重庆");
            provincecodes.put(51, "四川");
            provincecodes.put(52, "贵州");
            provincecodes.put(53, "云南");
            provincecodes.put(54, "西藏");
            provincecodes.put(61, "陕西");
            provincecodes.put(62, "甘肃");
            provincecodes.put(63, "青海");
            provincecodes.put(64, "宁夏");
            provincecodes.put(65, "新疆");
            provincecodes.put(71, "台湾");
            provincecodes.put(81, "香港");
            provincecodes.put(82, "澳门");
            provincecodes.put(91, "国外");
        }
        return provincecodes;
    }

    private boolean validateDate(String year, String month, String day) {
        try {
            int yearInt = Integer.parseInt(year);
            if (yearInt < 1900 && yearInt > Calendar.getInstance().get(Calendar.YEAR)) {
                throw new Exception();
            }
            int maxday = 0;
            switch (month) {
                case "02":
                    // 暂时不考虑平年、闰年
                    maxday = 29;
                    break;
                case "04":
                case "06":
                case "09":
                case "11":
                    maxday = 30;
                    break;
                case "01":
                case "03":
                case "05":
                case "07":
                case "08":
                case "10":
                case "12":
                    maxday = 31;
                    break;
            }
            if (maxday == 0) {
                throw new Exception();
            }
            int dayInt = Integer.parseInt(day);
            if (dayInt > maxday || dayInt < 1) {
                throw new Exception();
            }
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }
}
