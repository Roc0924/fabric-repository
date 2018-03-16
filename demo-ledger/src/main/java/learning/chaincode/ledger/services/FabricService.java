package learning.chaincode.ledger.services;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import learning.chaincode.ledger.configs.FabricConfigManager;
import learning.chaincode.ledger.configs.LedgerProperties;
import learning.chaincode.ledger.dtos.ChainCodeDto;
import learning.chaincode.ledger.entitys.ChainCodeConfig;
import learning.chaincode.ledger.entitys.LedgerOrg;
import learning.chaincode.ledger.entitys.RebateAccount;
import learning.chaincode.ledger.utils.Util;
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
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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



    /**
     * 创建账户
     * @param rebateAccount
     * @return
     */
    public Boolean createAccount(RebateAccount rebateAccount) {
        Gson gson = new Gson();
        String rebateAccountStr = gson.toJson(rebateAccount);
        writeAction(new String[]{"createAccount", rebateAccountStr});
        return null;
    }

    /**
     * 查询账户
     * @param accountId
     * @return
     */
    public RebateAccount queryAccount(String accountId) {
        Gson gson = new Gson();
        String rebateAccountStr = readAction(new String[]{"queryAccount", accountId});
        RebateAccount rebateAccount = null;
        if (null != rebateAccountStr) {
            rebateAccount = gson.fromJson(rebateAccountStr, RebateAccount.class);
        }
        log.info(rebateAccount.getDetails());
        return rebateAccount;
    }


    /**
     * 删除账户
     * @param accountId
     * @return
     */
    public RebateAccount deleteAccount(String accountId) {
        Gson gson = new Gson();
        String rebateAccountStr = writeAction(new String[]{"deleteAccount", accountId});

        RebateAccount rebateAccount = null;
        if (null != rebateAccountStr) {
            rebateAccount = gson.fromJson(rebateAccountStr, RebateAccount.class);
        }

        return rebateAccount;
    }


    /**
     * 查询账户历史
     * @param accountId
     * @return
     */
    public Map<String, Object> queryAccountHistory(String accountId) {
        Map<String, Object> result = new HashMap<>();

        String historyStr = readAction(new String[]{"queryHistory", accountId});


        List<Map<String,Object>> resultList = new Gson().fromJson(historyStr, new TypeToken<ArrayList<HashMap<String,Object>>>(){}.getType());


        formatHistoryList(resultList);

        result.put("accountId", accountId);
        result.put("history", resultList);

        return result;
    }


    /**
     * 格式化返回历史列表
     * @param resultList
     */
    private void formatHistoryList(List<Map<String, Object>> resultList) {



        for (Map<String, Object> result : resultList) {
            Set<String> set = new TreeSet<>();
            set.addAll(result.keySet());
            for (String key : set) {
                if (key.contains("_")) {
                    Object data = result.get(key);
                    result.remove(key);
                    result.put(Util.lineToHump(key), data);
                }
                if (key.equals("value")) {
                    String value = result.get(key).toString();
                    result.remove(key);

                    new Gson().fromJson(Util.Base64Decoder(value), new TypeToken<HashMap<String, Object>>(){}.getType());
                    result.put(key, new Gson().fromJson(Util.Base64Decoder(value), new TypeToken<HashMap<String, Object>>(){}.getType()));
                }
            }
        }

    }




    public void addAmountFromBudget(String orderId, String planId, String accountId, Integer delta, String kid) {

        String details = generateDetails(orderId, planId, accountId, kid,"direct", delta);

        String rebateAccountStr = writeAction(new String[]{"addAmountFromBudget", planId, accountId, delta.toString(), details});
        log.info(rebateAccountStr);

    }

    public void rollBackAmountToBudget(String orderId, String planId, String accountId, Integer delta, String kid) {

        String details = generateDetails(orderId, accountId, accountId, kid, "direct", delta);
        String rebateAccountStr = writeAction(new String[]{"rollBackAmountToBudget", accountId, planId, delta.toString(), details});
        log.info(rebateAccountStr);

    }

    public void addExpectAmountFromBudget(String orderId, String planId, String accountId, Integer delta, String kid) {

        String details = generateDetails(orderId, planId, accountId, kid, "expect", delta);
        String rebateAccountStr = writeAction(new String[]{"addExpectAmountFromBudget", planId, accountId, delta.toString(), details});
        log.info(rebateAccountStr);

    }

    public void rollBackExpectAmountToBudget(String orderId, String planId, String accountId, Integer delta, String kid) {

        String details = generateDetails(orderId, planId, accountId, kid, "expect", delta);
        String rebateAccountStr = writeAction(new String[]{"rollBackExpectAmountToBudget", accountId, planId, delta.toString(), details});
        log.info(rebateAccountStr);


    }



    public void collectExcept(String orderId, String accountId, Integer delta, String kid) {

        String details = generateDetails(orderId, accountId, accountId, kid, "collect", delta);
        String rebateAccountStr = writeAction(new String[]{"collectExcept", accountId, delta.toString(), details});
        log.info(rebateAccountStr);


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

            return transactionPropResp.iterator().next().getProposalResponse().getResponse().getPayload().toStringUtf8();

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

    private String generateDetails(String orderId, String source, String destination, String kid, String type, Integer delta) {


        return new StringBuffer().append("{\"type\":\"").append(type)
                .append("\",\"orderId\":\"").append(orderId)
                .append("\",\"source\":\"").append(source)
                .append("\",\"destination\":\"").append(destination)
                .append("\",\"delta\":").append(delta)
                .append(",\"kid\":\"").append(null != kid ? kid : "")
                .append("\"}").toString();
    }


}
