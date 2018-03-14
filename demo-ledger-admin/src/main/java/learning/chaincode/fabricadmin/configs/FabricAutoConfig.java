package learning.chaincode.fabricadmin.configs;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import learning.chaincode.fabricadmin.entitys.ChainCodeConfig;
import learning.chaincode.fabricadmin.entitys.LedgerOrg;
import learning.chaincode.fabricadmin.entitys.LedgerStore;
import learning.chaincode.fabricadmin.entitys.LedgerUser;
import learning.chaincode.fabricadmin.utils.Util;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.hyperledger.fabric.sdk.helper.Utils;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

/**
 * Create with IntelliJ IDEA
 * Author               : wangzhenpeng
 * Date                 : 2018/2/13
 * Time                 : 14:18
 * Description          : 自动注入配置
 */
@Slf4j
@Configuration
public class FabricAutoConfig {


    final LedgerProperties ledgerProperties;

    @Autowired
    public FabricAutoConfig(LedgerProperties ledgerProperties) {
        this.ledgerProperties = ledgerProperties;
    }

    /**
     * 自动注入合约ID
     * @return
     */
    @Bean(name = "chainCodeID")
    ChaincodeID chaincodeID() {

        ChainCodeConfig chainCodeConfig = ledgerProperties.getChainCodes().get("rebate_directly_cc_json");

        return ChaincodeID.newBuilder().setName(chainCodeConfig.getName())
                .setVersion(chainCodeConfig.getVersion())
                .setPath(chainCodeConfig.getPath()).build();
    }





    /**
     * 自动注入hyperledger fabric 客户端
     * @param fabricConfigManager
     * @return
     * @throws Exception
     */
    @Autowired
    @Bean(name = "hfClient")
    public HFClient hfClient(FabricConfigManager fabricConfigManager) throws Exception {
        HFClient client = HFClient.createNewInstance();

        try {
            client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        } catch (CryptoException | InvalidArgumentException e) {
            e.printStackTrace();
        }


        Collection<LedgerOrg> ledgerOrgs = fabricConfigManager.getIntegrationLedgerOrgs();


        for (LedgerOrg ledgerOrg : ledgerOrgs) {
            ledgerOrg.setCAClient(HFCAClient.createNewInstance(ledgerOrg.getCALocation(), ledgerOrg.getCAProperties()));
        }
        File ledgerStoreFile = new File(System.getProperty("java.io.tmpdir") + "/HFC.properties");
        if (ledgerStoreFile.exists()) {
            boolean result = ledgerStoreFile.delete();
            if (result) {
                log.info("remove HFC.properties");
            }
        }


        final LedgerStore ledgerStore = new LedgerStore(ledgerStoreFile);


        for (LedgerOrg ledgerOrg : ledgerOrgs) {

            HFCAClient ca = ledgerOrg.getCAClient();
            final String orgName = ledgerOrg.getName();
            final String mspid = ledgerOrg.getMSPID();
            ca.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
            LedgerUser admin = ledgerStore.getMember(ledgerProperties.getUsers().get("admin").getName(), orgName);
            if (!admin.isEnrolled()) {  //Preregistered admin only needs to be enrolled with Fabric caClient.
                admin.setEnrollment(ca.enroll(admin.getName(), "adminpw"));
                admin.setMspId(mspid);
            }

            ledgerOrg.setAdmin(admin); // The admin of this org --
            LedgerUser user = ledgerStore.getMember(ledgerProperties.getUsers().get("user1").getName(), ledgerOrg.getName());


            RegistrationRequest rr = new RegistrationRequest(user.getName(), "org1.department1");
            rr.setSecret(ledgerProperties.getUsers().get("user1").getSecret());
            String enrollmentSecret = null;
            try {
                enrollmentSecret = ca.register(rr, admin);
            } catch (Exception e) {
                Map requestStrObj = null;
                Map responseStrObj = null;
                String message = e.getMessage();

                if(message.contains("request body") && message.contains("Response")&& message.contains("status code: 500")) {
                    requestStrObj = JSONObject.parseObject(message.substring(message.indexOf("request body") + 13,
                            message.indexOf("with status code")), Map.class);
                    responseStrObj = JSONObject.parseObject(message.substring(message.indexOf("Response") + 9,
                            message.length()), Map.class);
                }
                assert responseStrObj != null;
                if (((JSONArray)responseStrObj.get("errors")).getJSONObject(0).get("code").toString().equals("0")) {
                    enrollmentSecret = requestStrObj.get("id").toString();
                    log.warn("{} is already registered", requestStrObj.get("id").toString());
                } else {
                    throw e;
                }
            }
            user.setEnrollmentSecret(enrollmentSecret);

            if (!user.isEnrolled()) {
                Enrollment enrollment = null;
                enrollment = ca.enroll(user.getName(), user.getEnrollmentSecret());
                user.setEnrollment(enrollment);
                user.setMspId(mspid);
            }
            ledgerOrg.addUser(user);

            final String ledgerOrgName = ledgerOrg.getName();
            final String ledgerOrgDomainName = ledgerOrg.getDomainName();

            LedgerUser peerOrgAdmin = ledgerStore.getMember(ledgerOrgName + "Admin", ledgerOrgName, ledgerOrg.getMSPID(),
                    Util.findFileSk(Paths.get(this.getClass().getResource("/").getPath(), ledgerProperties.getChannelPath(),
                            ledgerProperties.getCryptoConfigPath(),
                            ledgerOrgDomainName, format("/users/Admin@%s/msp/keystore", ledgerOrgDomainName)).toFile()),
                    Paths.get(this.getClass().getResource("/").getPath(), ledgerProperties.getChannelPath(),
                            ledgerProperties.getCryptoConfigPath(), ledgerOrgDomainName,
                            format("/users/Admin@%s/msp/signcerts/Admin@%s-cert.pem", ledgerOrgDomainName, ledgerOrgDomainName)).toFile());

            ledgerOrg.setPeerAdmin(peerOrgAdmin); //A special user that can create channels, join peers and install chaincode

        }

        return client;
    }




