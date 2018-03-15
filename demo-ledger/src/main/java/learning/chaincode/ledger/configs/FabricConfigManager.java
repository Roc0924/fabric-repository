package learning.chaincode.ledger.configs;

import learning.chaincode.ledger.entitys.*;
import lombok.Data;
import org.hyperledger.fabric.sdk.helper.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

/**
 * Create with IntelliJ IDEA
 * Author               : wangzhenpeng
 * Date                 : 2018/2/9
 * Time                 : 16:07
 * Description          :
 */
@Data
@Component
public class FabricConfigManager {

    Logger logger = Logger.getLogger(FabricConfigManager.class.getName());

    private final LedgerProperties ledgerProperties;
    private final Properties sdkProperties = new Properties();
    private final HashMap<String, LedgerOrg> ledgerOrgs = new HashMap<>();
    private final HashMap<String, LedgerOrg> ledgerOrgs1 = new HashMap<>();

    
    private final boolean runningTLS;
    private final boolean runningFabricCATLS;
    private final boolean runningFabricTLS;



    @Autowired
    private FabricConfigManager(
            LedgerProperties ledgerProperties
    ) {
        this.ledgerProperties = ledgerProperties;
        runningTLS = false;
        runningFabricCATLS = runningTLS;
        runningFabricTLS = runningTLS;



        for (Map.Entry<String, OrgConfig> entry : ledgerProperties.getOrgs().entrySet()) {
            String orgName = entry.getValue().getName();
            OrgConfig orgConfig = entry.getValue();
            ledgerOrgs.put(orgName, new LedgerOrg(orgName, orgConfig.getMspId()));

            LedgerOrg ledgerOrg = new LedgerOrg(orgName, orgConfig.getMspId());


            // set peers
            List<PeerConfig> peerConfigs = orgConfig.getPeers();
            for (PeerConfig peerConfig : peerConfigs) {
                ledgerOrg.addPeerLocation(peerConfig.getName(), grpcTLSify(peerConfig.getUrl()));
            }

            // set domainName
            ledgerOrg.setDomainName(orgConfig.getDomName());



            // set orderers
            List<OrdererConfig> ordererConfigs = orgConfig.getOrderers();
            for (OrdererConfig ordererConfig : ordererConfigs) {
                ledgerOrg.addOrdererLocation(ordererConfig.getName(), grpcTLSify(ordererConfig.getUrl()));
            }


            // set event hub
            List<EventHubConfig> eventHubConfigs = orgConfig.getEventHubs();
            for (EventHubConfig eventHubConfig : eventHubConfigs) {
                ledgerOrg.addEventHubLocation(eventHubConfig.getName(), grpcTLSify(eventHubConfig.getUrl()));
            }

            // set ca location
            ledgerOrg.setCALocation(httpTLSify(orgConfig.getCaLocation()));

            if (runningFabricCATLS) {
                String cert = "src/test/fixture/sdkintegration/e2e-2Orgs/channel/crypto-config/peerOrganizations/DNAME/ca/ca.DNAME-cert.pem".replaceAll("DNAME", orgConfig.getDomName());
                File cf = new File(cert);
                if (!cf.exists() || !cf.isFile()) {
                    throw new RuntimeException("TEST is missing cert file " + cf.getAbsolutePath());
                }
                Properties properties = new Properties();
                properties.setProperty("pemFile", cf.getAbsolutePath());

                properties.setProperty("allowAllHostNames", "true"); //testing environment only NOT FOR PRODUCTION!

                ledgerOrg.setCAProperties(properties);
            }


            ledgerOrgs.put(orgName, ledgerOrg);
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

    Collection<LedgerOrg> getIntegrationLedgerOrgs() {
        return Collections.unmodifiableCollection(ledgerOrgs.values());
    }


    public LedgerOrg getIntegrationTestsLedgerOrg(String name) {
        return ledgerOrgs.get(name);

    }

    Properties getOrdererProperties(String name) {

        return getEndPointProperties("orderer", name);

    }

    private Properties getEndPointProperties(final String type, final String name) {

        final String domainName = getDomainName(name);

        File cert = Paths.get(this.getClass().getResource("/").getPath(), "e2e-2Orgs/channel/crypto-config/ordererOrganizations".replace("orderer", type), domainName, type + "s",
                name, "tls/server.crt").toFile();
        if (!cert.exists()) {
            throw new RuntimeException(String.format("Missing cert file for: %s. Could not find at location: %s", name,
                    cert.getAbsolutePath()));
        }

        Properties ret = new Properties();
        ret.setProperty("pemFile", cert.getAbsolutePath());
        //      ret.setProperty("trustServerCertificate", "true"); //testing environment only NOT FOR PRODUCTION!
        ret.setProperty("hostnameOverride", name);
        ret.setProperty("sslProvider", "openSSL");
        ret.setProperty("negotiationType", "TLS");

        return ret;
    }

    private String getDomainName(final String name) {
        int dot = name.indexOf(".");
        if (-1 == dot) {
            return null;
        } else {
            return name.substring(dot + 1);
        }

    }

//    private String getTestChannelPath() {
//
//        return "demo-ledger-admin/target/classes/e2e-2Orgs/channel";
//
//    }


    Properties getPeerProperties(String name) {

        return getEndPointProperties("peer", name);

    }


    Properties getEventHubProperties(String name) {

        return getEndPointProperties("peer", name); //uses same as named peer

    }

    public int getTransactionWaitTime() {
        return ledgerProperties.getTimes().get("invokeWaitTime").intValue();
    }

    public LedgerOrg getOrgByName(String orgName) {
        return this.ledgerOrgs.get(orgName);

    }
}
