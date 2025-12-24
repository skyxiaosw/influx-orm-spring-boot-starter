package com.github.sky.storage.influx;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("spring.influx")
public class InfluxDbExtensionProperties {

    /**
     * 批量操作触发的点的数量。达到这个数量后，批量写操作将被触发。
     */
    private int actions = 2000;

    /**
     * 批量操作触发的时间间隔（以毫秒为单位）
     */
    private int flushDuration = 200;


}
