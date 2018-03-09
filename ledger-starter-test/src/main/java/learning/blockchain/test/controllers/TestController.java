package learning.blockchain.test.controllers;

import learning.blockchain.configuration.LedgerOrgs;
import learning.blockchain.test.services.UserService;
import org.hyperledger.fabric.sdk.HFClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
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
    final LedgerOrgs ledgerOrgs;
    final UserService userService;

    @Autowired
    public TestController(HFClient hfClient, LedgerOrgs ledgerOrgs, UserService userService) {
        this.hfClient = hfClient;
        this.ledgerOrgs = ledgerOrgs;
        this.userService = userService;
    }


    @RequestMapping(value = "/test", method = RequestMethod.GET)
    public String getName() {
        System.out.println("test");
        return hfClient.toString();
    }

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public Boolean login(@RequestParam(name = "orgName") String orgName,
                         @RequestParam(name = "userName") String userName,
                         @RequestParam(name = "secret") String secret) {
        return userService.login(orgName, userName, secret);
    }

}
