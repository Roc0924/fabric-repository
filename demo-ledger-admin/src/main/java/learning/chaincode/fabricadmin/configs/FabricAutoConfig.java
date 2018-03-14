package learning.chaincode.fabricadmin.configs;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import learning.chaincode.fabricadmin.dtos.ChainCodeConfig;
import learning.chaincode.fabricadmin.dtos.SampleOrg;
import learning.chaincode.fabricadmin.dtos.SampleStore;
import learning.chaincode.fabricadmin.dtos.SampleUser;
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


        Collection<SampleOrg> sampleOrgs = fabricConfigManager.getIntegrationSampleOrgs();


        for (SampleOrg sampleOrg : sampleOrgs) {
            sampleOrg.setCAClient(HFCAClient.createNewInstance(sampleOrg.getCALocation(), sampleOrg.getCAProperties()));
        }
        File sampleStoreFile = new File(System.getProperty("java.io.tmpdir") + "/HFC.properties");
        if (sampleStoreFile.exists()) {
            boolean result = sampleStoreFile.delete();
            if (result) {
                log.info("remove HFC.properties");
            }
        }


        final SampleStore sampleStore = new SampleStore(sampleStoreFile);


        for (SampleOrg sampleOrg : sampleOrgs) {

            HFCAClient ca = sampleOrg.getCAClient();
            final String orgName = sampleOrg.getName();
            final String mspid = sampleOrg.getMSPID();
            ca.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
            SampleUser admin = sampleStore.getMember(ledgerProperties.getUsers().get("admin").getName(), orgName);
            if (!admin.isEnrolled()) {  //Preregistered admin only needs to be enrolled with Fabric caClient.
                admin.setEnrollment(ca.enroll(admin.getName(), "adminpw"));
                admin.setMspId(mspid);
            }

            sampleOrg.setAdmin(admin); // The admin of this org --
            SampleUser user = sampleStore.getMember(ledgerProperties.getUsers().get("user1").getName(), sampleOrg.getName());


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
            sampleOrg.addUser(user);

            final String sampleOrgName = sampleOrg.getName();
            final String sampleOrgDomainName = sampleOrg.getDomainName();

            SampleUser peerOrgAdmin = sampleStore.getMember(sampleOrgName + "Admin", sampleOrgName, sampleOrg.getMSPID(),
                    Util.findFileSk(Paths.get(this.getClass().getResource("/").getPath(), ledgerProperties.getChannelPath(),
                            ledgerProperties.getCryptoConfigPath(),
                            sampleOrgDomainName, format("/users/Admin@%s/msp/keystore", sampleOrgDomainName)).toFile()),
                    Paths.get(this.getClass().getResource("/").getPath(), ledgerProperties.getChannelPath(),
                            ledgerProperties.getCryptoConfigPath(), sampleOrgDomainName,
                            format("/users/Admin@%s/msp/signcerts/Admin@%s-cert.pem", sampleOrgDomainName, sampleOrgDomainName)).toFile());

            sampleOrg.setPeerAdmin(peerOrgAdmin); //A special user that can create channels, join peers and install chaincode

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

        SampleOrg sampleOrg = fabricConfigManager.getIntegrationTestsSampleOrg("peerOrg1");
        String name = "foo";

        Channel newChannel = null;
        try {
            newChannel = reconstructChannel(fabricConfigManager, name, client, sampleOrg);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return newChannel;
    }


    private Channel reconstructChannel(FabricConfigManager fabricConfigManager, String name, HFClient client, SampleOrg sampleOrg) throws Exception {

        client.setUserContext(sampleOrg.getPeerAdmin());
        Channel newChannel = client.newChannel(name);

        for (String orderName : sampleOrg.getOrdererNames()) {
            newChannel.addOrderer(client.newOrderer(orderName, sampleOrg.getOrdererLocation(orderName),
                    fabricConfigManager.getOrdererProperties(orderName)));
        }

        for (String peerName : sampleOrg.getPeerNames()) {
            String peerLocation = sampleOrg.getPeerLocation(peerName);
            Peer peer = client.newPeer(peerName, peerLocation, fabricConfigManager.getPeerProperties(peerName));

            //Query the actual peer for which channels it belongs to and check it belongs to this channel
            Set<String> channels = client.queryChannels(peer);
            if (!channels.contains(name)) {
                log.warn("Peer {} does not appear to belong to channel {}", peerName, name);
                return constructChannel(fabricConfigManager, client, sampleOrg, name);
            }

            newChannel.addPeer(peer);
            sampleOrg.addPeer(peer);
        }

        for (String eventHubName : sampleOrg.getEventHubNames()) {
            EventHub eventHub = client.newEventHub(eventHubName, sampleOrg.getEventHubLocation(eventHubName),
                    fabricConfigManager.getEventHubProperties(eventHubName));
            newChannel.addEventHub(eventHub);
        }

        newChannel.initialize();

        return newChannel;
    }

    private Channel constructChannel(FabricConfigManager fabricConfigManager, HFClient client,
                                     SampleOrg sampleOrg, String name) throws Exception {


        client.setUserContext(sampleOrg.getPeerAdmin());

        Collection<Orderer> orderers = new LinkedList<>();

        for (String orderName : sampleOrg.getOrdererNames()) {

            Properties ordererProperties = fabricConfigManager.getOrdererProperties(orderName);

            //example of setting keepAlive to avoid timeouts on inactive http2 connections.
            // Under 5 minutes would require changes to server side to accept faster ping rates.
            ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveTime", new Object[] {5L, TimeUnit.MINUTES});
            ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveTimeout", new Object[] {8L, TimeUnit.SECONDS});

            orderers.add(client.newOrderer(orderName, sampleOrg.getOrdererLocation(orderName),
                    ordererProperties));
        }

        //Just pick the first orderer in the list to create the channel.

        Orderer anOrderer = orderers.iterator().next();
        orderers.remove(anOrderer);

        ChannelConfiguration channelConfiguration = new ChannelConfiguration(new File(this.getClass().getResource("/").getPath() + "/e2e-2Orgs/channel/" + name + ".tx"));

        //Create channel that has only one signer that is this orgs peer admin. If channel creation policy needed more signature they would need to be added too.
        Channel newChannel = client.newChannel(name, anOrderer, channelConfiguration, client.getChannelConfigurationSignature(channelConfiguration, sampleOrg.getPeerAdmin()));

        for (String peerName : sampleOrg.getPeerNames()) {
            String peerLocation = sampleOrg.getPeerLocation(peerName);

            Properties peerProperties = fabricConfigManager.getPeerProperties(peerName); //test properties for peer.. if any.
            if (peerProperties == null) {
                peerProperties = new Properties();
            }
            //Example of setting specific options on grpc's NettyChannelBuilder
            peerProperties.put("grpc.NettyChannelBuilderOption.maxInboundMessageSize", 9000000);

            Peer peer = client.newPeer(peerName, peerLocation, peerProperties);
            newChannel.joinPeer(peer);
            sampleOrg.addPeer(peer);
        }

        for (Orderer orderer : orderers) { //add remaining orderers if any.
            newChannel.addOrderer(orderer);
        }

        for (String eventHubName : sampleOrg.getEventHubNames()) {

            final Properties eventHubProperties = fabricConfigManager.getEventHubProperties(eventHubName);

            eventHubProperties.put("grpc.NettyChannelBuilderOption.keepAliveTime", new Object[] {5L, TimeUnit.MINUTES});
            eventHubProperties.put("grpc.NettyChannconfigelBuilderOption.keepAliveTimeout", new Object[] {8L, TimeUnit.SECONDS});

            EventHub eventHub = client.newEventHub(eventHubName, sampleOrg.getEventHubLocation(eventHubName),
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
