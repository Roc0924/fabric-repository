package learning.chaincode.fabricadmin.services;


import com.google.gson.Gson;
import learning.chaincode.fabricadmin.configs.FabricConfigManager;
import learning.chaincode.fabricadmin.configs.LedgerProperties;
import learning.chaincode.fabricadmin.dtos.ChainCodeDto;
import learning.chaincode.fabricadmin.entitys.ChainCodeConfig;
import learning.chaincode.fabricadmin.entitys.LedgerOrg;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.protos.peer.Query;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.ChaincodeEndorsementPolicyParseException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.lang.String.format;
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

    private final Channel channel;

    private final FabricConfigManager fabricConfigManager;

    private final LedgerProperties ledgerProperties;


    private final ChaincodeID chaincodeID;

    @Value("${ledger.currentOrgName}")
    private String currentOrgName;
    @Value("${ledger.currentUserName}")
    private String currentUserName;
    @Value("${ledger.currentChaincodeName}")
    private String currentChaincodeName;
    @Value("${ledger.currentChaincodeVersion}")
    private String currentChaincodeVersion;


    private Collection<ProposalResponse> successful = new LinkedList<>();
    private Collection<ProposalResponse> failed = new LinkedList<>();

    @Autowired
    public FabricService(
            HFClient hfClient,
            Channel channel,
            FabricConfigManager fabricConfigManager,
            LedgerProperties ledgerProperties,
            ChaincodeID chaincodeID
    ) {
        this.hfClient = hfClient;
        this.channel = channel;
        this.fabricConfigManager = fabricConfigManager;
        this.ledgerProperties = ledgerProperties;
        this.chaincodeID = chaincodeID;
    }




    public ChainCodeDto installChainCode() {
        LedgerOrg ledgerOrg = fabricConfigManager.getIntegrationTestsLedgerOrg("peerOrg1");

        ChainCodeDto chainCodeDto = queryInstalledChainCodeByName(chaincodeID.getName());
        if (null != chainCodeDto) {
            log.warn("chain code {} is exist", chainCodeDto);
            if (!chainCodeDto.getVersion().equals(chaincodeID.getVersion())) {
                return this.upgradeChainCode(chaincodeID, ledgerOrg);
            }
            return chainCodeDto;
        }
        try {
            channel.setTransactionWaitTime(fabricConfigManager.getTransactionWaitTime());
            channel.setDeployWaitTime(ledgerProperties.getTimes().get("deployWaitTime").intValue());

            Collection<Orderer> orderers = channel.getOrderers();
            Collection<ProposalResponse> responses;
            Collection<ProposalResponse> successful = new LinkedList<>();
            Collection<ProposalResponse> failed = new LinkedList<>();

            // Install Proposal Request
            hfClient.setUserContext(ledgerOrg.getPeerAdmin());


            InstallProposalRequest installProposalRequest = hfClient.newInstallProposalRequest();
            installProposalRequest.setChaincodeID(chaincodeID);

            installProposalRequest.setChaincodeSourceLocation(new File(this.getClass().getResource("/").getPath()));


            installProposalRequest.setChaincodeVersion(ledgerProperties.getChainCodes().get(currentChaincodeName).getVersion());

            int numInstallProposal = 0;

            Set<Peer> peersFromOrg = ledgerOrg.getPeers();
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
            instantiateProposalRequest.setProposalWaitTime(ledgerProperties.getTimes().get("proposalWaitTime"));
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
            chaincodeEndorsementPolicy.fromYamlFile(new File(this.getClass().getResource("/").getPath() + "/chaincodeendorsementpolicy.yaml"));
            instantiateProposalRequest.setChaincodeEndorsementPolicy(chaincodeEndorsementPolicy);

            log.info("Sending instantiateProposalRequest to all peers with arguments: a and b set to 100 and {} respectively", "" + "1000");
            successful.clear();
            failed.clear();

            responses = channel.sendInstantiationProposal(instantiateProposalRequest, channel.getPeers());

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
            log.info("Sending instantiateTransaction to orderer with a and b set to 100 and 200 respectively");
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


    public ChainCodeDto queryInstalledChainCodeByName(String name) {
        List<ChainCodeDto> chainCodeDtos = queryInstalledChainCodes();
        for (ChainCodeDto chainCodeDto : chainCodeDtos) {
            if (chainCodeDto.getName().equals(name)) {
                return chainCodeDto;
            }
        }
        return null;
    }

    public ChainCodeDto queryInstalledChainCodeByNameAndVersion(String name, String version) {
        List<ChainCodeDto> chainCodeDtos = queryInstalledChainCodes();
        for (ChainCodeDto chainCodeDto : chainCodeDtos) {
            if (chainCodeDto.getName().equals(name)
                    && chainCodeDto.getVersion().equals(version)) {
                return chainCodeDto;
            }
        }
        return null;
    }


    public ChainCodeDto upgradeChainCode(ChaincodeID chaincodeID, LedgerOrg ledgerOrg) {
        ChainCodeDto installedChainCodeDto = queryInstalledChainCodeByName(chaincodeID.getName());
        try {


            InstallProposalRequest installProposalRequest = hfClient.newInstallProposalRequest();
            installProposalRequest.setChaincodeID(chaincodeID);
            ////For GO language and serving just a single user, chaincodeSource is mostly likely the users GOPATH
            installProposalRequest.setChaincodeSourceLocation(new File(this.getClass().getResource("/").getPath()));
            installProposalRequest.setChaincodeVersion(chaincodeID.getVersion());
            installProposalRequest.setProposalWaitTime(ledgerProperties.getTimes().get("proposalWaitTime"));


            ////////////////////////////
            // only a client from the same org as the peer can issue an install request
            int numInstallProposal = 0;

            Collection<ProposalResponse> responses;
            final Collection<ProposalResponse> successful = new LinkedList<>();
            final Collection<ProposalResponse> failed = new LinkedList<>();
            Collection<Peer> peersFromOrg = channel.getPeers();
            numInstallProposal = numInstallProposal + peersFromOrg.size();

            responses = hfClient.sendInstallProposal(installProposalRequest, peersFromOrg);

            for (ProposalResponse response : responses) {
                if (response.getStatus() == ChaincodeResponse.Status.SUCCESS) {
                    log.info("Successful install proposal response Txid: {} from peer {}", response.getTransactionID(), response.getPeer().getName());
                    successful.add(response);
                } else {
                    failed.add(response);
                }
            }

            log.info("Received {} install proposal responses. Successful+verified: {} . Failed: {}", numInstallProposal, successful.size(), failed.size());

            if (failed.size() > 0) {
                ProposalResponse first = failed.iterator().next();
                log.error("Not enough endorsers for install :" + successful.size() + ".  " + first.getMessage());
            }

            // Check that all the proposals are consistent with each other. We should have only one set
            // where all the proposals above are consistent.
            Collection<Set<ProposalResponse>> proposalConsistencySets = SDKUtils.getProposalConsistencySets(responses);
            if (proposalConsistencySets.size() != 1) {
                log.error(format("Expected only one set of consistent install proposal responses but got {}", proposalConsistencySets.size()));
            }



            UpgradeProposalRequest upgradeProposalRequest = hfClient.newUpgradeProposalRequest();

            upgradeProposalRequest.setChaincodeID(chaincodeID);
            upgradeProposalRequest.setProposalWaitTime(12000000L);
            upgradeProposalRequest.setFcn("init");
            upgradeProposalRequest.setArgs(new String[] {});
            ChaincodeEndorsementPolicy chaincodeEndorsementPolicy = new ChaincodeEndorsementPolicy();
            chaincodeEndorsementPolicy.fromYamlFile(new File(this.getClass().getResource("/").getPath() + "/chaincodeendorsementpolicy.yaml"));

            upgradeProposalRequest.setChaincodeEndorsementPolicy(chaincodeEndorsementPolicy);
            Map<String, byte[]> tmap = new HashMap<>();
            tmap.put("test", "data".getBytes());
            upgradeProposalRequest.setTransientMap(tmap);
            upgradeProposalRequest.setUserContext(ledgerOrg.getPeerAdmin());

            Collection<ProposalResponse> responses2 = channel.sendUpgradeProposal(upgradeProposalRequest);
            Collection<Set<ProposalResponse>> proposalConsistencySets2 = SDKUtils.getProposalConsistencySets(responses2);
            if (proposalConsistencySets2.size() != 1) {
                log.error(format("upgrade Expected only one set of consistent proposal responses but got %d", proposalConsistencySets2.size()));
            }

            successful.clear();
            failed.clear();
            for (ProposalResponse proposalResponse : responses2) {
                if(proposalResponse.getStatus().equals(ChaincodeResponse.Status.SUCCESS)) {
                    successful.add(proposalResponse);
                } else {
                    failed.add(proposalResponse);
                }
            }

            channel.sendTransaction(successful).get(fabricConfigManager.getTransactionWaitTime(), TimeUnit.SECONDS);

        } catch (ChaincodeEndorsementPolicyParseException | IOException | InvalidArgumentException
                | ProposalException | InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }

        if (null != successful) {
            log.info("chain code:[{}] version changed from [{}] to [{}]", installedChainCodeDto.getName(),
                    installedChainCodeDto.getVersion(), chaincodeID.getVersion());
            return queryInstalledChainCodeByNameAndVersion(chaincodeID.getName(), chaincodeID.getVersion());
        }
        return null;
    }

    public ChainCodeDto upgradeInstalledChaincode(ChaincodeID newChaincodeID) {
        LedgerOrg ledgerOrg = fabricConfigManager.getIntegrationTestsLedgerOrg("peerOrg1");
        return upgradeChainCode(newChaincodeID, ledgerOrg);
    }


    /**
     * 注入预算
     * @param planId
     * @param budgetAmount
     * @return
     */
    public Boolean injectBudget(String planId, Integer budgetAmount) {
        writeAction(new String[]{"createPlan", planId, budgetAmount.toString()});
        return null;
    }


    /**
     * 查询预算
     * @param planId
     * @return
     */
    public String queryBudget(String planId) {

        return readAction(new String[]{"queryPlan", planId});
    }


    /**
     * 调用ChainCode写操作
     * @param args
     * @return
     */
    private String writeAction(String[] args) {


        Collection<ProposalResponse> successful = new ArrayList<>();


        ChainCodeConfig chainCodeConfig = ledgerProperties.getChainCodes().get(currentChaincodeName);


        ChaincodeID chaincodeID = ChaincodeID.newBuilder().setName(chainCodeConfig.getName())
                .setPath(chainCodeConfig.getPath())
                .setVersion(currentChaincodeVersion).build();


        try {
            hfClient.setUserContext(fabricConfigManager.getOrgByName(currentOrgName).getUser(currentUserName));

            // send proposal to all peers
            TransactionProposalRequest transactionProposalRequest = hfClient.newTransactionProposalRequest();
            transactionProposalRequest.setChaincodeID(chaincodeID);
            transactionProposalRequest.setFcn("invoke");
            transactionProposalRequest.setArgs(args);
            transactionProposalRequest.setProposalWaitTime(ledgerProperties.getTimes().get("proposalWaitTime"));


            Map<String, byte[]> tm2 = new HashMap<>();
            tm2.put("HyperLedgerFabric", "TransactionProposalRequest:JavaSDK".getBytes(UTF_8));
            tm2.put("method", "TransactionProposalRequest".getBytes(UTF_8));
            tm2.put("result", ":)".getBytes(UTF_8));  /// This should be returned see chaincode.
            transactionProposalRequest.setTransientMap(tm2);


            Collection<ProposalResponse> transactionPropResp = channel.sendTransactionProposal(transactionProposalRequest, channel.getPeers());

            for (ProposalResponse response : transactionPropResp) {
                if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                    log.info("Successful  transaction proposal response Txid::{} from peer:{}", response.getTransactionID(), response.getPeer().getName());
                    successful.add(response);
                }
            }


            channel.sendTransaction(successful).get(fabricConfigManager.getTransactionWaitTime(), TimeUnit.SECONDS);

        } catch (InvalidArgumentException | ProposalException | ExecutionException | InterruptedException | TimeoutException e) {
            e.printStackTrace();
        }



        return "";
    }


    /**
     * 调用chaincode读操作
     * @param args
     * @return
     */
    public String readAction(String[] args) {

        Gson gson = new Gson();


        ChainCodeConfig chainCodeConfig = ledgerProperties.getChainCodes().get(currentChaincodeName);
        ChaincodeID chaincodeID = ChaincodeID.newBuilder().setName(chainCodeConfig.getName())
                .setPath(chainCodeConfig.getPath())
                .setVersion(currentChaincodeVersion).build();


        QueryByChaincodeRequest queryByChaincodeRequest = hfClient.newQueryProposalRequest();
        queryByChaincodeRequest.setArgs(args);
        queryByChaincodeRequest.setFcn("invoke");
        queryByChaincodeRequest.setChaincodeID(chaincodeID);

        Collection<ProposalResponse> responses;

        try {
            responses = channel.queryByChaincode(queryByChaincodeRequest);



            Collection<Set<ProposalResponse>> proposalConsistencySets = SDKUtils.getProposalConsistencySets(responses);

            if (proposalConsistencySets.size() != 1) {
                log.error(format("Expected only one set of consistent install proposal responses but got {}", proposalConsistencySets.size()));
            }

            return responses.iterator().next().getProposalResponse().getResponse().getPayload().toStringUtf8();


        } catch (ProposalException | InvalidArgumentException e) {
            e.printStackTrace();
        }


        return null;
    }

}
