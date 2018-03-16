package learning.blockchain.fabric.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Create with IntelliJ IDEA
 * Author               : wangzhenpeng
 * Date                 : 2018/3/16
 * Time                 : 下午2:35
 * Description          :
 */

@Configuration
@EnableConfigurationProperties(FabricProperties.class)
public class FabricAutoConfig {
    final FabricProperties fabricProperties;

    public FabricAutoConfig(FabricProperties fabricProperties) {
        this.fabricProperties = fabricProperties;
    }
}
