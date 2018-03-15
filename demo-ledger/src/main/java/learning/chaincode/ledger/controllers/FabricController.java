package learning.chaincode.ledger.controllers;


import learning.chaincode.ledger.dtos.ChainCodeDto;
import learning.chaincode.ledger.entitys.RebateAccount;
import learning.chaincode.ledger.services.FabricService;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    private final FabricService fabricService;

    @Autowired
    public FabricController(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    @RequestMapping(value = "/createAccount", method = RequestMethod.GET)
    public Boolean createAccount(
            @RequestBody RebateAccount rebateAccount) {

        return fabricService.createAccount(rebateAccount);
    }


    @RequestMapping(value = "/queryAccount", method = RequestMethod.GET)
    public RebateAccount queryAccount(
            @RequestParam(name = "planId") String planId) {

        return fabricService.queryAccount(planId);
    }

}
