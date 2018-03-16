package learning.blockchain.fabric.entitys;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.hyperledger.fabric.sdk.Enrollment;

import java.io.*;
import java.security.PrivateKey;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Create with IntelliJ IDEA
 * Author               : wangzhenpeng
 * Date                 : 2018/3/6
 * Time                 : 下午10:48
 * Description          : A local file-based key value store.
 */

public class FabricStore {
    private Log logger = LogFactory.getLog(FabricStore.class);

    private String file;
    private final Map<String, FabricUser> members = new HashMap<>();


    public FabricStore(File file) {
        this.file = file.getAbsolutePath();
    }

    /**
     * Get the value associated with name.
     *
     * @param name
     * @return value associated with the name
     */
    public String getValue(String name) {
        Properties properties = loadProperties();
        return properties.getProperty(name);
    }

    private Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream(file)) {
            properties.load(input);
            input.close();
        } catch (FileNotFoundException e) {
            logger.warn(String.format("Could not find the file \"%s\"", file));
        } catch (IOException e) {
            logger.warn(String.format("Could not load keyvalue store from file \"%s\", reason:%s",
                    file, e.getMessage()));
        }

        return properties;
    }

    /**
     * Set the value associated with name.
     *
     * @param name  The name of the parameter
     * @param value Value for the parameter
     */
    public void setValue(String name, String value) {
        Properties properties = loadProperties();
        try (
                OutputStream output = new FileOutputStream(file)
        ) {
            properties.setProperty(name, value);
            properties.store(output, "");
            output.close();

        } catch (IOException e) {
            logger.warn(String.format("Could not save the keyvalue store, reason:%s", e.getMessage()));
        }
    }

    /**
     * Get the user with a given name
     * @param name
     * @param org
     * @return user
     */
    public FabricUser getMember(String name, String org) {

        // Try to get the FabricUser state from the cache
        FabricUser fabricUser = members.get(FabricUser.toKeyValStoreName(name, org));
        if (null != fabricUser) {
            return fabricUser;
        }

        // Create the FabricUser and try to restore it's state from the key value store (if found).
        fabricUser = new FabricUser(name, org, this);

        return fabricUser;

    }



    /**
     * Get the user with a given name
     * @param name
     * @param org
     * @param mspId
     * @param privateKeyFile
     * @param certificateFile
     * @return user
     * @throws IOException
     */
    public FabricUser getMember(String name, String org, String mspId, File privateKeyFile,
                                File certificateFile) throws IOException {

        try {
            // Try to get the FabricUser state from the cache
            FabricUser fabricUser = members.get(FabricUser.toKeyValStoreName(name, org));
            if (null != fabricUser) {
                return fabricUser;
            }

            // Create the FabricUser and try to restore it's state from the key value store (if found).
            fabricUser = new FabricUser(name, org, this);
            fabricUser.setMspId(mspId);

            String certificate = new String(IOUtils.toByteArray(new FileInputStream(certificateFile)), "UTF-8");

            PrivateKey privateKey = getPrivateKeyFromBytes(IOUtils.toByteArray(new FileInputStream(privateKeyFile)));

            fabricUser.setEnrollment(new FabricStoreEnrollement(privateKey, certificate));

            fabricUser.saveState();

            return fabricUser;
        } catch (IOException | ClassCastException e) {
            e.printStackTrace();
            throw e;

        }

    }


    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    static PrivateKey getPrivateKeyFromBytes(byte[] data) throws IOException{
        final Reader pemReader = new StringReader(new String(data));

        final PrivateKeyInfo pemPair;
        try (PEMParser pemParser = new PEMParser(pemReader)) {
            pemPair = (PrivateKeyInfo) pemParser.readObject();
        }

        PrivateKey privateKey = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME).getPrivateKey(pemPair);

        return privateKey;
    }

    static final class FabricStoreEnrollement implements Enrollment, Serializable {

        private static final long serialVersionUID = -7077068549994159470L;
        private final PrivateKey privateKey;
        private final String certificate;


        FabricStoreEnrollement(PrivateKey privateKey, String certificate)  {


            this.certificate = certificate;

            this.privateKey =  privateKey;
        }

        @Override
        public PrivateKey getKey() {

            return privateKey;
        }

        @Override
        public String getCert() {
            return certificate;
        }

    }


}
