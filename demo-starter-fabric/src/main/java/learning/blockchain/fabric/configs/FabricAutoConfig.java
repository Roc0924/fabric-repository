package learning.blockchain.fabric.configs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Create with IntelliJ IDEA
 * Author               : wangzhenpeng
 * Date                 : 2018/3/16
 * Time                 : 下午4:49
 * Description          :
 */
@Configuration
@EnableConfigurationProperties(FabricProperties.class)
@ConditionalOnProperty(prefix = "fabric", value = "enable", matchIfMissing = true)
public class FabricAutoConfig {
    final FabricProperties fabricProperties;

    @Autowired
    public FabricAutoConfig(FabricProperties fabricProperties) {
        this.fabricProperties = fabricProperties;
    }

    @Autowired
    @Bean
    @ConditionalOnMissingBean(FabricConfig.class)
    public FabricConfig fabricConfig(FabricProperties fabricProperties) {
        return new FabricConfig(fabricProperties);
    }

}
