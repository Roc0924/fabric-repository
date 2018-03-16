package learning.blockchain.ledger.admin.services;

import learning.blockchain.fabric.config.FabricProperties;
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
    final FabricProperties fabricProperties;

    public LedgerService(FabricProperties fabricProperties) {
        this.fabricProperties = fabricProperties;
    }

    public void test() {
        System.out.println();
    }
}
