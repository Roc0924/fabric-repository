package learning.chaincode.fabricadmin.dtos;

import lombok.Data;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * Create with IntelliJ IDEA
 * Author               : wangzhenpeng
 * Date                 : 2018/2/13
 * Time                 : 15:18
 * Description          :
 */
@Data
@Component
public class InstalledProposal {
    private Collection<ProposalResponse> successful;
    private Collection<ProposalResponse> failed;
}
