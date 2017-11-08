
package com.btcgarden.bitcoin.jsonrpcclient;

import java.util.AbstractList;
import java.util.List;
import java.util.Map;


abstract class ListMapWrapper<X> extends AbstractList<X> {

    public final List<Map> list;
    
    public ListMapWrapper(List<Map> list) {
        this.list = list;
    }

    protected abstract X wrap(Map m);

    @Override
    public X get(int index) {
        return wrap(list.get(index));
    }

    @Override
    public int size() {
        return list.size();
    }
    
}
