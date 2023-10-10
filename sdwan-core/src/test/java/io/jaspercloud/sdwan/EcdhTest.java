package io.jaspercloud.sdwan;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.util.encoders.Hex;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.X509EncodedKeySpec;

public class EcdhTest {

    public static void main(String[] args) throws Exception {
        // 添加Bouncy Castle作为Java安全提供程序
        Security.addProvider(new BouncyCastleProvider());

        // 选择ECDH曲线（例如，"secp256r1"是一种常用的曲线）
        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp256r1");
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDH", "BC");
        keyPairGenerator.initialize(ecSpec);

        // 生成Alice的密钥对
        KeyPair aliceKeyPair = keyPairGenerator.generateKeyPair();

        // 生成Bob的密钥对
        KeyPair bobKeyPair = keyPairGenerator.generateKeyPair();

        // Bob将Bob的公钥发送给Alice
        byte[] bobPublicKeyBytes = bobKeyPair.getPublic().getEncoded();

        // Alice将Alice的公钥发送给Bob
        byte[] alicePublicKeyBytes = aliceKeyPair.getPublic().getEncoded();

        // Bob接收到Alice的公钥并生成共享密钥
        KeyAgreement bobKeyAgreement = KeyAgreement.getInstance("ECDH", "BC");
        bobKeyAgreement.init(bobKeyPair.getPrivate());
        PublicKey alicePublicKey = KeyFactory.getInstance("ECDH", "BC").generatePublic(new X509EncodedKeySpec(alicePublicKeyBytes));
        bobKeyAgreement.doPhase(alicePublicKey, true);
        byte[] bobSharedSecret = bobKeyAgreement.generateSecret();

        // Alice接收到Bob的公钥并生成共享密钥
        KeyAgreement aliceKeyAgreement = KeyAgreement.getInstance("ECDH", "BC");
        aliceKeyAgreement.init(aliceKeyPair.getPrivate());
        PublicKey bobPublicKey = KeyFactory.getInstance("ECDH", "BC").generatePublic(new X509EncodedKeySpec(bobPublicKeyBytes));
        aliceKeyAgreement.doPhase(bobPublicKey, true);
        byte[] aliceSharedSecret = aliceKeyAgreement.generateSecret();

        // 打印共享密钥，确保Alice和Bob的共享密钥相同
        String aliceHexKey = Hex.toHexString(aliceSharedSecret);
        System.out.println("Shared Secret (Alice): " + aliceHexKey);
        String bobHexKey = Hex.toHexString(bobSharedSecret);
        System.out.println("Shared Secret (Bob): " + bobHexKey);
        System.out.println(StringUtils.equals(aliceHexKey, bobHexKey));

        SecretKey aliceKey = generateAESKey(aliceSharedSecret);
        SecretKey bobKey = generateAESKey(bobSharedSecret);

        String input = "say hello";
        byte[] encode = encryptAES(input.getBytes(), aliceKey);
        byte[] decode = decryptAES(encode, bobKey);
        String output = new String(decode);
        System.out.println(StringUtils.equals(input, output));
    }

    // 生成AES密钥
    public static SecretKey generateAESKey(byte[] sharedSecret) throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES", "BC");
        keyGenerator.init(256); // 使用256位AES密钥
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
