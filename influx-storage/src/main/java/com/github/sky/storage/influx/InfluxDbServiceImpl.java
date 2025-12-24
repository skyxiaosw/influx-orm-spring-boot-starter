package com.github.sky.storage.influx;


import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import com.influxdb.client.*;
import com.influxdb.client.domain.Query;
import com.influxdb.client.write.WriteParameters;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;

public class InfluxDbServiceImpl<T> implements InfluxDbService<T> {

    private final WriteApi writeApi;

    private final WriteApiBlocking writeApiBlocking;

    private final QueryApi queryApi;

    public InfluxDbServiceImpl(InfluxDBClient influxDBClient, InfluxDbExtensionProperties properties) {

        WriteOptions writeOptions = WriteOptions.builder()
                .batchSize(properties.getActions())
                .flushInterval(properties.getFlushDuration())
                .build();

        writeApi = influxDBClient.makeWriteApi(writeOptions);
        writeApiBlocking = influxDBClient.getWriteApiBlocking();

        queryApi = influxDBClient.getQueryApi();
    }

    @Override
    public void batchPoints(List<T> points) {
        points.forEach(this::checkInstant);
        writeApi.writeMeasurements(WriteParameters.DEFAULT_WRITE_PRECISION, points);
    }

    @Override
    public void batchPoint(T point) {
        checkInstant(point);
        writeApi.writeMeasurement(WriteParameters.DEFAULT_WRITE_PRECISION, point);
    }

    private void checkInstant(T point) {
        Class<?> aClass = point.getClass();
        AnnotationChecker.checkClassForAnnotation(aClass, Measurement.class);
        Field field = AnnotationChecker.checkFieldForAnnotation(aClass, Column.class, Column::timestamp);
        ReflectionUtils.makeAccessible(field);

        Object object = ReflectionUtils.getField(field, point);
        if (object == null) {
            Instant now = Instant.now();
            ReflectionUtils.setField(field, point, now);
        }
    }

    @Override
    public List<T> query(String command, Class<T> clazz) {
        return queryApi.query(command, clazz);

    }

    @Override
    public List<T> query(Query query, Class<T> clazz) {
        return queryApi.query(query, clazz);
    }

    @Override
    public void batchPointsBlocking(List<T> points) {
        points.forEach(this::batchPoint);
        writeApiBlocking.writeMeasurements(WriteParameters.DEFAULT_WRITE_PRECISION, points);
    }
}
