package com.epoint.cleaning.validate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dom4j.Element;
import org.springframework.stereotype.Component;

import com.epoint.core.dao.ICommonDao;
import com.epoint.core.grammar.Record;
import com.epoint.core.utils.string.StringUtil;

@Component("validate_code")
public class Validate_Code extends Validate
{

    @Override
    public List<String> getErrorDataRowguids(ICommonDao dao, String tablename, String columnname, Element ruleElement) {
        List<String> rowguids = new ArrayList<String>();
        String ifValue = ruleElement.attributeValue("if");
        String ruleValue = ruleElement.getStringValue();
        if (StringUtil.isNotBlank(ruleValue)) {
            String allowValue = ruleElement.attributeValue("allowtext");
            List<Record> vals = dao.findList(
                    "select itemvalue,itemtext from code_items where codeid in (select codeid from code_main where codename in ('"
                            + StringUtil.join(ruleValue.split(","), "','") + "'))",
                    Record.class);
            Set<String> ss = new HashSet<String>();
            for (Record val : vals) {
                ss.add(val.getStr("itemvalue"));
                if ("true".equals(allowValue)) {
                    ss.add(val.getStr("itemtext"));
                }
            }
            String where = columnname + " not in ('" + StringUtil.join(new ArrayList<String>(ss), "','") + "')";

            rowguids = super.findRowguids(dao, tablename,
                    (StringUtil.isNotBlank(ifValue) ? ("(" + ifValue + ") and ") : "(") + where + ")");
        }
        return rowguids;
    }

}
