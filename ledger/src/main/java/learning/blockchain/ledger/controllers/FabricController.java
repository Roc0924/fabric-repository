package learning.blockchain.ledger.controllers;

import learning.chaincode.demo.configs.InstalledProposal;
import learning.chaincode.demo.dtos.Record;
import learning.chaincode.demo.dtos.SampleOrg;
import learning.chaincode.demo.dtos.TransactionRsp;
import learning.chaincode.demo.services.FabricService;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Create with IntelliJ IDEA
 * Author               : wangzhenpeng
 * Date                 : 2018/2/9
 * Time                 : 14:05
 * Description          : fabric 控制类
 */
@RestController
@RequestMapping("/fabric")
public class FabricController {

    private final HFClient hfClient;

    private final FabricService fabricService;

    private final Channel channel;

    private final ChaincodeID chaincodeID;

    @Autowired
    public FabricController(HFClient hfClient, FabricService fabricService, Channel channel, ChaincodeID chaincodeID) {
        this.hfClient = hfClient;
        this.fabricService = fabricService;
        this.channel = channel;
        this.chaincodeID = chaincodeID;
    }

    @RequestMapping(value = "/transaction", method = RequestMethod.GET)
    public TransactionRsp transaction(@RequestParam(name = "source") String source,
                                      @RequestParam(name = "destination") String destination,
                                      @RequestParam(name = "delta") String delta) {
        SampleOrg org = fabricService.getOrg("peerOrg1");
        return fabricService.transaction(channel, hfClient, org, chaincodeID, source, destination, delta);
    }


    @RequestMapping(value = "/query", method = RequestMethod.GET)
    public Record query(@RequestParam(name = "userId") String userId) {
        return fabricService.queryByUserId(userId);
    }

//    @RequestMapping(value = "/queryBlocks", method = RequestMethod.GET)
//    public Test queryBlocks() {
//        return fabricService.queryBlock();
//    }


    @RequestMapping(value = "/queryBlockByTxID", method = RequestMethod.GET)
    public List<String> queryBlockByTxID(@RequestParam(name = "transactionID") String transactionID) {
        return fabricService.queryBlockByTxID(transactionID);
    }






}
