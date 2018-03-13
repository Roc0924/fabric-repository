package learning.chaincode.fabricadmin.configs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Create with IntelliJ IDEA
 * Author               : wangzhenpeng
 * Date                 : 2018/2/9
 * Time                 : 14:21
 * Description          :
 */
@Data
@Component
@ConfigurationProperties(prefix = "hyperledger.fabric")
public class FabricConfig {
    private String gossipWaitTime;
    private String invokeWaitTime;
    private String deployWaitTime;
    private String proposalWaitTime;
    private String gossipWaitTimeKey;
    private String invokeWaitTimeKey;
    private String deployWaitTimeKey;
    private String proposalWaitTimeKey;


    private String peerOrg1Mspid;
    private String peerOrg1Domname;
    private String peerOrg1CaLocation;
    private String peerOrg1PeerLocations;
    private String peerOrg1OrdererLocations;
    private String peerOrg1EventhubLocations;

    private String peerOrg2Mspid;
    private String peerOrg2Domname;
    private String peerOrg2CaLocation;
    private String peerOrg2PeerLocations;
    private String peerOrg2OrdererLocations;
    private String peerOrg2EventhubLocations;

    private String integrationTestsOrg;
    private String integrationTestsTls;

    private boolean runningTLS;

    private String adminName;
    private String user1Name;
    private String user1Secret;
    private String fixturesPath;
    private String chainCodeName;
    private String chainCodePath;
    private String chainCodeVersion;
    private String fooChannelName;
    private String barChannelName;

    private String channelPath;
    private String peerOrganizations;


}
