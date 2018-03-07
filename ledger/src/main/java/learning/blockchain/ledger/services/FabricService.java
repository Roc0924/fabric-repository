package learning.blockchain.ledger.services;

import com.google.protobuf.ByteString;
import learning.blockchain.ledger.configs.FabricConfig;
import learning.blockchain.ledger.configs.FabricConfigManager;
import learning.blockchain.ledger.entitys.LedgerOrg;
import learning.blockchain.ledger.entitys.Record;
import learning.blockchain.ledger.entitys.TransactionRsp;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.CompletionException;
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

    @Autowired
    public FabricService(HFClient hfClient, FabricConfig config, Channel channel, FabricConfigManager fabricConfigManager, ChaincodeID chaincodeID) {
        this.hfClient = hfClient;
        this.config = config;
        this.channel = channel;
        this.fabricConfigManager = fabricConfigManager;
        this.chaincodeID = chaincodeID;
    }


    public LedgerOrg getOrg(String orgName) {
        return fabricConfigManager.getIntegrationTestsLedgerOrg(orgName);
    }



    public TransactionRsp transaction(Channel channel, HFClient client, LedgerOrg sampleOrg,
                                      ChaincodeID chaincodeID, String source, String destination, String delta) {
        Collection<ProposalResponse> successful = new ArrayList<>();
        String transactionID = null;
        try {
            client.setUserContext(sampleOrg.getUser(config.getUser1Name()));

            // Send transaction proposal to all peers
            TransactionProposalRequest transactionProposalRequest = client.newTransactionProposalRequest();
            transactionProposalRequest.setChaincodeID(chaincodeID);
            transactionProposalRequest.setFcn("invoke");
            transactionProposalRequest.setProposalWaitTime(Integer.parseInt(config.getProposalWaitTime()));
            transactionProposalRequest.setArgs(new String[] {"move", source, destination, delta});

            Map<String, byte[]> tm2 = new HashMap<>();
            tm2.put("HyperLedgerFabric", "TransactionProposalRequest:JavaSDK".getBytes(UTF_8));
            tm2.put("method", "TransactionProposalRequest".getBytes(UTF_8));
            tm2.put("result", ":)".getBytes(UTF_8));  /// This should be returned see chaincode.
            transactionProposalRequest.setTransientMap(tm2);

            log.info("sending transactionProposal to all peers with arguments: move(a,b,100)");

            Collection<ProposalResponse> transactionPropResp = channel.sendTransactionProposal(transactionProposalRequest, channel.getPeers());

            for (ProposalResponse response : transactionPropResp) {
                if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                    log.info("Successful  transaction proposal response Txid::{} from peer:{}", response.getTransactionID(), response.getPeer().getName());
                    successful.add(response);
                }
            }





            BlockEvent.TransactionEvent transactionEvent = channel.sendTransaction(successful).get(fabricConfigManager.getTransactionWaitTime(), TimeUnit.SECONDS);
            EventHub eventHub = transactionEvent.getEventHub();
            log.info("eventHub:[{}]", eventHub);
            String channelId = transactionEvent.getChannelId();
            log.info("channelId:[{}]", channelId);
            Long epoch = transactionEvent.getEpoch();
            log.info("epoch:[{}]", epoch);
            Date timestamp = transactionEvent.getTimestamp();
            log.info("timestamp:[{}]", timestamp);
            Integer transactionActionInfoCount = transactionEvent.getTransactionActionInfoCount();
            log.info("transactionActionInfoCount:[{}]", transactionActionInfoCount);
            log.info("--------------------------------------------------------------");
            for(int i = 0; i < transactionActionInfoCount ;i++) {
                BlockInfo.TransactionEnvelopeInfo.TransactionActionInfo transactionActionInfo = transactionEvent.getTransactionActionInfo(i);

                log.info("transactionActionInfo:[{}]", transactionActionInfo);
            }
            Iterable<BlockInfo.TransactionEnvelopeInfo.TransactionActionInfo> transactionActionInfos = transactionEvent.getTransactionActionInfos();
            log.info("--------------------------------------------------------------");
            for (BlockInfo.TransactionEnvelopeInfo.TransactionActionInfo transactionActionInfo : transactionActionInfos) {
                log.info("transactionActionInfo:[{}]", transactionActionInfo);
                int inputArgsCount = transactionActionInfo.getChaincodeInputArgsCount();
                for (int i = 0; i < inputArgsCount; i++) {
                    log.info("ChaincodeInputArgs:[{}]", new String(transactionActionInfo.getChaincodeInputArgs(i), "UTF-8"));
                }
                log.info("transactionActionInfo.getChaincodeInputArgsCount():[{}]", transactionActionInfo.getChaincodeInputArgsCount());
                byte[] bytes = transactionActionInfo.getProposalResponseMessageBytes();
                String message = new String(bytes, "UTF-8");
                log.info("ProposalResponseMessage:[{}]", message);
                int endorsementCount = transactionActionInfo.getEndorsementsCount();
                for (int i = 0; i < endorsementCount; i++) {
                    log.info("transactionActionInfo.getEndorsement:[{}]", transactionActionInfo.getEndorsementInfo(i));
                }
                log.info("transactionActionInfo.getProposalResponseMessage:[{}]", transactionActionInfo.getProposalResponseMessageBytes().toString());
                byte[] payload = transactionActionInfo.getProposalResponsePayload();
                String payloadStr = new String(payload, "UTF-8");
                log.info("payload:[{}]", payloadStr);
                log.info("proposal status:[{}]", transactionActionInfo.getProposalResponseStatus());
                log.info("response message:[{}]", transactionActionInfo.getResponseMessage());
                log.info("response status:[{}]", transactionActionInfo.getResponseStatus());
                TxReadWriteSetInfo txReadWriteSetInfo = transactionActionInfo.getTxReadWriteSet();
                for (int i = 0; i < txReadWriteSetInfo.getNsRwsetCount(); i++) {
                    TxReadWriteSetInfo.NsRwsetInfo nsRwsetInfo = txReadWriteSetInfo.getNsRwsetInfo(i);
                    log.info("nsRwsetInfo:[{}]", nsRwsetInfo);
                }
            }
            transactionID = transactionEvent.getTransactionID();
            log.info("transactionID:[{}]", transactionID);
            BlockInfo.EnvelopeType envelopeType = transactionEvent.getType();
            log.info("envelopeType:[{}]", envelopeType);

            log.info("obj class:[{}]", transactionEvent);
        } catch (ProposalException | InvalidArgumentException | ExecutionException | InterruptedException | TimeoutException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        TransactionRsp transactionRsp = new TransactionRsp();
        transactionRsp.setTransactionID(transactionID);
        return transactionRsp;

    }




    public Record queryByUserId(String userId) {
        QueryByChaincodeRequest queryByChaincodeRequest = hfClient.newQueryProposalRequest();
        queryByChaincodeRequest.setArgs(new String[] {"query", userId});
        queryByChaincodeRequest.setFcn("invoke");
        queryByChaincodeRequest.setChaincodeID(chaincodeID);

        Collection<ProposalResponse> queryProposals;

        try {
            queryProposals = channel.queryByChaincode(queryByChaincodeRequest);
        } catch (Exception e) {
            throw new CompletionException(e);
        }


        boolean isEqual = false;
        Integer balance = null;

        for (ProposalResponse response : queryProposals) {
            Integer tempBalance = Integer.parseInt(response.getProposalResponse().getResponse().getPayload().toStringUtf8());
            if (null !=  balance) {
                if ( tempBalance.intValue() == balance.intValue()) {
                    isEqual = true;
                }
            } else {
                balance = tempBalance;
            }
        }

        Record record = new Record();

        if (isEqual) {
            record.setUserId(userId);
            record.setBalance(balance);
        }
        return record;
    }




