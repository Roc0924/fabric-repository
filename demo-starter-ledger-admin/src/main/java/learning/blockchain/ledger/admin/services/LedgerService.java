package learning.blockchain.ledger.admin.services;

import learning.blockchain.fabric.configs.FabricConfig;
import learning.blockchain.fabric.configs.FabricProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Create with IntelliJ IDEA
 * Author               : wangzhenpeng
 * Date                 : 2018/3/16
 * Time                 : 下午2:22
 * Description          :
 */
@Service
public class LedgerService {

    final FabricConfig fabricConfig;

    @Autowired
    public LedgerService(FabricConfig fabricConfig) {
        this.fabricConfig = fabricConfig;
    }

    public void test() {
        System.out.println();
    }
}
