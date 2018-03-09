package learning.blockchain.test.services;

import learning.blockchain.configuration.LedgerOrgs;
import learning.blockchain.entitys.LedgerOrg;
import learning.blockchain.entitys.LedgerStore;
import learning.blockchain.entitys.LedgerUser;
import learning.blockchain.utils.Util;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.exception.EnrollmentException;
import org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import static java.lang.String.format;

/**
 * Create with IntelliJ IDEA
 * Author               : wangzhenpeng
 * Date                 : 2018/3/9
 * Time                 : 上午9:41
 * Description          :
 */
@Slf4j
@Service("userService")
public class UserService {

    @Value("${ledger.channelPath}")
    private String channelPath;

    @Value("${ledger.cryptoConfigPath}")
    private String cryptoConfigPath;

    final LedgerOrgs ledgerOrgs;

    @Autowired
    public UserService(LedgerOrgs ledgerOrgs) {
        this.ledgerOrgs = ledgerOrgs;
    }

    public Boolean login(String orgName, String userName, String secret) {
        LedgerOrg ledgerOrg = ledgerOrgs.getLedgerOrgs().get(orgName);

        try {
            ledgerOrg.setCAClient(HFCAClient.createNewInstance(ledgerOrg.getCALocation(), ledgerOrg.getCAProperties()));

            File ledgerStoreFile = new File(System.getProperty("java.io.tmpdir") + "/HFC.properties");
            if (ledgerStoreFile.exists()) {
                boolean result = ledgerStoreFile.delete();
                if (result) {
                    log.info("remove HFC.properties");
                }
            }

            final LedgerStore ledgerStore = new LedgerStore(ledgerStoreFile);

            HFCAClient ca = ledgerOrg.getCAClient();
            final String mspId = ledgerOrg.getMSPID();
            ca.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
            LedgerUser admin = ledgerStore.getMember(userName, orgName);
            if (!admin.isEnrolled()) {  //Preregistered admin only needs to be enrolled with Fabric caClient.
                admin.setEnrollment(ca.enroll(admin.getName(), secret));
                admin.setMspId(mspId);
            }

            ledgerOrg.setAdmin(admin);


            LedgerUser peerOrgAdmin = ledgerStore.getMember(orgName + "Admin", orgName, ledgerOrg.getMSPID(),
                    Util.findFileSk(
                            Paths.get(
                                    "fabric-admin\\target\\classes",
                                    channelPath,
                                    cryptoConfigPath,
                                    ledgerOrg.getDomainName(),
                                    format(
                                            "/users/Admin@%s/msp/keystore",
                                            ledgerOrg.getDomainName()
                                    )
                            ).toFile()
                    ),
                    Paths.get(
                            "fabric-admin\\target\\classes",
                            channelPath,
                            cryptoConfigPath,
                            ledgerOrg.getDomainName(),
                            format(
                                    "/users/Admin@%s/msp/signcerts/Admin@%s-cert.pem",
                                    ledgerOrg.getDomainName(),
                                    ledgerOrg.getDomainName()
                            )
                    ).toFile()
            );

            ledgerOrg.setPeerAdmin(peerOrgAdmin); //A special user that can create channels, join peers and install chaincode


        } catch (EnrollmentException | InvalidArgumentException | IOException e) {
            log.error("login error: [{}]", e);
            return Boolean.FALSE;

        }
        return Boolean.TRUE;
    }
}
