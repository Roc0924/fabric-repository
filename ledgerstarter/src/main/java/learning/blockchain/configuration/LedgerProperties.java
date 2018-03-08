package learning.blockchain.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/**
 * Create with IntelliJ IDEA
 * Author               : wangzhenpeng
 * Date                 : 2018/3/8
 * Time                 : 上午11:48
 * Description          : 账本配置
 */
@ConfigurationProperties(prefix = "ledger")
public class LedgerProperties {


    private Map<String, OrgConfig> orgs;                    // 组织
    private List<Map<String, String>> userList;             // 用户列表
    private List<Map<String, String>> channelList;          // 频道列表
    private List<Map<String, String>> chainCodeList;        // 智能合约列表
    private List<String> times;                             // 时间
    private boolean runWithTls;                             // 是否开启tls
    private String cryptoConfigPath;                        // crypto配置路径

    public Map<String, OrgConfig> getOrgs() {
        return orgs;
    }

    public void setOrgs(Map<String, OrgConfig> orgs) {
        this.orgs = orgs;
    }

    public List<Map<String, String>> getUserList() {
        return userList;
    }

    public void setUserList(List<Map<String, String>> userList) {
        this.userList = userList;
    }

    public List<Map<String, String>> getChannelList() {
        return channelList;
    }

    public void setChannelList(List<Map<String, String>> channelList) {
        this.channelList = channelList;
    }

    public List<Map<String, String>> getChainCodeList() {
        return chainCodeList;
    }

    public void setChainCodeList(List<Map<String, String>> chainCodeList) {
        this.chainCodeList = chainCodeList;
    }

    public List<String> getTimes() {
        return times;
    }

    public void setTimes(List<String> times) {
        this.times = times;
    }

    public boolean isRunWithTls() {
        return runWithTls;
    }

    public void setRunWithTls(boolean runWithTls) {
        this.runWithTls = runWithTls;
    }

    public String getCryptoConfigPath() {
        return cryptoConfigPath;
    }

    public void setCryptoConfigPath(String cryptoConfigPath) {
        this.cryptoConfigPath = cryptoConfigPath;
    }
}
