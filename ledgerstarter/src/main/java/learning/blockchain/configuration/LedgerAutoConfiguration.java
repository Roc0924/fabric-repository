package learning.blockchain.configuration;

import learning.blockchain.entitys.LedgerOrg;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

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

        OrgConfig orgConfig = ledgerProperties.getOrgs().get("org1");
        System.out.println(orgConfig);

        return hfClient;
    }


    @Bean
    @ConditionalOnMissingBean(LedgerOrgs.class)
    public LedgerOrgs ledgerOrgs() {
        LedgerOrgs ledgerOrgs = new LedgerOrgs();

        Map<String, OrgConfig> orgConfigs = ledgerProperties.getOrgs();

        for (String orgStr : orgConfigs.keySet()) {
            OrgConfig orgConfig = orgConfigs.get(orgStr);
            LedgerOrg ledgerOrg = new LedgerOrg(orgConfig.getName(), orgConfig.getMspId());

            // set peers



            ledgerOrgs.getLedgerOrgs().put(orgConfig.getName(), ledgerOrg);

        }


        return ledgerOrgs;
    }


    @Bean
    @ConditionalOnMissingBean(LedgerChannels.class)
    public LedgerChannels channels() {
        LedgerChannels ledgerChannels = new LedgerChannels();













        return ledgerChannels;
    }









}
