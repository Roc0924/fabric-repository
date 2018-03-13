package learning.chaincode.fabricadmin.dtos;

import org.hyperledger.fabric.sdk.Channel;

import java.util.HashMap;
import java.util.Map;

/**
 * Create with IntelliJ IDEA
 * Author               : wangzhenpeng
 * Date                 : 2018/3/8
 * Time                 : 下午3:31
 * Description          :
 */

public class LedgerChannels {
    Map<String, Channel> channels = new HashMap<>();

    public Map<String, Channel> getChannels() {
        return channels;
    }

    public void setChannels(Map<String, Channel> channels) {
        this.channels = channels;
    }
}
