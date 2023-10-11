package io.jaspercloud.sdwan;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public final class Ecdh {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private Ecdh() {

    }

    //生成密钥对
    public static KeyPair generateKeyPair() throws NoSuchProviderException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp256r1");
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDH", "BC");
        keyPairGenerator.initialize(ecSpec);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        return keyPair;
    }

    //生成共享密钥
    private static byte[] generateSecret(PrivateKey privateKey, byte[] publicKeyBytes) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException {
        KeyAgreement bobKeyAgreement = KeyAgreement.getInstance("ECDH", "BC");
        bobKeyAgreement.init(privateKey);
        PublicKey alicePublicKey = KeyFactory.getInstance("ECDH", "BC").generatePublic(new X509EncodedKeySpec(publicKeyBytes));
        bobKeyAgreement.doPhase(alicePublicKey, true);
        byte[] sharedSecret = bobKeyAgreement.generateSecret();
        return sharedSecret;
    }

    // 生成AES密钥
    public static SecretKey generateAESKey(PrivateKey privateKey, byte[] publicKeyBytes) throws Exception {
        byte[] sharedSecret = generateSecret(privateKey, publicKeyBytes);
        // 使用256位AES密钥
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES", "BC");
        keyGenerator.init(256);
        return new SecretKeySpec(sharedSecret, "AES");
    }

    // 使用AES加密数据
    public static byte[] encryptAES(byte[] data, SecretKey aesKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey);
        return cipher.doFinal(data);
    }

    // 使用AES解密数据
    public static byte[] decryptAES(byte[] data, SecretKey aesKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES", "BC");
        cipher.init(Cipher.DECRYPT_MODE, aesKey);
        return cipher.doFinal(data);
    }
}
