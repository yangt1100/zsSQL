package com.zssql.common.handler;

import org.apache.commons.collections.CollectionUtils;

import java.util.Collection;

public interface Collector<T> {
    void collect(T record);

    default void collects(Collection<T> records) {
        if (CollectionUtils.isEmpty(records)) {
            return;
        }

        for (T record : records) {
            collect(record);
        }
    }
}