    /**
     * 自动注入渠道
     * @param fabricConfigManager
     * @param client
     * @return
     * @throws InvalidArgumentException
     * @throws TransactionException
     * @throws ProposalException
     * @throws IOException
     */
    @Bean(name = "channel")
    @Autowired
    public Channel channel(FabricConfigManager fabricConfigManager, HFClient client) {

        LedgerOrg ledgerOrg = fabricConfigManager.getIntegrationTestsLedgerOrg("peerOrg1");
        String name = "foo";

        Channel newChannel = null;
        try {
            newChannel = reconstructChannel(fabricConfigManager, name, client, ledgerOrg);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return newChannel;
    }


    private Channel reconstructChannel(FabricConfigManager fabricConfigManager, String name, HFClient client, LedgerOrg ledgerOrg) throws Exception {

        client.setUserContext(ledgerOrg.getPeerAdmin());
        Channel newChannel = client.newChannel(name);

        for (String orderName : ledgerOrg.getOrdererNames()) {
            newChannel.addOrderer(client.newOrderer(orderName, ledgerOrg.getOrdererLocation(orderName),
                    fabricConfigManager.getOrdererProperties(orderName)));
        }

        for (String peerName : ledgerOrg.getPeerNames()) {
            String peerLocation = ledgerOrg.getPeerLocation(peerName);
            Peer peer = client.newPeer(peerName, peerLocation, fabricConfigManager.getPeerProperties(peerName));

            //Query the actual peer for which channels it belongs to and check it belongs to this channel
            Set<String> channels = client.queryChannels(peer);
            if (!channels.contains(name)) {
                log.warn("Peer {} does not appear to belong to channel {}", peerName, name);
                return constructChannel(fabricConfigManager, client, ledgerOrg, name);
            }

            newChannel.addPeer(peer);
            ledgerOrg.addPeer(peer);
        }

        for (String eventHubName : ledgerOrg.getEventHubNames()) {
            EventHub eventHub = client.newEventHub(eventHubName, ledgerOrg.getEventHubLocation(eventHubName),
                    fabricConfigManager.getEventHubProperties(eventHubName));
            newChannel.addEventHub(eventHub);
        }

        newChannel.initialize();

        return newChannel;
    }

    private Channel constructChannel(FabricConfigManager fabricConfigManager, HFClient client,
                                     LedgerOrg ledgerOrg, String name) throws Exception {


        client.setUserContext(ledgerOrg.getPeerAdmin());

        Collection<Orderer> orderers = new LinkedList<>();

        for (String orderName : ledgerOrg.getOrdererNames()) {

            Properties ordererProperties = fabricConfigManager.getOrdererProperties(orderName);

            //example of setting keepAlive to avoid timeouts on inactive http2 connections.
            // Under 5 minutes would require changes to server side to accept faster ping rates.
            ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveTime", new Object[] {5L, TimeUnit.MINUTES});
            ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveTimeout", new Object[] {8L, TimeUnit.SECONDS});

            orderers.add(client.newOrderer(orderName, ledgerOrg.getOrdererLocation(orderName),
                    ordererProperties));
        }

        //Just pick the first orderer in the list to create the channel.

        Orderer anOrderer = orderers.iterator().next();
        orderers.remove(anOrderer);

        ChannelConfiguration channelConfiguration = new ChannelConfiguration(new File(this.getClass().getResource("/").getPath() + "/e2e-2Orgs/channel/" + name + ".tx"));

        //Create channel that has only one signer that is this orgs peer admin. If channel creation policy needed more signature they would need to be added too.
        Channel newChannel = client.newChannel(name, anOrderer, channelConfiguration, client.getChannelConfigurationSignature(channelConfiguration, ledgerOrg.getPeerAdmin()));

        for (String peerName : ledgerOrg.getPeerNames()) {
            String peerLocation = ledgerOrg.getPeerLocation(peerName);

            Properties peerProperties = fabricConfigManager.getPeerProperties(peerName); //test properties for peer.. if any.
            if (peerProperties == null) {
                peerProperties = new Properties();
            }
            //Example of setting specific options on grpc's NettyChannelBuilder
            peerProperties.put("grpc.NettyChannelBuilderOption.maxInboundMessageSize", 9000000);

            Peer peer = client.newPeer(peerName, peerLocation, peerProperties);
            newChannel.joinPeer(peer);
            ledgerOrg.addPeer(peer);
        }

        for (Orderer orderer : orderers) { //add remaining orderers if any.
            newChannel.addOrderer(orderer);
        }

        for (String eventHubName : ledgerOrg.getEventHubNames()) {

            final Properties eventHubProperties = fabricConfigManager.getEventHubProperties(eventHubName);

            eventHubProperties.put("grpc.NettyChannelBuilderOption.keepAliveTime", new Object[] {5L, TimeUnit.MINUTES});
            eventHubProperties.put("grpc.NettyChannconfigelBuilderOption.keepAliveTimeout", new Object[] {8L, TimeUnit.SECONDS});

            EventHub eventHub = client.newEventHub(eventHubName, ledgerOrg.getEventHubLocation(eventHubName),
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

}
