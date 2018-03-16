package learning.blockchain.fabric.config;

import learning.blockchain.fabric.entity.ChainCodeConfig;
import learning.blockchain.fabric.entity.ChannelConfig;
import learning.blockchain.fabric.entity.OrgConfig;
import learning.blockchain.fabric.entity.UserConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Create with IntelliJ IDEA
 * Author               : wangzhenpeng
 * Date                 : 2018/3/16
 * Time                 : 下午2:24
 * Description          :
 */
@ConfigurationProperties(prefix = "fabric")
public class FabricProperties {
    private Map<String, OrgConfig> orgs;                                               // 组织
    private Map<String, UserConfig> users;                                             // 用户列表
    private List<ChannelConfig> channelList;                                           // 频道列表
    private String channelPath;                                                        // 频道路径
    private List<Map<String, String>> chainCodeList;                                   // 智能合约列表
    private Map<String, ChainCodeConfig> chainCodes;
    private Map<String, Long> times;                                                        // 时间
    private boolean runWithTls;                                                        // 是否开启tls
    private String cryptoConfigPath;                                                   // crypto配置路径
    private String currentOrgName;                                                     // 当前组织名称
    private String currentUserName;
    private String currentChaincodeVersion;
    private String currentChaincodeName;


    public Map<String, OrgConfig> getOrgs() {
        return orgs;
    }

    public void setOrgs(Map<String, OrgConfig> orgs) {
        this.orgs = orgs;
    }

    public Map<String, UserConfig> getUsers() {
        return users;
    }

    public void setUsers(Map<String, UserConfig> users) {
        this.users = users;
    }

    public List<ChannelConfig> getChannelList() {
        return channelList;
    }

    public void setChannelList(List<ChannelConfig> channelList) {
        this.channelList = channelList;
    }

    public String getChannelPath() {
        return channelPath;
    }

    public void setChannelPath(String channelPath) {
        this.channelPath = channelPath;
    }

    public List<Map<String, String>> getChainCodeList() {
        return chainCodeList;
    }

    public void setChainCodeList(List<Map<String, String>> chainCodeList) {
        this.chainCodeList = chainCodeList;
    }

    public Map<String, ChainCodeConfig> getChainCodes() {
        return chainCodes;
    }

    public void setChainCodes(Map<String, ChainCodeConfig> chainCodes) {
        this.chainCodes = chainCodes;
    }

    public Map<String, Long> getTimes() {
        return times;
    }

    public void setTimes(Map<String, Long> times) {
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

    public String getCurrentOrgName() {
        return currentOrgName;
    }

    public void setCurrentOrgName(String currentOrgName) {
        this.currentOrgName = currentOrgName;
    }

    public String getCurrentUserName() {
        return currentUserName;
    }

    public void setCurrentUserName(String currentUserName) {
        this.currentUserName = currentUserName;
    }

    public String getCurrentChaincodeVersion() {
        return currentChaincodeVersion;
    }

    public void setCurrentChaincodeVersion(String currentChaincodeVersion) {
        this.currentChaincodeVersion = currentChaincodeVersion;
    }

    public String getCurrentChaincodeName() {
        return currentChaincodeName;
    }

    public void setCurrentChaincodeName(String currentChaincodeName) {
        this.currentChaincodeName = currentChaincodeName;
    }
}
