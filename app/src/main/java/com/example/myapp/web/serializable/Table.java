package com.example.myapp.web.serializable;

import org.ksoap2.serialization.KvmSerializable;
import org.ksoap2.serialization.PropertyInfo;

import java.util.Hashtable;
import java.util.Vector;

public class Table extends Vector<Row> implements KvmSerializable {
    @Override
    public Object getProperty(int index) {
        return this.get(index);
    }

    @Override
    public int getPropertyCount() {
        return this.elementCount;
    }

    @Override
    public void setProperty(int index, Object value) {
        this.add((Row) value);
    }

    @Override
    public void getPropertyInfo(int index, Hashtable properties, PropertyInfo info) {
        info.setNamespace("http://webservice");
        info.setName("row");
        info.setType(Row.class);
        info.setValue(this.get(index));
    }
}
