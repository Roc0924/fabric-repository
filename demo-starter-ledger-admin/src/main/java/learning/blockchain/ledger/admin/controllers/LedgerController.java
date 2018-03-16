package learning.blockchain.ledger.admin.controllers;

import learning.blockchain.ledger.admin.services.LedgerService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Create with IntelliJ IDEA
 * Author               : wangzhenpeng
 * Date                 : 2018/3/16
 * Time                 : 下午2:20
 * Description          : 账本控制类
 */

@RestController
@RequestMapping("/ledger")
public class LedgerController {
    final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }


    @RequestMapping(value = "/test", method = RequestMethod.GET)
    public void test() {
        ledgerService.test();

    }

}
