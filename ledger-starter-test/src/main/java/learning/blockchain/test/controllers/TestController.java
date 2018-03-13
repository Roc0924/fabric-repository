package learning.blockchain.test.controllers;

import learning.blockchain.test.dtos.ChainCodeDTO;
import learning.blockchain.test.services.ChainCodeService;
import org.hyperledger.fabric.sdk.HFClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Create with IntelliJ IDEA
 * Author               : wangzhenpeng
 * Date                 : 2018/3/8
 * Time                 : 下午12:17
 * Description          :
 */
@RestController
public class TestController {

    final HFClient hfClient;
//    final LedgerOrgs ledgerOrgs;
    final ChainCodeService chainCodeService;

    @Autowired
    public TestController(HFClient hfClient,
//                          LedgerOrgs ledgerOrgs,
                          ChainCodeService chainCodeService) {
        this.hfClient = hfClient;
//        this.ledgerOrgs = ledgerOrgs;
        this.chainCodeService = chainCodeService;
    }


    @RequestMapping(value = "/test", method = RequestMethod.GET)
    public String getName() {
        System.out.println("test");
        return hfClient.toString();
    }

    @RequestMapping(value = "/install", method = RequestMethod.GET)
    public ChainCodeDTO install() {
        return chainCodeService.install();
    }

}
