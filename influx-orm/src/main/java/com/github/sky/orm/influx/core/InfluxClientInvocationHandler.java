package com.github.sky.orm.influx.core;

import com.github.sky.orm.influx.annotation.Insert;
import com.github.sky.orm.influx.annotation.Select;
import com.github.sky.orm.influx.annotation.Update;
import com.github.sky.orm.influx.binding.InfluxClientMethod;
import com.github.sky.orm.influx.service.IInfluxClientHandler;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * @Description:
 * @author: sky
 * @date: 2024/1/5 16:43
 */
public class InfluxClientInvocationHandler implements InvocationHandler {

    private final String influxClientName;

    private IInfluxClientHandler iInfluxClientHandler;

    private final Map<Method, InfluxClientMethod> dispatch;

    public InfluxClientInvocationHandler(String influxClientName, IInfluxClientHandler iInfluxClientHander, Map<Method, InfluxClientMethod> dispatch) {
        this.influxClientName = influxClientName;
        this.iInfluxClientHandler = iInfluxClientHander;
        this.dispatch = dispatch;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        InfluxClientMethod influxClientMethod = dispatch.get(method);

        Annotation annotations = influxClientMethod.getAnnotations();

        Class<? extends Annotation> annotationType = annotations.annotationType();

        if (Select.class.equals(annotationType)) {
            return iInfluxClientHandler.selectInflux(((Select) annotations).value(), influxClientMethod, args);
        }

        if (Insert.class.equals(annotationType)) {
            return iInfluxClientHandler.insertInflux(args);
        }

        if (Update.class.equals(annotationType)) {
            return iInfluxClientHandler.updateInflux(args);
        }

        return method.invoke(proxy, args);
    }


}
