package com.github.sky.storage.influx;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.annotation.Annotation;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassPropertyAnnotation<T extends Annotation> {

    private Class<?> clazz;

    private Class<T> annotation;

}
