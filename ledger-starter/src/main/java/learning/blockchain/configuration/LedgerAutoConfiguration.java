package learning.blockchain.configuration;

import learning.blockchain.entitys.LedgerOrg;
import learning.blockchain.entitys.LedgerStore;
import learning.blockchain.entitys.LedgerUser;
import learning.blockchain.utils.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.hyperledger.fabric.sdk.helper.Utils;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.exception.EnrollmentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

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

    Logger logger = Logger.getLogger(LedgerAutoConfiguration.class);

    final LedgerProperties ledgerProperties;

    @Autowired
    public LedgerAutoConfiguration(LedgerProperties ledgerProperties) {
        this.ledgerProperties = ledgerProperties;
    }






    @Bean
    @Autowired
    @ConditionalOnMissingBean(LedgerOrgs.class)
    public LedgerOrgs ledgerOrgs(LedgerProperties ledgerProperties) {
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






            try {
                File ledgerStoreFile = new File(System.getProperty("java.io.tmpdir") + "/HFC.properties");
                if (ledgerStoreFile.exists()) {
                    boolean result = ledgerStoreFile.delete();
                    if (result) {
                        logger.info("remove HFC.properties");
                    }
                }

                final LedgerStore ledgerStore = new LedgerStore(ledgerStoreFile);


                // set admin

                ledgerOrg.setCAClient(HFCAClient.createNewInstance(ledgerOrg.getCALocation(), ledgerOrg.getCAProperties()));


                HFCAClient hfcaClient = ledgerOrg.getCAClient();
                hfcaClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
                UserConfig userConfig = ledgerProperties.getUsers().get("admin");
                LedgerUser admin = ledgerStore.getMember(userConfig.getName(), orgConfig.getName());

                if (!admin.isEnrolled()) {
                    admin.setEnrollment(hfcaClient.enroll(admin.getName(), userConfig.getSecret()));
                    admin.setMspId(ledgerOrg.getMSPID());
                }

                ledgerOrg.setAdmin(admin);


                // set peerAdmin
                LedgerUser peerOrgAdmin = ledgerStore.getMember(orgConfig.getName() + "Admin", orgConfig.getName(), ledgerOrg.getMSPID(),
                        Util.findFileSk(
                                Paths.get(
                                        this.getClass().getResource("/").getPath(),
                                        ledgerProperties.getChannelPath(),
                                        ledgerProperties.getCryptoConfigPath(),
                                        ledgerOrg.getDomainName(),
                                        format(
                                                "/users/Admin@%s/msp/keystore",
                                                ledgerOrg.getDomainName()
                                        )
                                ).toFile()
                        ),
                        Paths.get(
                                this.getClass().getResource("/").getPath(),
                                ledgerProperties.getChannelPath(),
                                ledgerProperties.getCryptoConfigPath(),
                                ledgerOrg.getDomainName(),
                                format(
                                        "/users/Admin@%s/msp/signcerts/Admin@%s-cert.pem",
                                        ledgerOrg.getDomainName(),
                                        ledgerOrg.getDomainName()
                                )
                        ).toFile()
                );

                ledgerOrg.setPeerAdmin(peerOrgAdmin); //A special user that can create channels, join peers and install chaincode
            } catch (IOException | EnrollmentException | org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException e) {
                e.printStackTrace();
            }


            ledgerOrgMap.put(ledgerOrg.getName(), ledgerOrg);
        }

        ledgerOrgs.setLedgerOrgs(ledgerOrgMap);
        return ledgerOrgs;
    }




    @Bean
    @Autowired
    @ConditionalOnMissingBean(HFClient.class)
    public HFClient hFClient(LedgerOrgs ledgerOrgs) {
        HFClient hfClient = HFClient.createNewInstance();

        try {
            hfClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        } catch (CryptoException | InvalidArgumentException e) {
            e.printStackTrace();
        }



        try {
            String orgName = ledgerProperties.getCurrentOrgName();
            UserConfig userConfig = ledgerProperties.getUsers().get("admin");


            LedgerOrg ledgerOrg = ledgerOrgs.getLedgerOrgs().get(orgName);

            ledgerOrg.setCAClient(HFCAClient.createNewInstance(ledgerOrg.getCALocation(), ledgerOrg.getCAProperties()));

            File ledgerStoreFile = new File(System.getProperty("java.io.tmpdir") + "/HFC.properties");
            if (ledgerStoreFile.exists()) {
                boolean result = ledgerStoreFile.delete();
                if (result) {
                    logger.info("remove HFC.properties");
                }
            }

            final LedgerStore ledgerStore = new LedgerStore(ledgerStoreFile);

            HFCAClient ca = ledgerOrg.getCAClient();
            final String mspId = ledgerOrg.getMSPID();
            ca.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
            LedgerUser admin = ledgerStore.getMember(userConfig.getName(), orgName);
            if (!admin.isEnrolled()) {  //Preregistered admin only needs to be enrolled with Fabric caClient.
                admin.setEnrollment(ca.enroll(admin.getName(), userConfig.getSecret()));
                admin.setMspId(mspId);
            }

            ledgerOrg.setAdmin(admin);
        } catch (org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException | MalformedURLException | EnrollmentException e) {
            e.printStackTrace();
        }

        return hfClient;
    }




    @Bean
    @Autowired
    @ConditionalOnMissingBean(LedgerChannels.class)
    public LedgerChannels channels(HFClient hfClient, LedgerProperties ledgerProperties, LedgerOrgs ledgerOrgs) {
        LedgerChannels ledgerChannels = new LedgerChannels();

        List<ChannelConfig> channelList = ledgerProperties.getChannelList();


        try {
            for (ChannelConfig channelConfig : channelList) {
                Channel channel = null;
                LedgerOrg ledgerOrg = ledgerOrgs.getLedgerOrgs().get(channelConfig.getOrgName());

                channel = reconstructChannel(channelConfig.getName(), hfClient, ledgerOrg);

                ledgerChannels.getChannels().put(channelConfig.getName(), channel);

            }
        } catch (InvalidArgumentException | ProposalException | TransactionException | IOException e) {
            e.printStackTrace();
        }

        return ledgerChannels;
    }

    private Channel reconstructChannel(String name, HFClient hfClient, LedgerOrg ledgerOrg)
            throws InvalidArgumentException, TransactionException, ProposalException, IOException {

        hfClient.setUserContext(ledgerOrg.getPeerAdmin());
        Channel newChannel = hfClient.newChannel(name);

        for (String orderName : ledgerOrg.getOrdererNames()) {
            newChannel.addOrderer(hfClient.newOrderer(orderName, ledgerOrg.getOrdererLocation(orderName),
                    getOrdererProperties(orderName, ledgerOrg.getDomainName())));
        }

        for (String peerName : ledgerOrg.getPeerNames()) {
            String peerLocation = ledgerOrg.getPeerLocation(peerName);
            Peer peer = hfClient.newPeer(peerName, peerLocation, getPeerProperties(peerName, ledgerOrg.getDomainName()));

            //Query the actual peer for which channels it belongs to and check it belongs to this channel
            Set<String> channels = hfClient.queryChannels(peer);
            if (!channels.contains(name)) {
                logger.warn("Peer " + peerName + " does not appear to belong to channel " + name);
                return constructChannel(name, hfClient, ledgerOrg, ledgerOrg.getDomainName());
            }

            newChannel.addPeer(peer);
            ledgerOrg.addPeer(peer);
        }

        for (String eventHubName : ledgerOrg.getEventHubNames()) {
            EventHub eventHub = hfClient.newEventHub(eventHubName, ledgerOrg.getEventHubLocation(eventHubName),
                    getEventHubProperties(eventHubName, ledgerOrg.getDomainName()));
            newChannel.addEventHub(eventHub);
        }

        newChannel.initialize();

        return newChannel;
    }

    private Channel constructChannel(String name, HFClient hfClient, LedgerOrg ledgerOrg, String domainName)
            throws InvalidArgumentException, IOException, TransactionException, ProposalException {

        Collection<Orderer> orderers = new LinkedList<>();

        for (String orderName : ledgerOrg.getOrdererNames()) {

            Properties ordererProperties = getOrdererProperties(orderName, domainName);

            //example of setting keepAlive to avoid timeouts on inactive http2 connections.
            // Under 5 minutes would require changes to server side to accept faster ping rates.
            ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveTime", new Object[] {5L, TimeUnit.MINUTES});
            ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveTimeout", new Object[] {8L, TimeUnit.SECONDS});

            orderers.add(hfClient.newOrderer(orderName, ledgerOrg.getOrdererLocation(orderName),
                    ordererProperties));
        }

        //Just pick the first orderer in the list to create the channel.
        Orderer anOrderer = orderers.iterator().next();
        orderers.remove(anOrderer);

        ChannelConfiguration channelConfiguration = new ChannelConfiguration(new File(this.getClass().getResource("/").getPath() + "/e2e-2Orgs/channel/" + name + ".tx"));

        //Create channel that has only one signer that is this orgs peer admin. If channel creation policy needed more signature they would need to be added too.
        Channel newChannel = hfClient.newChannel(name, anOrderer, channelConfiguration, hfClient.getChannelConfigurationSignature(channelConfiguration, ledgerOrg.getPeerAdmin()));

        //client
        for (String peerName : ledgerOrg.getPeerNames()) {
            String peerLocation = ledgerOrg.getPeerLocation(peerName);

            Properties peerProperties = getPeerProperties(peerName, domainName); //test properties for peer.. if any.
            if (peerProperties == null) {
                peerProperties = new Properties();
            }
            //Example of setting specific options on grpc's NettyChannelBuilder
            peerProperties.put("grpc.NettyChannelBuilderOption.maxInboundMessageSize", 9000000);

            Peer peer = hfClient.newPeer(peerName, peerLocation, peerProperties);
            newChannel.joinPeer(peer);
            ledgerOrg.addPeer(peer);
        }

        for (Orderer orderer : orderers) { //add remaining orderers if any.
            newChannel.addOrderer(orderer);
        }

        for (String eventHubName : ledgerOrg.getEventHubNames()) {

            final Properties eventHubProperties = getEventHubProperties(eventHubName, domainName);

            eventHubProperties.put("grpc.NettyChannelBuilderOption.keepAliveTime", new Object[] {5L, TimeUnit.MINUTES});
            eventHubProperties.put("grpc.NettyChannconfigelBuilderOption.keepAliveTimeout", new Object[] {8L, TimeUnit.SECONDS});

            EventHub eventHub = hfClient.newEventHub(eventHubName, ledgerOrg.getEventHubLocation(eventHubName),
                    eventHubProperties);
            newChannel.addEventHub(eventHub);
        }

        newChannel.initialize();


        return newChannel;
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



    private Properties getOrdererProperties(String orderName, String domainName) {
        return getEndPointProperties("orderer", orderName, domainName);
    }

    private Properties getPeerProperties(String peerName, String domainName) {
        return getEndPointProperties("peer", peerName, domainName);
    }

    private Properties getEventHubProperties(String eventHubName, String domainName) {
        return getEndPointProperties("peer", eventHubName, domainName);
    }

    private Properties getEndPointProperties(final String type, final String name, final String domainName) {

//        final String domainName = getDomainName(name);

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



}
