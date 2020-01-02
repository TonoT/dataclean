package com.epoint.cleaning.validate;

import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Element;

import com.epoint.cleaning.params.CleanStatus;
import com.epoint.cleaning.service.RuleFunctions;
import com.epoint.core.dao.ICommonDao;
import com.epoint.core.utils.config.ConfigUtil;

public abstract class Validate
{
    private Logger logger = Logger.getLogger(getClass());

    private boolean isCleaningSQLLogDisplayed = false;

    {
        String b = ConfigUtil.getConfigValue("datacleaning", "IsCleaningSQLLogDisplayed");
        if ("true".equalsIgnoreCase(b)) {
            isCleaningSQLLogDisplayed = true;
        }
    }

    /**
     * 返回非法记录的rowguid值
     * 
     * @param ruleElement
     * @return
     */
    public abstract List<String> getErrorDataRowguids(ICommonDao dao, String tablename, String columnname,
            Element ruleElement);

    public List<String> validate(ICommonDao dao, String tablename, String columnname, Element ruleElement) {
        // 重新设置string值
        ruleElement.setText(RuleFunctions.execute(ruleElement.getStringValue().trim()));
        return getErrorDataRowguids(dao, tablename, columnname, ruleElement);
    }

    protected List<String> findRowguids(ICommonDao dao, String tablename, String where) {
        String sql = "select rowguid from " + tablename + "_temp where cleanstatus = " + CleanStatus.正在清洗.getValue()
                + " and " + where;
        if (isCleaningSQLLogDisplayed) {
            logger.info("=====" + tablename + "=====\n" + sql);
        }
        return dao.findList(sql, String.class);
    }
}
