package learning.chaincode.fabricadmin.controllers;


import learning.chaincode.fabricadmin.dtos.ChainCodeDto;
import learning.chaincode.fabricadmin.services.FabricService;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @RequestMapping(value = "/install", method = RequestMethod.GET)
    public ChainCodeDto install() {
        return fabricService.installChainCode();
    }



    @RequestMapping(value = "/queryInstalledChaincodes", method = RequestMethod.GET)
    public List<ChainCodeDto> queryInstalledChaincodes() {
        return fabricService.queryInstalledChainCodes();
    }

    @RequestMapping(value = "/queryInstalledChaincodeByName", method = RequestMethod.GET)
    public ChainCodeDto queryInstalledChaincodeByName(@RequestParam(name = "chainCodeName") String chainCodeName) {
        return fabricService.queryInstalledChainCodeByName(chainCodeName);
    }


    @RequestMapping(value = "/queryInstalledChaincodeByNameAndVersion", method = RequestMethod.GET)
    public ChainCodeDto queryInstalledChaincodeByNameAndVersion(
            @RequestParam(name = "name") String name,
            @RequestParam(name = "version") String version
    ) {
        return fabricService.queryInstalledChainCodeByNameAndVersion(name, version);
    }


    @RequestMapping(value = "/upgradeInstalledChaincode", method = RequestMethod.PUT)
    public ChainCodeDto upgradeInstalledChaincode(@RequestBody ChainCodeDto chainCodeDto) {
        ChaincodeID newChaincodeID = ChaincodeID.newBuilder().setName(chainCodeDto.getName())
                .setPath(chainCodeDto.getPath()).setVersion(chainCodeDto.getVersion()).build();

        return fabricService.upgradeInstalledChaincode(newChaincodeID);
    }


    @RequestMapping(value = "/queryBlockById", method = RequestMethod.GET)
    public ChainCodeDto queryBlockById(@RequestBody ChainCodeDto chainCodeDto) {
        ChaincodeID newChaincodeID = ChaincodeID.newBuilder().setName(chainCodeDto.getName())
                .setPath(chainCodeDto.getPath()).setVersion(chainCodeDto.getVersion()).build();

        return fabricService.upgradeInstalledChaincode(newChaincodeID);
    }


    @RequestMapping(value = "/injectBudget", method = RequestMethod.GET)
    public Boolean injectBudget(
            @RequestParam(name = "planId") String planId,
            @RequestParam(name = "budgetAmount") Integer budgetAmount) {

        return fabricService.injectBudget(planId, budgetAmount);
    }


    @RequestMapping(value = "/queryBudget", method = RequestMethod.GET)
    public String queryBudget(
            @RequestParam(name = "planId") String planId) {

        return fabricService.queryBudget(planId);
    }

}
