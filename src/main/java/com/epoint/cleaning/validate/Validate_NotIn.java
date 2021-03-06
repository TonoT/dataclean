package com.epoint.cleaning.validate;

import java.util.ArrayList;
import java.util.List;

import org.dom4j.Element;
import org.springframework.stereotype.Component;

import com.epoint.core.dao.ICommonDao;
import com.epoint.core.utils.string.StringUtil;

@Component("validate_notin")
public class Validate_NotIn extends Validate
{

    @Override
    public List<String> getErrorDataRowguids(ICommonDao dao, String tablename, String columnname, Element ruleElement) {
        List<String> rowguids = new ArrayList<String>();
        String ifValue = ruleElement.attributeValue("if");
        String ruleValue = ruleElement.getStringValue();
        if (StringUtil.isNotBlank(ruleValue)) {
            rowguids = super.findRowguids(dao, tablename,
                    (StringUtil.isNotBlank(ifValue) ? ("(" + ifValue + ") and ") : "") + columnname + " in ('"
                            + StringUtil.join(ruleValue.split(","), "','") + "')");
        }
        return rowguids;
    }

}
