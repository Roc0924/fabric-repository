package learning.chaincode.ledger.entitys;

import lombok.Data;

/**
 * Create with IntelliJ IDEA
 * Author               : wangzhenpeng
 * Date                 : 2018/3/14
 * Time                 : 下午5:44
 * Description          : 返利账户实体
 */
@Data
public class RebateAccount {
    private String accountId;
    private Long amount;
    private Long expectAmount;
    private String status;
    private String details;
    private String memo;

}
