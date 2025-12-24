package com.github.sky.orm.influx.service;

import com.data.source.orm.influx.annotation.Insert;
import com.data.source.orm.influx.annotation.Update;
import com.data.source.orm.influx.binding.InfluxClientMethod;
import com.data.source.storage.influx.InfluxDbService;
import com.influxdb.client.domain.Query;
import com.influxdb.spring.influx.InfluxDB2Properties;

import java.util.*;

public class InfluxClientHandler implements IInfluxClientHandler {

    private InfluxDbService influxDbService;

    private IParamResolve paramResolve;

    private InfluxDB2Properties influxDB2Properties;


    public InfluxClientHandler(InfluxDbService influxDbService, IParamResolve paramResolve, InfluxDB2Properties properties) {
        this.influxDbService = influxDbService;
        this.paramResolve = paramResolve;
        this.influxDB2Properties = properties;
    }

    @Override
    public Object selectInflux(String oriSql, InfluxClientMethod influxClientMethod, Object[] args) {

        /**
         * 1.进行sql解析
         * 2.绑定参数
         * 3.添加database
         */

        Class<?> returnType = influxClientMethod.getReturnType();

        Map<Integer, String> paramNames = influxClientMethod.getParamNames();

        oriSql = oriSql.replaceFirst("#\\{bucket}", influxDB2Properties.getBucket());

        //#{}字符串替换形式
        String sql = paramResolve.resolveSql(paramNames, args, oriSql);

        Map<String, Object> params = new HashMap<>();

        //绑定参数
        paramNames.forEach((k, v) -> {
            if (Objects.nonNull(args[k])) {
                params.put(v, args[k]);
            }
        });

        Query query = new Query().query(sql).params(params);

        return influxDbService.query(query, returnType);
    }



    @Override
    public Object insertInflux(Object[] args) {
        insertOrUpdate(args, Insert.class);

        throw new RuntimeException("Inserting uncertain data types");
    }

    @Override
    public Object updateInflux(Object[] args) {
        insertOrUpdate(args, Update.class);

        throw new RuntimeException("Operation type error");
    }

    private void insertOrUpdate(Object[] args, Class<?> updateClass) {
        Object arg = args[0];

        if (Objects.nonNull(arg) && paramResolve.checkId(arg, updateClass)) {
            if (List.class.isAssignableFrom(arg.getClass()) || arg.getClass().isArray()) {
                influxDbService.batchPoints(arg.getClass().isArray() ? Arrays.asList((Object[]) arg) : (List) arg);
            } else {
                influxDbService.batchPoint(arg);
            }
        }
    }
}
