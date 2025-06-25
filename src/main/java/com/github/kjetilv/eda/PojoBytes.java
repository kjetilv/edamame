package com.github.kjetilv.eda;

import com.github.kjetilv.eda.impl.MapMemoizerFactory;

public interface PojoBytes {

    byte[] bytes(Object pojo);

    /**
     * Use hashcode to derive four bytes
     */
    PojoBytes HASHCODE = MapMemoizerFactory.HASHCODE;

    /**
     * Use {@link Object#toString()} to derive bytes
     */
    PojoBytes TOSTRING = MapMemoizerFactory.TOSTRING;
}
