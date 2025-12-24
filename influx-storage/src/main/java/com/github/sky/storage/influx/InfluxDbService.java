package com.github.sky.storage.influx;


import com.influxdb.client.domain.Query;

import java.util.List;

public interface InfluxDbService<T> {

    void batchPoints(List<T> points);

    void batchPoint(T point);

    List<T> query(String command, Class<T> clazz);

    List<T> query(Query query, Class<T> clazz);

    void batchPointsBlocking(List<T> points);
}
