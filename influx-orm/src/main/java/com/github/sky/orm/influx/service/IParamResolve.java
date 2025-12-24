package com.github.sky.orm.influx.service;


import java.util.Map;

public interface IParamResolve {

    String resolveSql(Map<Integer, String> paramMap, Object[] args, String value);


    boolean checkId(Object arg, Class<?> aClass);
}
