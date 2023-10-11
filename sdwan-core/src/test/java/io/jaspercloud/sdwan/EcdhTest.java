package io.jaspercloud.sdwan;

import org.apache.commons.lang3.StringUtils;

import javax.crypto.SecretKey;
import java.security.KeyPair;

public class EcdhTest {

    public static void main(String[] args) throws Exception {
        KeyPair p1 = Ecdh.generateKeyPair();
        KeyPair p2 = Ecdh.generateKeyPair();

        SecretKey encodeKey = Ecdh.generateAESKey(p1.getPrivate(), p2.getPublic().getEncoded());
        SecretKey decodeKey = Ecdh.generateAESKey(p2.getPrivate(), p1.getPublic().getEncoded());

        String input = "say hello";
        byte[] encode = Ecdh.encryptAES(input.getBytes(), encodeKey);
        byte[] decode = Ecdh.decryptAES(encode, decodeKey);
        String output = new String(decode);

        System.out.println(StringUtils.equals(input, output));
    }
}
