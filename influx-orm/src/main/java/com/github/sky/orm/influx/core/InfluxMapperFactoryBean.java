package com.github.sky.orm.influx.core;

import com.github.sky.orm.influx.annotation.Insert;
import com.github.sky.orm.influx.annotation.ParamName;
import com.github.sky.orm.influx.annotation.Select;
import com.github.sky.orm.influx.annotation.Update;
import com.github.sky.orm.influx.binding.InfluxClientMethod;
import com.github.sky.orm.influx.service.IInfluxClientHandler;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @Description:
 * @author: sky
 * @date: 2024/1/5 14:40
 */
public class InfluxMapperFactoryBean implements FactoryBean<Object>, ApplicationContextAware {

    private static final Set<Class<? extends Annotation>> statementAnnotationTypes = Stream
            .of(Select.class, Insert.class, Update.class)
            .collect(Collectors.toSet());

    private Class<?> type;

    private String name;

    private String clientName;

    private BeanFactory beanFactory;

    private ApplicationContext applicationContext;

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        applicationContext = context;
        beanFactory = context;
    }

    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    <T> T getTarget() {
        Map<Method, InfluxClientMethod> dispatch = apply(type);

        InfluxClientInvocationHandler handler = new InfluxClientInvocationHandler(name,
                beanFactory != null ? beanFactory.getBean(IInfluxClientHandler.class) : applicationContext.getBean(IInfluxClientHandler.class)
                , dispatch);

        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    @Override
    public Object getObject() {
        return getTarget();
    }

    Map<Method, InfluxClientMethod> apply(Class<?> type) {
        final Map<Method, InfluxClientMethod> result = new LinkedHashMap<>();

        for (Method method : type.getDeclaredMethods()) {

            Map<Integer, String> paramMap = new HashMap<>();

            Parameter[] parameters = method.getParameters();

            for (int i = 0; i < parameters.length; i++) {

                Class<?> aClass = parameters[i].getType();

                if (parameters[i].isAnnotationPresent(ParamName.class)) {
                    paramMap.put(i, parameters[i].getAnnotation(ParamName.class).value());

                } else if (ClassUtils.isPrimitiveOrWrapper(aClass) || aClass.equals(String.class) || aClass.isEnum()) {
                    paramMap.put(i, null);

                } else {
                    paramMap.put(i, "OBJECT");
                }
            }
            for (Class<? extends Annotation> annotationType : statementAnnotationTypes) {

                if (Objects.nonNull(method.getAnnotation(annotationType))) {
                    Annotation annotation = method.getAnnotation(annotationType);
                    Type genericReturnType = method.getGenericReturnType();

                    Class<?> returnType = getReturnType(genericReturnType);

                    result.put(method, new InfluxClientMethod(returnType, annotation, paramMap));
                }
            }
        }
        return result;
    }

    private Class<?> getReturnType(Type genericReturnType) {
        // 如果是 ParameterizedType，表示有泛型参数
        if (genericReturnType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericReturnType;

            // 获取泛型参数的类型数组
            Type[] typeArguments = parameterizedType.getActualTypeArguments();

            // typeArguments[0] 是 List 的泛型参数
            if (typeArguments[0] instanceof ParameterizedType) {
                return (Class<?>) ((ParameterizedType) typeArguments[0]).getRawType();
            } else {
                return (Class<?>) typeArguments[0];
            }
        } else {
            return (Class<?>) genericReturnType;
        }
    }

    @Override
    public Class<?> getObjectType() {
        return type;
    }

    @Override
    public boolean isSingleton() {
        return FactoryBean.super.isSingleton();
    }

}
