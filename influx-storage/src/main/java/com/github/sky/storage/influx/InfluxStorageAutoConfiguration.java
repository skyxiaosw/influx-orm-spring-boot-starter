package com.github.sky.storage.influx;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.spring.influx.InfluxDB2AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = {InfluxDB2AutoConfiguration.class})
@EnableConfigurationProperties(InfluxDbExtensionProperties.class)
public class InfluxStorageAutoConfiguration {



    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnSingleCandidate(InfluxDBClient.class)
    public InfluxDbService<?> influxDbService(InfluxDBClient influxDBClient, InfluxDbExtensionProperties properties) {
        return new InfluxDbServiceImpl<>(influxDBClient, properties);
    }



}
