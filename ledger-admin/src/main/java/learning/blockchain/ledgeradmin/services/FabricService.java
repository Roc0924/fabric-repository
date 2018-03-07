package learning.blockchain.ledgeradmin.services;


import learning.blockchain.ledgeradmin.entitys.ChainCodeDto;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.protos.peer.Query;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.ChaincodeEndorsementPolicyParseException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Create with IntelliJ IDEA
 * Author               : wangzhenpeng
 * Date                 : 2018/2/9
 * Time                 : 14:04
 * Description          : fabric 服务类
 */
@Slf4j
@Service("fabricService")
public class FabricService {
    private final HFClient hfClient;

    private final FabricConfig config;

    private final Channel channel;

    private final FabricConfigManager fabricConfigManager;


    private final ChaincodeID chaincodeID;


    private Collection<ProposalResponse> successful = new LinkedList<>();
    private Collection<ProposalResponse> failed = new LinkedList<>();

    @Autowired
    public FabricService(HFClient hfClient, FabricConfig config, Channel channel, FabricConfigManager fabricConfigManager, ChaincodeID chaincodeID) {
        this.hfClient = hfClient;
        this.config = config;
        this.channel = channel;
        this.fabricConfigManager = fabricConfigManager;
        this.chaincodeID = chaincodeID;
    }




