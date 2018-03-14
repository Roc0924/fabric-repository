package learning.chaincode.ledger.entitys;


import java.util.HashMap;
import java.util.Map;

/**
 * Create with IntelliJ IDEA
 * Author               : wangzhenpeng
 * Date                 : 2018/3/8
 * Time                 : 下午3:50
 * Description          :
 */

public class LedgerOrgs {
    private Map<String, LedgerOrg> ledgerOrgs = new HashMap<>();

    public Map<String, LedgerOrg> getLedgerOrgs() {
        return ledgerOrgs;
    }

    public void setLedgerOrgs(Map<String, LedgerOrg> ledgerOrgs) {
        this.ledgerOrgs = ledgerOrgs;
    }
}
