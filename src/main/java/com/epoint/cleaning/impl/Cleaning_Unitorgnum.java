package com.epoint.cleaning.impl;

import org.springframework.stereotype.Component;

import com.epoint.cleaning.iface.ICleaning;
import com.epoint.core.dao.ICommonDao;
import com.epoint.core.grammar.Record;
import com.epoint.core.utils.string.StringUtil;

@Component("cleaning_unitorgnum")
public class Cleaning_Unitorgnum implements ICleaning
{

    @Override
    public boolean clean(ICommonDao dao, Record params, Object value) {
        if (StringUtil.isNotBlank(value)) {
            // 去掉统一社会信用代码中的副本标记
            String unitorgnum = value.toString().toUpperCase().replaceAll("[^0-9A-Z\\-]||[\\(（][\\s\\S]*[\\)）]", "");
            if (unitorgnum.matches("[^_IOZSVa-z\\W]{2}\\d{6}[^_IOZSVa-z\\W]{10}")) {
                // 是信用代码的再去检查截取下来的组织机构代码是否是正确格式。
                if ((clean(dao, null, unitorgnum.substring(8, 16) + "-" + unitorgnum.substring(16, 17)))) {
                    return true;
                }
            }
            else {
                int[] ws = {3, 7, 9, 10, 5, 8, 4, 2 };
                String str = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
                String regex = "^([0-9A-Z]){8}-[0-9|X]$";

                if (unitorgnum.matches(regex)) {
                    int sum = 0;
                    for (int i = 0; i < 8; i++) {
                        sum += str.indexOf(String.valueOf(unitorgnum.charAt(i))) * ws[i];
                    }

                    int c9 = 11 - (sum % 11);

                    String sc9 = String.valueOf(c9);
                    if (11 == c9) {
                        sc9 = "0";
                    }
                    else if (10 == c9) {
                        sc9 = "X";
                    }

                    if (sc9.equals(String.valueOf(unitorgnum.charAt(9)))) {
                        return true;
                    }
                }
            }

            if (new Cleaning_IDCard().clean(dao, params, value)) {
                // 或者是自然人，填的是身份证。
                return true;
            }
        }
        return false;
    }

}
