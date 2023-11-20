package io.jaspercloud.sdwan.node.support;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.stun.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class MappingManager implements InitializingBean, Runnable {

    private SDWanNodeProperties properties;
    private StunClient stunClient;

    private AtomicReference<MappingAddress> ref = new AtomicReference<>();

    public MappingAddress getMappingAddress() {
        return ref.get();
    }

    public MappingManager(SDWanNodeProperties properties, StunClient stunClient) {
        this.properties = properties;
        this.stunClient = stunClient;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        MappingAddress mappingAddress = check(properties.getStunServer());
        ref.set(mappingAddress);
        Thread thread = new Thread(this, "mapping-manager");
        thread.start();
    }

    @Override
    public void run() {
        while (true) {
            try {
                MappingAddress mappingAddress = check(properties.getStunServer());
                ref.set(mappingAddress);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            try {
                Thread.sleep(5 * 1000L);
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private MappingAddress check(InetSocketAddress remote) throws Exception {
        StunPacket response = stunClient.sendBind(remote).get();
        int localPort = response.recipient().getPort();
        Map<AttrType, Attr> attrs = response.content().getAttrs();
        AddressAttr changedAddressAttr = (AddressAttr) attrs.get(AttrType.ChangedAddress);
        InetSocketAddress changedAddress = changedAddressAttr.getAddress();
        AddressAttr mappedAddressAttr = (AddressAttr) attrs.get(AttrType.MappedAddress);
        InetSocketAddress mappedAddress1 = mappedAddressAttr.getAddress();
        if (null != (response = testChangeBind(remote, true, true))) {
            return new MappingAddress(localPort, SDWanProtos.MappingTypeCode.FullCone, mappedAddress1);
        } else if (null != (response = testChangeBind(remote, false, true))) {
            return new MappingAddress(localPort, SDWanProtos.MappingTypeCode.RestrictedCone, mappedAddress1);
        }
        response = stunClient.sendBind(changedAddress).get();
        attrs = response.content().getAttrs();
        mappedAddressAttr = (AddressAttr) attrs.get(AttrType.MappedAddress);
        InetSocketAddress mappedAddress2 = mappedAddressAttr.getAddress();
        if (Objects.equals(mappedAddress1, mappedAddress2)) {
            return new MappingAddress(localPort, SDWanProtos.MappingTypeCode.PortRestrictedCone, mappedAddress1);
        } else {
            return new MappingAddress(localPort, SDWanProtos.MappingTypeCode.Symmetric, mappedAddress1);
        }
    }

    private StunPacket testChangeBind(InetSocketAddress address, boolean changeIP, boolean changePort) {
        try {
            StunPacket response = stunClient.sendChangeBind(address, changeIP, changePort).get();
            return response;
        } catch (Exception e) {
            return null;
        }
    }
}
