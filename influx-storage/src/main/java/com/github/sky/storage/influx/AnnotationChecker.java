package com.github.sky.storage.influx;


import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class AnnotationChecker {


    private static final Map<ClassPropertyAnnotation, Field> CLASS_CHECK_MAP = new ConcurrentHashMap<>();

    public static void checkClassForAnnotation(Class<?> clazz, Class<? extends Annotation> annotationClass) {
        if (!clazz.isAnnotationPresent(annotationClass)) {
            throw new RuntimeException("In class " + clazz.getName() + ", must be annotated with @" + annotationClass.getName());
        }
    }

    public static <T extends Annotation> Field checkFieldForAnnotation(Class<?> clazz, Class<T> annotationClass) {
        return checkFieldForAnnotation(clazz, annotationClass, null);
    }

    public static <T extends Annotation> Field checkFieldForAnnotation(Class<?> clazz, Class<T> annotationClass, Function<T, Boolean> function) {

        ClassPropertyAnnotation<T> classPropertyAnnotation = new ClassPropertyAnnotation<>(clazz, annotationClass);

        Field targetField = CLASS_CHECK_MAP.computeIfAbsent(classPropertyAnnotation, k -> {

            Class<?> targetClass = k.getClazz();
            Class<T> annotation = k.getAnnotation();

            do {
                Field[] fields = targetClass.getDeclaredFields();
                for (Field field : fields) {
                    if (field.isAnnotationPresent(annotation)
                            && (Objects.isNull(function) || function.apply(field.getAnnotation(annotation)))) {
                        return field;
                    }
                }
                targetClass = targetClass.getSuperclass();
            }
            while (targetClass != null && targetClass != Object.class);

            return null;
        });

        if (targetField == null) {
            throw new RuntimeException("In class " + clazz.getName() + ", at least one field must be annotated with @" + annotationClass.getName());
        }
        return targetField;
    }


}
