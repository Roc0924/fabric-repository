package learning.blockchain.ledgeradmin.configs;

import learning.chaincode.fabricadmin.dtos.SampleOrg;
import lombok.Data;
import org.hyperledger.fabric.sdk.helper.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private final FabricConfig fabricConfig;
    private Pattern orgPat = null;
    private final Properties sdkProperties = new Properties();
    private final HashMap<String, SampleOrg> sampleOrgs = new HashMap<>();
    
    
    private final boolean runningTLS;
    private final boolean runningFabricCATLS;
    private final boolean runningFabricTLS;



    @Autowired
    private FabricConfigManager(FabricConfig fabricConfig) {
        this.orgPat = Pattern.compile("^" + Pattern.quote(fabricConfig.getIntegrationTestsOrg()) + "([^\\.]+)\\.mspid$");
        defaultProperty(fabricConfig.getGossipWaitTimeKey(), fabricConfig.getGossipWaitTime());
        defaultProperty(fabricConfig.getInvokeWaitTimeKey(), fabricConfig.getInvokeWaitTime());
        defaultProperty(fabricConfig.getDeployWaitTimeKey(), fabricConfig.getDeployWaitTime());
        defaultProperty(fabricConfig.getProposalWaitTimeKey(), fabricConfig.getProposalWaitTime());


        defaultProperty(fabricConfig.getIntegrationTestsOrg() + "peerOrg1.mspid", fabricConfig.getPeerOrg1Mspid());
        defaultProperty(fabricConfig.getIntegrationTestsOrg() + "peerOrg1.domname", fabricConfig.getPeerOrg1Domname());
        defaultProperty(fabricConfig.getIntegrationTestsOrg() + "peerOrg1.ca_location", fabricConfig.getPeerOrg1CaLocation());
        defaultProperty(fabricConfig.getIntegrationTestsOrg() + "peerOrg1.peer_locations", fabricConfig.getPeerOrg1PeerLocations());
        defaultProperty(fabricConfig.getIntegrationTestsOrg() + "peerOrg1.orderer_locations", fabricConfig.getPeerOrg1OrdererLocations());
        defaultProperty(fabricConfig.getIntegrationTestsOrg() + "peerOrg1.eventhub_locations", fabricConfig.getPeerOrg1EventhubLocations());
        defaultProperty(fabricConfig.getIntegrationTestsOrg() + "peerOrg2.mspid", fabricConfig.getPeerOrg2Mspid());
        defaultProperty(fabricConfig.getIntegrationTestsOrg() + "peerOrg2.domname", fabricConfig.getPeerOrg2Domname());
        defaultProperty(fabricConfig.getIntegrationTestsOrg() + "peerOrg2.ca_location", fabricConfig.getPeerOrg2CaLocation());
        defaultProperty(fabricConfig.getIntegrationTestsOrg() + "peerOrg2.peer_locations", fabricConfig.getPeerOrg2PeerLocations());
        defaultProperty(fabricConfig.getIntegrationTestsOrg() + "peerOrg2.orderer_locations", fabricConfig.getPeerOrg2OrdererLocations());
        defaultProperty(fabricConfig.getIntegrationTestsOrg() + "peerOrg2.eventhub_locations", fabricConfig.getPeerOrg2EventhubLocations());

        defaultProperty(fabricConfig.getIntegrationTestsTls(), null);

        runningTLS = null != sdkProperties.getProperty(fabricConfig.getIntegrationTestsTls(), null);
        runningFabricCATLS = runningTLS;
        runningFabricTLS = runningTLS;

        for (Map.Entry<Object, Object> x : sdkProperties.entrySet()) {
            final String key = x.getKey() + "";
            final String val = x.getValue() + "";

            if (key.startsWith(fabricConfig.getIntegrationTestsOrg())) {

                Matcher match = orgPat.matcher(key);

                if (match.matches() && match.groupCount() == 1) {
                    String orgName = match.group(1).trim();
                    sampleOrgs.put(orgName, new SampleOrg(orgName, val.trim()));

                }
            }
        }



        for (Map.Entry<String, SampleOrg> org : sampleOrgs.entrySet()) {
            final SampleOrg sampleOrg = org.getValue();
            final String orgName = org.getKey();

            String peerNames = sdkProperties.getProperty(fabricConfig.getIntegrationTestsOrg() + orgName + ".peer_locations");
            String[] ps = peerNames.split("[ \t]*,[ \t]*");
            for (String peer : ps) {
                String[] nl = peer.split("[ \t]*@[ \t]*");
                sampleOrg.addPeerLocation(nl[0], grpcTLSify(nl[1]));
            }

            final String domainName = sdkProperties.getProperty(fabricConfig.getIntegrationTestsOrg() + orgName + ".domname");

            sampleOrg.setDomainName(domainName);

            String ordererNames = sdkProperties.getProperty(fabricConfig.getIntegrationTestsOrg() + orgName + ".orderer_locations");
            ps = ordererNames.split("[ \t]*,[ \t]*");
            for (String peer : ps) {
                String[] nl = peer.split("[ \t]*@[ \t]*");
                sampleOrg.addOrdererLocation(nl[0], grpcTLSify(nl[1]));
            }

            String eventHubNames = sdkProperties.getProperty(fabricConfig.getIntegrationTestsOrg() + orgName + ".eventhub_locations");
            ps = eventHubNames.split("[ \t]*,[ \t]*");
            for (String peer : ps) {
                String[] nl = peer.split("[ \t]*@[ \t]*");
                sampleOrg.addEventHubLocation(nl[0], grpcTLSify(nl[1]));
            }

            sampleOrg.setCALocation(httpTLSify(sdkProperties.getProperty((fabricConfig.getIntegrationTestsOrg() + org.getKey() + ".ca_location"))));

            if (runningFabricCATLS) {
                String cert = "src/test/fixture/sdkintegration/e2e-2Orgs/channel/crypto-config/peerOrganizations/DNAME/ca/ca.DNAME-cert.pem".replaceAll("DNAME", domainName);
                File cf = new File(cert);
                if (!cf.exists() || !cf.isFile()) {
                    throw new RuntimeException("TEST is missing cert file " + cf.getAbsolutePath());
                }
                Properties properties = new Properties();
                properties.setProperty("pemFile", cf.getAbsolutePath());

                properties.setProperty("allowAllHostNames", "true"); //testing environment only NOT FOR PRODUCTION!

                sampleOrg.setCAProperties(properties);
            }
        }


        this.fabricConfig = fabricConfig;
    }



    private void defaultProperty(String key, String value) {

        String ret = System.getProperty(key);
        if (ret != null) {
            sdkProperties.put(key, ret);
        } else {
            String envKey = key.toUpperCase().replaceAll("\\.", "_");
            ret = System.getenv(envKey);
            if (null != ret) {
                sdkProperties.put(key, ret);
            } else {
                if (null == sdkProperties.getProperty(key) && value != null) {
                    sdkProperties.put(key, value);
                }

            }

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

    Collection<SampleOrg> getIntegrationSampleOrgs() {
        return Collections.unmodifiableCollection(sampleOrgs.values());
    }


    public SampleOrg getIntegrationTestsSampleOrg(String name) {
        return sampleOrgs.get(name);

    }

    Properties getOrdererProperties(String name) {

        return getEndPointProperties("orderer", name);

    }

    private Properties getEndPointProperties(final String type, final String name) {

        final String domainName = getDomainName(name);

        File cert = Paths.get(getTestChannelPath(), "crypto-config/ordererOrganizations".replace("orderer", type), domainName, type + "s",
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

    private String getTestChannelPath() {

        return "demo/target/classes/e2e-2Orgs/channel";

    }


    Properties getPeerProperties(String name) {

        return getEndPointProperties("peer", name);

    }


    Properties getEventHubProperties(String name) {

        return getEndPointProperties("peer", name); //uses same as named peer

    }

    public int getTransactionWaitTime() {
        return Integer.parseInt(getProperty(fabricConfig.getInvokeWaitTimeKey()));
    }

    private String getProperty(String property) {

        String ret = sdkProperties.getProperty(property);

        if (null == ret) {
            System.out.println(String.format("No configuration value found for '%s'", property));
        }
        return ret;
    }
}
