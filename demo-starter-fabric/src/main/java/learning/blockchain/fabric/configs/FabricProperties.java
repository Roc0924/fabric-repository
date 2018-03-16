package learning.blockchain.fabric.configs;

import learning.blockchain.fabric.entitys.ChainCodeConfig;
import learning.blockchain.fabric.entitys.OrgConfig;
import learning.blockchain.fabric.entitys.UserConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Create with IntelliJ IDEA
 * Author               : wangzhenpeng
 * Date                 : 2018/3/16
 * Time                 : 下午4:37
 * Description          :
 */
@Data
@ConfigurationProperties(prefix = "fabric")
public class FabricProperties {

    private Map<String, OrgConfig> orgs;                                                // 组织
    private Map<String, UserConfig> users;                                              // 用户列表
    private Map<String, ChainCodeConfig> chainCodes;                                    // 智能合约
}
