package learning.blockchain.ledgeradmin.entitys;

import learning.blockchain.ledgeradmin.dtos.SampleStore;
import learning.blockchain.ledgeradmin.dtos.SampleUser;
import org.bouncycastle.util.encoders.Hex;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.User;

import java.io.*;
import java.util.Set;

/**
 * Create with IntelliJ IDEA
 * Author               : wangzhenpeng
 * Date                 : 2018/3/6
 * Time                 : 下午6:25
 * Description          : 账本中的用户
 */

public class LedgerUser implements User, Serializable{
    private static final long serialVersionUID = 8966887165686602959L;

    private String name;
    private Set<String> roles;
    private String account;
    private String affiliation;
    private String organization;
    private String enrollmentSecret;
    Enrollment enrollment = null;

    private transient SampleStore keyValStore;
    private String keyValStoreName;


    LedgerUser(String name, String ledgerOrg, LedgerStore ledgerStore) {
        this.name = name;
        this.organization = ledgerOrg;
        this.keyValStoreName = toKeyValStoreName(this.name, ledgerOrg);
        String memberStr = keyValStore.getValue(keyValStoreName);
        if (null == memberStr) {
            saveState();
        } else {
            restoreState();
        }

    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public Set<String> getRoles() {
        return null;
    }

    @Override
    public String getAccount() {
        return null;
    }

    @Override
    public String getAffiliation() {
        return null;
    }

    @Override
    public Enrollment getEnrollment() {
        return null;
    }

    @Override
    public String getMspId() {
        return null;
    }

    public static String toKeyValStoreName(String name, String org) {
        return "user." + name + org;
    }

    /**
     * Save the state of this user to the key value store.
     */
    void saveState() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(this);
            oos.flush();
            keyValStore.setValue(keyValStoreName, Hex.toHexString(bos.toByteArray()));
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Restore the state of this user from the key value store (if found).  If not found, do nothing.
     */
    SampleUser restoreState() {
        String memberStr = keyValStore.getValue(keyValStoreName);
        if (null != memberStr) {
            // The user was found in the key value store, so restore the
            // state.
            byte[] serialized = Hex.decode(memberStr);
            ByteArrayInputStream bis = new ByteArrayInputStream(serialized);
            try {
                ObjectInputStream ois = new ObjectInputStream(bis);
                SampleUser state = (SampleUser) ois.readObject();
                if (state != null) {
                    this.name = state.name;
                    this.roles = state.roles;
                    this.account = state.account;
                    this.affiliation = state.affiliation;
                    this.organization = state.organization;
                    this.enrollmentSecret = state.enrollmentSecret;
                    this.enrollment = state.enrollment;
                    this.mspId = state.mspId;
                    return this;
                }
            } catch (Exception e) {
                throw new RuntimeException(String.format("Could not restore state of member %s", this.name), e);
            }
        }
        return null;
    }
}
