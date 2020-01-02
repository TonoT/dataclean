package com.epoint.cleaning.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.epoint.core.dao.ICommonDao;
import com.epoint.core.grammar.Record;
import com.epoint.core.utils.string.StringUtil;

public class CityService
{
    private ICommonDao dao;
    private static Map<String, String> city;

    public CityService(ICommonDao dao) {
        this.dao = dao;
    }

    public Map<String, String> getCityList() {
        if (city == null) {
            city = new HashMap<String, String>();
            List<Record> citys = dao.findList("select citycode,cityname from huiyuan_city", Record.class);
            for (Record c : citys) {
                city.put(c.getStr("citycode"), c.getStr("cityname"));
            }
            // 数据库里的可能会不太准确，增加读取配置文件。
            rebuild(city);
        }
        return city;
    }

    public String getCityName(String citycode) {
        return getCityList().get(citycode);
    }

    public String getFullCityName(String citycode) {
        String fullname = "";
        if (citycode.matches("^[0-9]{6}$")) {
            for (int i = 2; i <= citycode.length(); i += 2) {
                String code = citycode.substring(0, i);
                if (!"00".equals(citycode.substring(i - 2, i))) {
                    for (int j = code.length(); j < 6; j++) {
                        code += "0";
                    }
                    String city = getCityName(code);
                    if (StringUtil.isNotBlank(city)) {
                        fullname += city;
                    }
                    fullname += "·";
                }
            }
            return fullname.substring(0, fullname.length() - 1);
        }
        return fullname;
    }

    private void rebuild(Map<String, String> city) {
        String json = CleaningService.getJsonString("xiaqu");
        if (StringUtil.isNotBlank(json)) {
            city.putAll(JSON.parseObject(json, new TypeReference<Map<String, String>>()
            {
            }));
        }
    }
}
