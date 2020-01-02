package com.epoint.cleaning.validate;

import java.util.ArrayList;
import java.util.List;

import org.dom4j.Element;
import org.springframework.stereotype.Component;

import com.epoint.core.dao.ICommonDao;
import com.epoint.core.utils.string.StringUtil;

@Component("validate_min")
public class Validate_Min extends Validate
{

    @Override
    public List<String> getErrorDataRowguids(ICommonDao dao, String tablename, String columnname, Element ruleElement) {
        List<String> rowguids = new ArrayList<String>();
        String ifValue = ruleElement.attributeValue("if");
        String included = ruleElement.attributeValue("included");
        String ruleValue = ruleElement.getStringValue();
        if (StringUtil.isNotBlank(ruleValue)) {
            String dateformat = ruleElement.attributeValue("dateformat");
            if (StringUtil.isNotBlank(dateformat)) {
                columnname = "date_format(" + columnname + ", '" + dateformat + "')";
                ruleValue = "date_format(" + ruleValue + ", '" + dateformat + "')";
            }

            String round = ruleElement.attributeValue("round");
            if (StringUtil.isNotBlank(round)) {
                columnname = "round(" + columnname + ", '" + round + "')";
                ruleValue = "round(" + ruleValue + ", '" + round + "')";
            }

            rowguids = super.findRowguids(dao, tablename,
                    (StringUtil.isNotBlank(ifValue) ? ("(" + ifValue + ") and ") : "") + columnname
                            + ("false".equals(included) ? " <= " : " < ") + ruleValue);
        }
        return rowguids;
    }

}
