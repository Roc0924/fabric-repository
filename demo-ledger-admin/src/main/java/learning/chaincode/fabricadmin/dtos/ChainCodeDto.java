package learning.chaincode.fabricadmin.dtos;

import lombok.Data;

/**
 * Create with IntelliJ IDEA
 * Author               : wangzhenpeng
 * Date                 : 2018/3/5
 * Time                 : 15:23
 * Description          :
 */
@Data
public class ChainCodeDto {
    private String path;
    private String name;
    private String version;
}
