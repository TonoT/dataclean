package com.epoint.cleaning.iface;

import com.epoint.core.dao.ICommonDao;
import com.epoint.core.grammar.Record;

public interface ICleaning
{
    public boolean clean(ICommonDao dao, Record params, Object value);
}
