package com.github.sky.orm.influx.service;

import com.data.source.orm.influx.binding.InfluxClientMethod;

public interface IInfluxClientHandler {

    Object selectInflux(String oriSql, InfluxClientMethod influxClientMethod, Object[] args);

    Object insertInflux(Object[] args);

    Object updateInflux(Object[] args);
}
