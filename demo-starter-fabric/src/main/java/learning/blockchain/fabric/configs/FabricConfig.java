package learning.blockchain.fabric.configs;

import learning.blockchain.fabric.entitys.*;
import org.hyperledger.fabric.sdk.helper.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Create with IntelliJ IDEA
 * Author               : wangzhenpeng
 * Date                 : 2018/3/16
 * Time                 : 下午4:59
 * Description          :
 */
@Component
public class FabricConfig {
    private FabricProperties fabricProperties;

    private final HashMap<String, FabricOrg> fabricOrgs = new HashMap<>();
    private final boolean runningTLS;
    private final boolean runningFabricCATLS;
    private final boolean runningFabricTLS;

    @Autowired
    public FabricConfig(
            FabricProperties fabricProperties
    ) {
        this.fabricProperties = fabricProperties;
        runningTLS = false;
        runningFabricCATLS = runningTLS;
        runningFabricTLS = runningTLS;



        for (Map.Entry<String, OrgConfig> entry : fabricProperties.getOrgs().entrySet()) {
            String orgName = entry.getValue().getName();
            OrgConfig orgConfig = entry.getValue();
            fabricOrgs.put(orgName, new FabricOrg(orgName, orgConfig.getMspId()));

            FabricOrg fabricOrg = new FabricOrg(orgName, orgConfig.getMspId());


            // set peers
            List<PeerConfig> peerConfigs = orgConfig.getPeers();
            for (PeerConfig peerConfig : peerConfigs) {
                fabricOrg.addPeerLocation(peerConfig.getName(), grpcTLSify(peerConfig.getUrl()));
            }

            // set domainName
            fabricOrg.setDomainName(orgConfig.getDomName());



            // set orderers
            List<OrdererConfig> ordererConfigs = orgConfig.getOrderers();
            for (OrdererConfig ordererConfig : ordererConfigs) {
                fabricOrg.addOrdererLocation(ordererConfig.getName(), grpcTLSify(ordererConfig.getUrl()));
            }


            // set event hub
            List<EventHubConfig> eventHubConfigs = orgConfig.getEventHubs();
            for (EventHubConfig eventHubConfig : eventHubConfigs) {
                fabricOrg.addEventHubLocation(eventHubConfig.getName(), grpcTLSify(eventHubConfig.getUrl()));
            }

            // set ca location
            fabricOrg.setCALocation(httpTLSify(orgConfig.getCaLocation()));

            if (runningFabricCATLS) {
                String cert = "src/test/fixture/sdkintegration/e2e-2Orgs/channel/crypto-config/peerOrganizations/DNAME/ca/ca.DNAME-cert.pem".replaceAll("DNAME", orgConfig.getDomName());
                File cf = new File(cert);
                if (!cf.exists() || !cf.isFile()) {
                    throw new RuntimeException("TEST is missing cert file " + cf.getAbsolutePath());
                }
                Properties properties = new Properties();
                properties.setProperty("pemFile", cf.getAbsolutePath());

                properties.setProperty("allowAllHostNames", "true"); //testing environment only NOT FOR PRODUCTION!

                fabricOrg.setCAProperties(properties);
            }


            fabricOrgs.put(orgName, fabricOrg);
        }


    }


    private String grpcTLSify(String location) {
        location = location.trim();
        Exception e = Utils.checkGrpcUrl(location);
        if (e != null) {
            throw new RuntimeException(String.format("Bad TEST parameters for grpc url %s", location), e);
        }
        return runningFabricTLS ?
                location.replaceFirst("^grpc://", "grpcs://") : location;

    }

    private String httpTLSify(String location) {
        location = location.trim();

        return runningFabricCATLS ?
                location.replaceFirst("^http://", "https://") : location;
    }
}
