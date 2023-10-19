package io.jaspercloud.sdwan.node.support;

import io.jaspercloud.sdwan.AsyncTask;
import io.jaspercloud.sdwan.Ecdh;
import io.jaspercloud.sdwan.stun.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.InitializingBean;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Slf4j
public class RelayClient implements InitializingBean {

    private SDWanNodeProperties properties;
    private StunClient stunClient;
    private String localVIP;
    private KeyPair ecdhKeyPair;
    private SecretKey secretKey;
    private Consumer<SecretKey> consumer;

    public SecretKey getSecretKey() {
        return secretKey;
    }

    public void setLocalVIP(String localVIP) {
        this.localVIP = localVIP;
    }

    public void onUpdateSecretKey(Consumer<SecretKey> consumer) {
        this.consumer = consumer;
        consumer.accept(secretKey);
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
                        String publicKey = Hex.toHexString(ecdhKeyPair.getPublic().getEncoded());
                        StunPacket stunPacket = sendBindRelay(localVIP, publicKey, 3000).get();
                        StringAttr encryptKeyAttr = (StringAttr) stunPacket.content().getAttrs().get(AttrType.EncryptKey);
                        secretKey = Ecdh.generateAESKey(ecdhKeyPair.getPrivate(), Hex.decode(encryptKeyAttr.getData()));
                        if (null != consumer) {
                            consumer.accept(secretKey);
                        }
                        while (true) {
                            sendRelayHeart(localVIP, 3000).get();
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException e) {
                                log.error(e.getMessage(), e);
                            }
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

    public StunPacket createRelayPacket(String localVIP, String dstVIP, byte[] bytes) {
        StunMessage message = new StunMessage(MessageType.Transfer);
        message.getAttrs().put(AttrType.SrcVIP, new StringAttr(localVIP));
        message.getAttrs().put(AttrType.DstVIP, new StringAttr(dstVIP));
        message.getAttrs().put(AttrType.Data, new BytesAttr(bytes));
        StunPacket packet = new StunPacket(message, properties.getRelayServer());
        return packet;
    }

    private CompletableFuture<StunPacket> sendBindRelay(String vip, String publicKey, long timeout) {
        StunMessage message = new StunMessage(MessageType.BindRelayRequest);
        message.getAttrs().put(AttrType.VIP, new StringAttr(vip));
        message.getAttrs().put(AttrType.EncryptKey, new StringAttr(publicKey));
        StunPacket request = new StunPacket(message, properties.getRelayServer());
        CompletableFuture<StunPacket> future = AsyncTask.waitTask(request.content().getTranId(), timeout);
        stunClient.getChannel().writeAndFlush(request);
        return future;
    }

    private CompletableFuture<StunPacket> sendRelayHeart(String vip, long timeout) {
        StunMessage message = new StunMessage(MessageType.RelayHeart);
        message.getAttrs().put(AttrType.VIP, new StringAttr(vip));
        StunPacket request = new StunPacket(message, properties.getRelayServer());
        CompletableFuture<StunPacket> future = AsyncTask.waitTask(request.content().getTranId(), timeout);
        stunClient.getChannel().writeAndFlush(request);
        return future;
    }
}
