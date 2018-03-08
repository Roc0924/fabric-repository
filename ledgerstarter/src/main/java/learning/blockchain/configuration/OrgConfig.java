package learning.blockchain.configuration;

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
    private List<PeerCofnig> peers;
    private String ordererLocations;
    private String eventHubLocations;

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

    public String getOrdererLocations() {
        return ordererLocations;
    }

    public void setOrdererLocations(String ordererLocations) {
        this.ordererLocations = ordererLocations;
    }

    public String getEventHubLocations() {
        return eventHubLocations;
    }

    public void setEventHubLocations(String eventHubLocations) {
        this.eventHubLocations = eventHubLocations;
    }

    public List<PeerCofnig> getPeers() {
        return peers;
    }

    public void setPeers(List<PeerCofnig> peers) {
        this.peers = peers;
    }
}