//    public Test queryBlock() {
//        BlockchainInfo channelInfo = null;
//        try {
//            channelInfo = channel.queryBlockchainInfo();
//
//            for (long current = channelInfo.getHeight() - 1; current > -1; --current){
//                BlockInfo returnedBlock = channel.queryBlockByNumber(current);
//
//
//                int i = 0;
//                for (BlockInfo.EnvelopeInfo envelopeInfo : returnedBlock.getEnvelopeInfos()) {
//                    ++i;
//                    final String channelId = envelopeInfo.getChannelId();
//                    log.info("channelId:{}", channelId);
//                    if (envelopeInfo.getType() == BlockInfo.EnvelopeType.TRANSACTION_ENVELOPE) {
//                        BlockInfo.TransactionEnvelopeInfo transactionEnvelopeInfo = (BlockInfo.TransactionEnvelopeInfo) envelopeInfo;
//                        log.info("  Transaction number:{} has:{} actions", i, transactionEnvelopeInfo.getTransactionActionInfoCount());
//                        log.info("  Transaction number:{} isValid:{}", i, transactionEnvelopeInfo.isValid());
//                        log.info("  Transaction number:{} validation code:{}", i, transactionEnvelopeInfo.getValidationCode());
//
//                        int j = 0;
//                        for (BlockInfo.TransactionEnvelopeInfo.TransactionActionInfo transactionActionInfo : transactionEnvelopeInfo.getTransactionActionInfos()) {
//                            ++j;
//                            log.info("   Transaction action:{} has response status:{}", j, transactionActionInfo.getResponseStatus());
//                            log.info("   Transaction action:{} has response message bytes as string::{}", j, printableString(new String(transactionActionInfo.getResponseMessageBytes(), "UTF-8")));
//                            log.info("   Transaction action:{} has:{} endorsements", j, transactionActionInfo.getEndorsementsCount());
//
//
//
//
//
//
//                            for (int n = 0; n < transactionActionInfo.getEndorsementsCount(); ++n) {
//                                BlockInfo.EndorserInfo endorserInfo = transactionActionInfo.getEndorsementInfo(n);
//                                log.info("Endorser:{} signature::{}", n, Hex.encodeHexString(endorserInfo.getSignature()));
//                                log.info("Endorser:{} endorser::{}", n, new String(endorserInfo.getEndorser(), "UTF-8"));
//                            }
//                            log.info("   Transaction action:{} has:{} chaincode input arguments", j, transactionActionInfo.getChaincodeInputArgsCount());
//                            for (int z = 0; z < transactionActionInfo.getChaincodeInputArgsCount(); ++z) {
//                                log.info("     Transaction action:{} has chaincode input argument:{} is::{}", j, z,
//                                        printableString(new String(transactionActionInfo.getChaincodeInputArgs(z), "UTF-8")));
//                            }
//
//                            log.info("   Transaction action:{} proposal response status::{}", j,
//                                    transactionActionInfo.getProposalResponseStatus());
//                            log.info("   Transaction action:{} proposal response payload::{}", j,
//                                    printableString(new String(transactionActionInfo.getProposalResponsePayload())));
//
//                            TxReadWriteSetInfo rwsetInfo = transactionActionInfo.getTxReadWriteSet();
//                            if (null != rwsetInfo) {
//                                log.info("   Transaction action:{} has:{} name space read write sets", j, rwsetInfo.getNsRwsetCount());
//
//                                for (TxReadWriteSetInfo.NsRwsetInfo nsRwsetInfo : rwsetInfo.getNsRwsetInfos()) {
//                                    final String namespace = nsRwsetInfo.getNamespace();
//                                    KvRwset.KVRWSet rws = nsRwsetInfo.getRwset();
//
//                                    int rs = -1;
//                                    for (KvRwset.KVRead readList : rws.getReadsList()) {
//                                        rs++;
//
//                                        log.info("     Namespace:{} read set:{} key:{}  version [{}:{}]", namespace, rs, readList.getKey(),
//                                                readList.getVersion().getBlockNum(), readList.getVersion().getTxNum());
//
////                                        if ("bar".equals(channelId) && blockNumber == 2) {
////                                            if ("example_cc_go".equals(namespace)) {
////                                                if (rs == 0) {
////                                                    assertEquals("a", readList.getKey());
////                                                    assertEquals(1, readList.getVersion().getBlockNum());
////                                                    assertEquals(0, readList.getVersion().getTxNum());
////                                                } else if (rs == 1) {
////                                                    assertEquals("b", readList.getKey());
////                                                    assertEquals(1, readList.getVersion().getBlockNum());
////                                                    assertEquals(0, readList.getVersion().getTxNum());
////                                                } else {
////                                                    fail(format("unexpected readset:{}", rs));
////                                                }
////
////                                                TX_EXPECTED.remove("readset1");
////                                            }
////                                        }
//                                    }
//
//                                    rs = -1;
//                                    for (KvRwset.KVWrite writeList : rws.getWritesList()) {
//                                        rs++;
//                                        String valAsString = printableString(new String(writeList.getValue().toByteArray(), "UTF-8"));
//
//                                        log.info("     Namespace:{} write set:{} key:{} has value {} ", namespace, rs,
//                                                writeList.getKey(),
//                                                valAsString);
//
////                                        if ("bar".equals(channelId) && blockNumber == 2) {
////                                            if (rs == 0) {
////                                                assertEquals("a", writeList.getKey());
////                                                assertEquals("400", valAsString);
////                                            } else if (rs == 1) {
////                                                assertEquals("b", writeList.getKey());
////                                                assertEquals("400", valAsString);
////                                            } else {
////                                                fail(format("unexpected writeset:{}", rs));
////                                            }
////
////                                            TX_EXPECTED.remove("writeset1");
////                                        }
//                                    }
//                                }
//                            }
//
//
//
//
//
//
//
//
//
//
//                        }
//
//
//                    }
//
//
//                }
//
//                log.info("=========================================================================================================================================================");
//                log.info("=========================================================================================================================================================");
//                log.info("=========================================================================================================================================================");
//                log.info("=========================================================================================================================================================");
//            }
//
//
//        } catch (ProposalException | InvalidArgumentException | UnsupportedEncodingException | InvalidProtocolBufferException e) {
//            e.printStackTrace();
//        }
//
//
//        assert channelInfo != null;
//        Test test = new Test();
//        test.setCount(Long.valueOf(channelInfo.getHeight()).intValue());
//        return test;
//    }



    static String printableString(final String string) {
        int maxLogStringLength = 64;
        if (string == null || string.length() == 0) {
            return string;
        }

        String ret = string.replaceAll("[^\\p{Print}]", "?");

        ret = ret.substring(0, Math.min(ret.length(), maxLogStringLength)) + (ret.length() > maxLogStringLength ? "..." : "");

        return ret;

    }


    public List<String> queryBlockByTxID(String txID) {
        List<String> result = new ArrayList<>();
        try {
            BlockInfo blockInfo = channel.queryBlockByTransactionID(txID);
            List<ByteString> dataList = blockInfo.getBlock().getData().getDataList();
            for (ByteString bytes : dataList) {
                String tem = bytes.toString("utf-8");
                result.add(tem);
                log.info("block data :[{}]", tem);
            }
            log.info("");
        } catch (InvalidArgumentException | ProposalException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return result;

    }
}



/**









                getChaincodeInputArgs(int index)
               getChaincodeInputArgsCount()
	                      getEndorsementInfo(int index)
              getEndorsementsCount()
     getProposalResponseMessageBytes()
     getProposalResponsePayload()
       getProposalResponseStatus()
   getResponseMessage()
   getResponseMessageBytes()
       getResponseStatus()
   getTxReadWriteSet()














 */