package com.github.kjetilv.eda;

import com.github.kjetilv.eda.impl.MapMemoizerFactory;

/**
 * Say you have a Java object.  How do you turn it into a byte array for hashing?  This interface knows how.
 */
@SuppressWarnings("unused")
public interface PojoBytes {

    byte[] bytes(Object pojo);

    /**
     * Uses hashcode to derive four bytes
     */
    PojoBytes HASHCODE = MapMemoizerFactory.HASHCODE;

    /**
     * Uses {@link Object#toString()} to derive bytes from the string
     */
    PojoBytes TOSTRING = MapMemoizerFactory.TOSTRING;
}
