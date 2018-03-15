package learning.chaincode.ledger.controllers;


import learning.chaincode.ledger.entitys.RebateAccount;
import learning.chaincode.ledger.services.FabricService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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

    private final FabricService fabricService;

    @Autowired
    public FabricController(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    @RequestMapping(value = "/createAccount", method = RequestMethod.POST)
    public Boolean createAccount(
            @RequestBody RebateAccount rebateAccount) {

        return fabricService.createAccount(rebateAccount);
    }


    @RequestMapping(value = "/queryAccount", method = RequestMethod.GET)
    public RebateAccount queryAccount(
            @RequestParam(name = "accountId") String accountId) {

        return fabricService.queryAccount(accountId);
    }

    @RequestMapping(value = "/deleteAccount", method = RequestMethod.GET)
    public RebateAccount deleteAccount(
            @RequestParam(name = "accountId") String accountId) {
        return fabricService.deleteAccount(accountId);

    }

    @RequestMapping(value = "/queryAccountHistory", method = RequestMethod.GET)
    public Map<String, Object> queryAccountHistory(
            @RequestParam(name = "accountId") String accountId) {
        return fabricService.queryAccountHistory(accountId);

    }

}
