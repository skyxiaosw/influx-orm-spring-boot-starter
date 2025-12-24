package com.github.sky.orm.influx.core;

import com.data.source.orm.influx.annotation.EnableInfluxMappers;
import com.data.source.orm.influx.annotation.InfluxMapper;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * @Description:
 * @author: sky
 * @date: 2024/1/5 13:53
 */
public class InfluxMappersRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {

    private ResourceLoader resourceLoader;

    private Environment environment;

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }


    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        registerInfluxMappers(importingClassMetadata, registry);
    }

    public void registerInfluxMappers(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        /**
         * 1. 获取注解属性
         * 2. 获取相关扫描器
         * 3. 添加注解过滤条件
         * 4. 添加扫描包
         * 5. 注册bean
         */

        LinkedHashSet<BeanDefinition> candidateComponents = new LinkedHashSet<>();
        Map<String, Object> attrs = metadata.getAnnotationAttributes(EnableInfluxMappers.class.getName());
        final Class<?>[] mappers = attrs == null ? null : (Class<?>[]) attrs.get("mappers");
        if (mappers == null || mappers.length == 0) {
            ClassPathScanningCandidateComponentProvider scanner = getScanner();
            scanner.setResourceLoader(this.resourceLoader);
            scanner.addIncludeFilter(new AnnotationTypeFilter(InfluxMapper.class));
            Set<String> basePackages = getBasePackages(metadata);
            for (String basePackage : basePackages) {
                candidateComponents.addAll(scanner.findCandidateComponents(basePackage));
            }
        } else {
            for (Class<?> clazz : mappers) {
                candidateComponents.add(new AnnotatedGenericBeanDefinition(clazz));
            }
        }

        for (BeanDefinition candidateComponent : candidateComponents) {
            if (candidateComponent instanceof AnnotatedBeanDefinition) {
                // verify annotated class is an interface
                AnnotatedBeanDefinition beanDefinition = (AnnotatedBeanDefinition) candidateComponent;
                AnnotationMetadata annotationMetadata = beanDefinition.getMetadata();
                Assert.isTrue(annotationMetadata.isInterface(), "@InfluxClient can only be specified on an interface");

                // todo 获取注解的属性 暂时只做标识 不做其他操作
                Map<String, Object> attributes = annotationMetadata
                        .getAnnotationAttributes(InfluxMapper.class.getCanonicalName());

                // 注册InfluxClient客户端 和相关方法
                registerInfluxMapper(registry, annotationMetadata, attributes);
            }
        }
    }

    private void registerInfluxMapper(BeanDefinitionRegistry registry, AnnotationMetadata annotationMetadata,
                                      Map<String, Object> attributes) {
        String className = annotationMetadata.getClassName();

        registerInfluxClientBeanDefinition(className, attributes, registry);
    }

    private void registerInfluxClientBeanDefinition(String className, Map<String, Object> attributes,
                                                    BeanDefinitionRegistry registry) {

        Class clazz = ClassUtils.resolveClassName(className, null);
        ConfigurableBeanFactory beanFactory = registry instanceof ConfigurableBeanFactory
                ? (ConfigurableBeanFactory) registry : null;
        InfluxMapperFactoryBean factoryBean = new InfluxMapperFactoryBean();
        factoryBean.setBeanFactory(beanFactory);
        factoryBean.setClientName(Objects.nonNull(attributes.get("name")) ? attributes.get("name").toString() : "");
        factoryBean.setName(className);
        factoryBean.setType(clazz);
        BeanDefinitionBuilder definition = BeanDefinitionBuilder.genericBeanDefinition(clazz, () -> {


            return factoryBean.getObject();
        });
        definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
        definition.setLazyInit(true);
        AbstractBeanDefinition beanDefinition = definition.getBeanDefinition();
        beanDefinition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, className);
        beanDefinition.setAttribute("influxMappersRegistrarFactoryBean", factoryBean);

        // has a default, won't be null
        boolean primary = (Boolean) attributes.get("primary");

        beanDefinition.setPrimary(primary);

        //bean注册
        BeanDefinitionHolder holder = new BeanDefinitionHolder(beanDefinition, className);
        BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
    }

    protected ClassPathScanningCandidateComponentProvider getScanner() {
        return new ClassPathScanningCandidateComponentProvider(false, this.environment) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                boolean isCandidate = false;
                if (beanDefinition.getMetadata().isIndependent()) {
                    if (!beanDefinition.getMetadata().isAnnotation()) {
                        isCandidate = true;
                    }
                }
                return isCandidate;
            }
        };
    }
    protected Set<String> getBasePackages(AnnotationMetadata importingClassMetadata) {
        Map<String, Object> attributes = importingClassMetadata
                .getAnnotationAttributes(EnableInfluxMappers.class.getCanonicalName());

        if (CollectionUtils.isEmpty(attributes)) {
            return Collections.emptySet();
        }
        Set<String> basePackages = new HashSet<>();
        for (String pkg : (String[]) attributes.get("value")) {
            if (StringUtils.hasText(pkg)) {
                basePackages.add(pkg);
            }
        }

        if (basePackages.isEmpty()) {
            basePackages.add(ClassUtils.getPackageName(importingClassMetadata.getClassName()));
        }
        return basePackages;
    }




    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
