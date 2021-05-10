package com.example.myapp.web.serializable;

import org.ksoap2.serialization.KvmSerializable;
import org.ksoap2.serialization.PropertyInfo;

import java.util.ArrayList;
import java.util.Hashtable;

public class DataArray<T> extends ArrayList<TestGetDataRequest.Data> implements KvmSerializable {

    @Override
    public Object getProperty(int index) {
        return this.get(index);
    }

    @Override
    public int getPropertyCount() {
        return 1;
    }

    @Override
    public void setProperty(int index, Object value) {
        this.add((TestGetDataRequest.Data) value);
    }

    @Override
    public void getPropertyInfo(int index, Hashtable properties, PropertyInfo info) {
        info.setName("data");
        info.setType(TestGetDataRequest.Data.class);
        info.setValue(this.get(index));
    }
}