    public ChainCodeDto installChainCode() {
                int delta = 0;




        SampleOrg sampleOrg = fabricConfigManager.getIntegrationTestsSampleOrg("peerOrg1");

        ChainCodeDto chainCodeDto = queryInstalledChainCodeByName(chaincodeID.getName());
        if (null != chainCodeDto) {
            log.warn("chain code {} is exist", chainCodeDto);
            if (!chainCodeDto.getVersion().equals(chaincodeID.getVersion())) {
                return this.upgradeChainCode(chaincodeID, sampleOrg);
            }
            return chainCodeDto;
        }
        try {

            final String channelName = channel.getName();
            boolean isFooChain = config.getFooChannelName().equals(channelName);
            channel.setTransactionWaitTime(fabricConfigManager.getTransactionWaitTime());
            channel.setDeployWaitTime(Integer.parseInt(config.getDeployWaitTime()));

            Collection<Orderer> orderers = channel.getOrderers();
            Collection<ProposalResponse> responses;
            Collection<ProposalResponse> successful = new LinkedList<>();
            Collection<ProposalResponse> failed = new LinkedList<>();

            // Install Proposal Request
            hfClient.setUserContext(sampleOrg.getPeerAdmin());


            InstallProposalRequest installProposalRequest = hfClient.newInstallProposalRequest();
            installProposalRequest.setChaincodeID(chaincodeID);

            if (isFooChain) {
                // on foo chain install from directory.
                installProposalRequest.setChaincodeSourceLocation(new File(config.getFixturesPath()));
            } else {
                log.error("chain code error");
                throw new Exception("chain code error");
            }

            installProposalRequest.setChaincodeVersion(config.getChainCodeVersion());

            int numInstallProposal = 0;

            Set<Peer> peersFromOrg = sampleOrg.getPeers();
            numInstallProposal = numInstallProposal + peersFromOrg.size();
            responses = hfClient.sendInstallProposal(installProposalRequest, peersFromOrg);

            for (ProposalResponse response : responses) {
                if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                    log.info("Successful install proposal response Txid: {} from peer {}", response.getTransactionID(), response.getPeer().getName());
                    successful.add(response);
                } else {
                    failed.add(response);
                }
            }


            SDKUtils.getProposalConsistencySets(responses);

            log.info("Received %d install proposal responses. Successful+verified: {} . Failed: {}", numInstallProposal, successful.size(), failed.size());

            if (failed.size() > 0) {
                ProposalResponse first = failed.iterator().next();
                log.info("Not enough endorsers for install :" + successful.size() + ".  " + first.getMessage());
            }

            // Instantiate chaincode.
            InstantiateProposalRequest instantiateProposalRequest = hfClient.newInstantiationProposalRequest();
            instantiateProposalRequest.setProposalWaitTime(Integer.parseInt(config.getProposalWaitTime()));
            instantiateProposalRequest.setChaincodeID(chaincodeID);
            instantiateProposalRequest.setFcn("init");
            instantiateProposalRequest.setArgs(new String[] {"a", "500", "b", "1000"});
            Map<String, byte[]> tm = new HashMap<>();
            tm.put("HyperLedgerFabric", "InstantiateProposalRequest:JavaSDK".getBytes(UTF_8));
            tm.put("method", "InstantiateProposalRequest".getBytes(UTF_8));
            instantiateProposalRequest.setTransientMap(tm);

            /*
              policy OR(Org1MSP.member, Org2MSP.member) meaning 1 signature from someone in either Org1 or Org2
              See README.md Chaincode endorsement policies section for more details.
            */
            ChaincodeEndorsementPolicy chaincodeEndorsementPolicy = new ChaincodeEndorsementPolicy();
            chaincodeEndorsementPolicy.fromYamlFile(new File(config.getFixturesPath() + "/chaincodeendorsementpolicy.yaml"));
            instantiateProposalRequest.setChaincodeEndorsementPolicy(chaincodeEndorsementPolicy);

            log.info("Sending instantiateProposalRequest to all peers with arguments: a and b set to 100 and {} respectively", "" + "1000");
            successful.clear();
            failed.clear();

            if (isFooChain) {  //Send responses both ways with specifying peers and by using those on the channel.
                responses = channel.sendInstantiationProposal(instantiateProposalRequest, channel.getPeers());
            } else {
                responses = channel.sendInstantiationProposal(instantiateProposalRequest);
            }
            for (ProposalResponse response : responses) {
                if (response.isVerified() && response.getStatus() == ProposalResponse.Status.SUCCESS) {
                    successful.add(response);
                    log.info("Succesful instantiate proposal response Txid: {} from peer {}", response.getTransactionID(), response.getPeer().getName());
                } else {
                    failed.add(response);
                }
            }
            log.info("Received %d instantiate proposal responses. Successful+verified: {} . Failed: {}", responses.size(), successful.size(), failed.size());
            if (failed.size() > 0) {
                ProposalResponse first = failed.iterator().next();
                log.error("Not enough endorsers for instantiate :" + successful.size() + "endorser failed with " + first.getMessage() + ". Was verified:" + first.isVerified());
            }

            // Send instantiate transaction to orderer
            log.info("Sending instantiateTransaction to orderer with a and b set to 100 and {} respectively", "" + (200 + delta));
            channel.sendTransaction(successful, orderers);

        } catch (Exception e) {
            log.info("Caught an exception running channel {}", channel.getName());
            e.printStackTrace();
            log.error("Test failed with error : " + e.getMessage());
        }
        return queryInstalledChainCodeByName(chaincodeID.getName());
    }

    public List<ChainCodeDto> queryInstalledChainCodes() {
        List<ChainCodeDto> result = new ArrayList<>();
        Collection<Peer> peers = channel.getPeers();
        Peer peer = null;
        List<Query.ChaincodeInfo> chaincodeInfos = null;
        try {
            if (peers.size() > 0) {
                peer = (Peer)peers.toArray()[0];
                chaincodeInfos = hfClient.queryInstalledChaincodes(peer);
            }
        } catch (ProposalException | InvalidArgumentException e) {
            e.printStackTrace();
        }
        assert chaincodeInfos != null;
        for (Query.ChaincodeInfo chaincodeInfo : chaincodeInfos) {
            ChainCodeDto chainCodeDto = new ChainCodeDto();
            chainCodeDto.setName(chaincodeInfo.getName());
            chainCodeDto.setVersion(chaincodeInfo.getVersion());
            chainCodeDto.setPath(chaincodeInfo.getPath());
            result.add(chainCodeDto);
        }
        return result;
    }


    public ChainCodeDto queryInstalledChainCodeByName(String ChainCodeName) {
        List<ChainCodeDto> chainCodeDtos = queryInstalledChainCodes();
        for (ChainCodeDto chainCodeDto : chainCodeDtos) {
            if (chainCodeDto.getName().equals(ChainCodeName)) {
                return chainCodeDto;
            }
        }
        return null;
    }


    public ChainCodeDto upgradeChainCode(ChaincodeID chaincodeID, SampleOrg sampleOrg) {
        ChainCodeDto installedChainCodeDto = queryInstalledChainCodeByName(chaincodeID.getName());
        try {
            UpgradeProposalRequest upgradeProposalRequest = hfClient.newUpgradeProposalRequest();

            upgradeProposalRequest.setChaincodeID(chaincodeID);
            upgradeProposalRequest.setProposalWaitTime(Long.parseLong(fabricConfigManager.getFabricConfig().getProposalWaitTime()));
            upgradeProposalRequest.setFcn("init");
            upgradeProposalRequest.setArgs(new String[] {"c", "500"});
            ChaincodeEndorsementPolicy chaincodeEndorsementPolicy = new ChaincodeEndorsementPolicy();
            chaincodeEndorsementPolicy.fromYamlFile(new File(config.getFixturesPath() + "/chaincodeendorsementpolicy.yaml"));

            upgradeProposalRequest.setChaincodeEndorsementPolicy(chaincodeEndorsementPolicy);
            Map<String, byte[]> tmap = new HashMap<>();
            tmap.put("test", "data".getBytes());
            upgradeProposalRequest.setTransientMap(tmap);
            upgradeProposalRequest.setUserContext(sampleOrg.getPeerAdmin());

            Collection<ProposalResponse> responses = channel.sendUpgradeProposal(upgradeProposalRequest);

            for (ProposalResponse proposalResponse : responses) {
                if(proposalResponse.getStatus().equals(ChaincodeResponse.Status.SUCCESS)) {
                    successful.add(proposalResponse);
                } else {
                    failed.add(proposalResponse);
                }
            }

            Object obj = channel.sendTransaction(successful).get(fabricConfigManager.getTransactionWaitTime(), TimeUnit.SECONDS);
            log.info("type: {}", obj.getClass());

        } catch (ChaincodeEndorsementPolicyParseException | IOException | InvalidArgumentException
                | ProposalException | InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }

        if (null != successful) {
            log.info("chain code:[{}] version changed from [{}] to [{}]", installedChainCodeDto.getName(),
                    installedChainCodeDto.getVersion(), chaincodeID.getVersion());
            return queryInstalledChainCodeByName(chaincodeID.getName());
        }
        return null;
    }

    public ChainCodeDto upgradeInstalledChaincode(ChaincodeID newChaincodeID) {
        SampleOrg sampleOrg = fabricConfigManager.getIntegrationTestsSampleOrg("peerOrg1");
        return upgradeChainCode(newChaincodeID, sampleOrg);
    }

}
