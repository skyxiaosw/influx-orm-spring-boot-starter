package com.github.sky.orm.influx;

import com.data.source.orm.influx.service.IInfluxClientHandler;
import com.data.source.orm.influx.service.IParamResolve;
import com.data.source.orm.influx.service.InfluxClientHandler;
import com.data.source.orm.influx.service.ParamResolveService;
import com.data.source.storage.influx.InfluxDbExtensionProperties;
import com.data.source.storage.influx.InfluxDbService;
import com.influxdb.spring.influx.InfluxDB2Properties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(InfluxDbExtensionProperties.class)
public class InfluxPlusAutoConfiguration {

    @Bean
    public IParamResolve paramResolve() {
        return new ParamResolveService();
    }

    @Bean
    public IInfluxClientHandler influxClientHandler(InfluxDbService influxDbService, IParamResolve paramResolve,
    InfluxDB2Properties influxDB2Properties) {
        return new InfluxClientHandler(influxDbService, paramResolve,influxDB2Properties);
    }
}
