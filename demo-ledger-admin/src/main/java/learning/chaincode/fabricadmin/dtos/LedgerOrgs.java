package learning.chaincode.fabricadmin.dtos;


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
    private Map<String, SampleOrg> sampleOrgs = new HashMap<>();

    public Map<String, SampleOrg> getSampleOrgs() {
        return sampleOrgs;
    }

    public void setSampleOrgs(Map<String, SampleOrg> sampleOrgs) {
        this.sampleOrgs = sampleOrgs;
    }
}
