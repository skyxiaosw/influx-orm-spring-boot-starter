package com.github.sky.orm.influx.service;

import com.data.source.orm.influx.annotation.Insert;
import com.influxdb.annotations.Column;
import org.springframework.util.ClassUtils;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ParamResolveService implements IParamResolve {

    private static String regex = "#\\{[^}]+\\}";

    private static String OBJECT_DEFAUT_NAME = "OBJECT";

    @Override
    public String resolveSql(Map<Integer, String> paramNames, Object[] args, String value) {

        if (!StringUtils.hasText(value)) {
            return value;
        }

        Properties properties = new Properties();

        HashMap<Integer, String> paramMap = new HashMap<>();

        paramNames.forEach((k, v) -> {

            if (Objects.nonNull(v)) {

                if (OBJECT_DEFAUT_NAME.equals(v)) {
                    // 对象属性绑定
                    Map<String, String> stringMap = objectPropertyBind(args[k]);

                    properties.putAll(stringMap);
                } else {
                    properties.setProperty(v, args[k].toString());
                }
            } else {
                paramMap.put(k, args[k].toString());
            }

        });
        PropertyPlaceholderHelper helper = new PropertyPlaceholderHelper("#{", "}");

        String sql = helper.replacePlaceholders(value, properties);

        for (Map.Entry<Integer, String> entry : paramMap.entrySet()) {

            sql = sql.replaceFirst(regex, entry.getValue());
        }
        return sql;
    }

    private Map<String, String> objectPropertyBind(Object value) {

        HashMap<String, String> map = new HashMap<>();

        ReflectionUtils.doWithFields(value.getClass(), field -> {

            Class<?> fieldType = field.getType();

            ReflectionUtils.makeAccessible(field);

            if (field.get(value) == null) {
                return;
            }
            if (isObjectField(fieldType)) {
                map.putAll(objectPropertyBind(field.get(value)));
            }
            map.put(field.getName(), field.get(value).toString());

        });

        return map;
    }

    private boolean isObjectField(Class<?> fieldType) {
        return !fieldType.isPrimitive() && !fieldType.equals(String.class)
                && !ClassUtils.isPrimitiveWrapper(fieldType) && !fieldType.isEnum();
    }

    @Override
    public boolean checkId(Object arg, Class<?> aClass) {
        boolean isPass = false;

        Class<?> argClass = arg.getClass();

        if (aClass.equals(Insert.class)) {

            if (List.class.isAssignableFrom(argClass) || argClass.isArray()) {
                if (argClass.getGenericSuperclass() instanceof ParameterizedType) {

                    Type[] typeArguments = ((ParameterizedType) argClass.getGenericSuperclass()).getActualTypeArguments();

                    if (typeArguments[0] instanceof ParameterizedType) {
                        throw new RuntimeException("Inserting uncertain data types ");
                    } else {
                        isPass = hasTimeColumn((Class) typeArguments[0]);
                    }
                } else {
                    throw new RuntimeException("Inserting uncertain data types ");
                }

            } else {
                return hasTimeColumn(argClass);
            }
        }

        return isPass;
    }


    private String getValue(Object args) {
        return "'" + args + "'";
    }


    private boolean hasTimeColumn(Class typeArgument) {

        AtomicBoolean isPass = new AtomicBoolean(false);

        ReflectionUtils.doWithFields(typeArgument, field -> {

            if (field.isAnnotationPresent(Column.class) && field.getAnnotation(Column.class).timestamp()) {
                isPass.set(true);
            }
        });
        return isPass.get();
    }
}
