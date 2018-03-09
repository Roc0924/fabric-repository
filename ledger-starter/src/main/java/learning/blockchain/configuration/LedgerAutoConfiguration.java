package learning.blockchain.configuration;

import learning.blockchain.entitys.LedgerOrg;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.helper.Utils;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Create with IntelliJ IDEA
 * Author               : wangzhenpeng
 * Date                 : 2018/3/8
 * Time                 : 上午11:46
 * Description          :
 */
@Configuration
@EnableConfigurationProperties(LedgerProperties.class)
@ConditionalOnClass({HFClient.class, LedgerChannels.class})
@ConditionalOnProperty(prefix = "ledger", value = "enable", matchIfMissing = true)
public class LedgerAutoConfiguration {
    final LedgerProperties ledgerProperties;

    @Autowired
    public LedgerAutoConfiguration(LedgerProperties ledgerProperties) {
        this.ledgerProperties = ledgerProperties;
    }

    @Bean
    @ConditionalOnMissingBean(HFClient.class)
    public HFClient hFClient() {
        HFClient hfClient = HFClient.createNewInstance();

        try {
            hfClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        } catch (CryptoException | InvalidArgumentException e) {
            e.printStackTrace();
        }

        return hfClient;
    }


    @Bean
    @ConditionalOnMissingBean(LedgerOrgs.class)
    public LedgerOrgs ledgerOrgs() {
        LedgerOrgs ledgerOrgs = new LedgerOrgs();
        Map<String, LedgerOrg> ledgerOrgMap = new HashMap<>();

        Map<String, OrgConfig> orgConfigs = ledgerProperties.getOrgs();

        for (String orgStr : orgConfigs.keySet()) {
            OrgConfig orgConfig = orgConfigs.get(orgStr);
            LedgerOrg ledgerOrg = new LedgerOrg(orgConfig.getName(), orgConfig.getMspId());


            ledgerOrgs.getLedgerOrgs().put(orgConfig.getName(), ledgerOrg);
            // set peers
            for (PeerConfig peerConfig : orgConfig.getPeers()) {
                ledgerOrg.addPeerLocation(peerConfig.getName(), grpcTLS(peerConfig.getUrl()));
            }

            // set domainName
            ledgerOrg.setDomainName(orgConfig.getDomName());

            // set orderers
            for (OrdererConfig ordererConfig : orgConfig.getOrderers()) {
                ledgerOrg.addOrdererLocation(ordererConfig.getName(), grpcTLS(ordererConfig.getUrl()));
            }

            // set eventHubs
            for (EventHubConfig eventHubConfig : orgConfig.getEventHubs()) {
                ledgerOrg.addEventHubLocation(eventHubConfig.getName(), grpcTLS(eventHubConfig.getUrl()));
            }

            // set ca location
            ledgerOrg.setCALocation(httpTLSify(orgConfig.getCaLocation()));

            if (ledgerProperties.isRunWithTls()) {
                String cert = "src/test/fixture/sdkintegration/e2e-2Orgs/channel/crypto-config/peerOrganizations/DNAME/ca/ca.DNAME-cert.pem"
                        .replaceAll("DNAME", orgConfig.getDomName());
                File certFile = new File(cert);
                if (!certFile.exists() || !certFile.isFile()) {
                    throw new RuntimeException("missing cert file " + certFile.getAbsolutePath());
                }
                Properties properties = new Properties();
                properties.setProperty("pemFile", certFile.getAbsolutePath());

                properties.setProperty("allowAllHostNames", "true"); //testing environment only NOT FOR PRODUCTION!

                ledgerOrg.setCAProperties(properties);
            }

            ledgerOrgMap.put(ledgerOrg.getName(), ledgerOrg);
        }

        ledgerOrgs.setLedgerOrgs(ledgerOrgMap);
        return ledgerOrgs;
    }


    @Bean
    @ConditionalOnMissingBean(LedgerChannels.class)
    public LedgerChannels channels() {
        LedgerChannels ledgerChannels = new LedgerChannels();













        return ledgerChannels;
    }


    /**
     * grpc是否开启TLS
     * @param url
     * @return
     */
    private String grpcTLS(String url) {
        url = url.trim();
        Exception e = Utils.checkGrpcUrl(url);
        if (e != null) {
            throw new RuntimeException(String.format("Bad TEST parameters for grpc url %s", url), e);
        }

        return ledgerProperties.isRunWithTls() ? url.replace("grpc://", "grpcs://") : url;
    }

    /**
     * http是否开启TLS
     * @param url
     * @return
     */
    private String httpTLSify(String url) {
        url = url.trim();

        return ledgerProperties.isRunWithTls() ? url.replaceFirst("^http://", "https://") : url;
    }



}
