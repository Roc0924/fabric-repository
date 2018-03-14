package learning.chaincode.fabricadmin.entitys;

import java.util.List;

/**
 * Create with IntelliJ IDEA
 * Author               : wangzhenpeng
 * Date                 : 2018/3/8
 * Time                 : 下午2:48
 * Description          :
 */

public class OrgConfig {
    private String name;
    private String mspId;
    private String domName;
    private String caLocation;
    private List<PeerConfig> peers;
    private List<OrdererConfig> orderers;
    private List<EventHubConfig> eventHubs;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMspId() {
        return mspId;
    }

    public void setMspId(String mspId) {
        this.mspId = mspId;
    }

    public String getDomName() {
        return domName;
    }

    public void setDomName(String domName) {
        this.domName = domName;
    }

    public String getCaLocation() {
        return caLocation;
    }

    public void setCaLocation(String caLocation) {
        this.caLocation = caLocation;
    }

    public List<PeerConfig> getPeers() {
        return peers;
    }

    public void setPeers(List<PeerConfig> peers) {
        this.peers = peers;
    }

    public List<OrdererConfig> getOrderers() {
        return orderers;
    }

    public void setOrderers(List<OrdererConfig> orderers) {
        this.orderers = orderers;
    }

    public List<EventHubConfig> getEventHubs() {
        return eventHubs;
    }

    public void setEventHubs(List<EventHubConfig> eventHubs) {
        this.eventHubs = eventHubs;
    }
}
