package learning.blockchain.test.services;

import learning.blockchain.test.dtos.ChainCodeDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Create with IntelliJ IDEA
 * Author               : wangzhenpeng
 * Date                 : 2018/3/9
 * Time                 : 下午2:09
 * Description          : 链码服务类
 */
@Service("chainCodeService")
public class ChainCodeService {

    public ChainCodeDTO install() {


        return null;
    }



    public List<ChainCodeDTO> queryInstalledChainCodeByName(String ChainCodeName) {
        List<ChainCodeDTO> chainCodeDTOS = queryInstalledChainCodes();
        List<ChainCodeDTO> result = new ArrayList<>();
        for (ChainCodeDTO chainCodeDTO : chainCodeDTOS) {
            if (chainCodeDTO.getName().equals(ChainCodeName)) {
                result.add(chainCodeDTO);
            }
        }
        return result;
    }

    private List<ChainCodeDTO> queryInstalledChainCodes() {
        return null;
    }

}
