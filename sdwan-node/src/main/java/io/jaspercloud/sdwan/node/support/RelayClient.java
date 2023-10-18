package io.jaspercloud.sdwan.node.support;

import io.jaspercloud.sdwan.Ecdh;
import io.jaspercloud.sdwan.stun.*;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.InitializingBean;

import javax.crypto.SecretKey;
import java.security.KeyPair;

@Slf4j
public class RelayClient implements InitializingBean {

    private SDWanNodeProperties properties;
    private StunClient stunClient;
    private String localVIP;
    private KeyPair ecdhKeyPair;
    private String publicKey;
    private SecretKey secretKey;

    public SecretKey getSecretKey() {
        return secretKey;
    }

    public void setLocalVIP(String localVIP) {
        this.localVIP = localVIP;
    }

    public RelayClient(SDWanNodeProperties properties, StunClient stunClient) {
        this.properties = properties;
        this.stunClient = stunClient;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ecdhKeyPair = Ecdh.generateKeyPair();
        Thread thread = new Thread(() -> {
            while (true) {
                if (StringUtils.isNotEmpty(localVIP)) {
                    try {
                        StunPacket stunPacket = stunClient.sendBindRelay(properties.getRelayServer(), localVIP, Hex.toHexString(ecdhKeyPair.getPublic().getEncoded()), 3000).get();
                        StringAttr encryptKeyAttr = (StringAttr) stunPacket.content().getAttrs().get(AttrType.EncryptKey);
                        if (!StringUtils.equals(encryptKeyAttr.getData(), publicKey)) {
                            publicKey = encryptKeyAttr.getData();
                            secretKey = Ecdh.generateAESKey(ecdhKeyPair.getPrivate(), Hex.decode(publicKey));
                        }
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }, "relay-client-heart");
        thread.setDaemon(true);
        thread.start();
    }

    public StunPacket createRelayPacket(String localVIP, String dstVIP, ByteBuf byteBuf) {
        StunMessage message = new StunMessage(MessageType.Transfer);
        message.getAttrs().put(AttrType.SrcVIP, new StringAttr(localVIP));
        message.getAttrs().put(AttrType.DstVIP, new StringAttr(dstVIP));
        message.getAttrs().put(AttrType.Data, new ByteBufAttr(byteBuf));
        StunPacket packet = new StunPacket(message, properties.getRelayServer());
        return packet;
    }
}
