package io.jaspercloud.sdwan.node.node;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.node.config.SDWanNodeProperties;
import io.jaspercloud.sdwan.stun.AddressAttr;
import io.jaspercloud.sdwan.stun.Attr;
import io.jaspercloud.sdwan.stun.AttrType;
import io.jaspercloud.sdwan.stun.MappingAddress;
import io.jaspercloud.sdwan.stun.StunClient;
import io.jaspercloud.sdwan.stun.StunPacket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
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
        MappingAddress mappingAddress = check(properties.getStun().getAddress());
        ref.set(mappingAddress);
        Thread thread = new Thread(this, "mapping-manager");
        thread.start();
    }

    @Override
    public void run() {
        while (true) {
            try {
                MappingAddress mappingAddress = check(properties.getStun().getAddress());
                ref.set(mappingAddress);
            } catch (ExecutionException e) {
                if (e.getCause() instanceof TimeoutException) {
                    log.error("mapping heart timeout");
                } else {
                    log.error(e.getMessage(), e);
                }
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
            try {
                Thread.sleep(5 * 1000L);
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private MappingAddress check(InetSocketAddress remote) throws Exception {
        Long mappingTimeout = properties.getStun().getMappingTimeout();
        StunPacket response = stunClient.sendBind(remote, mappingTimeout).get();
        Map<AttrType, Attr> attrs = response.content().getAttrs();
        AddressAttr changedAddressAttr = (AddressAttr) attrs.get(AttrType.ChangedAddress);
        InetSocketAddress changedAddress = changedAddressAttr.getAddress();
        AddressAttr mappedAddressAttr = (AddressAttr) attrs.get(AttrType.MappedAddress);
        InetSocketAddress mappedAddress1 = mappedAddressAttr.getAddress();
        if (null != (response = testChangeBind(remote, true, true, mappingTimeout))) {
            return new MappingAddress(SDWanProtos.MappingTypeCode.FullCone, mappedAddress1);
        } else if (null != (response = testChangeBind(remote, false, true, mappingTimeout))) {
            return new MappingAddress(SDWanProtos.MappingTypeCode.RestrictedCone, mappedAddress1);
        }
        try {
            response = stunClient.sendBind(changedAddress, mappingTimeout).get();
            attrs = response.content().getAttrs();
            mappedAddressAttr = (AddressAttr) attrs.get(AttrType.MappedAddress);
            InetSocketAddress mappedAddress2 = mappedAddressAttr.getAddress();
            if (Objects.equals(mappedAddress1, mappedAddress2)) {
                return new MappingAddress(SDWanProtos.MappingTypeCode.PortRestrictedCone, mappedAddress1);
            } else {
                return new MappingAddress(SDWanProtos.MappingTypeCode.Symmetric, mappedAddress1);
            }
        } catch (ExecutionException e) {
            if (e.getCause() instanceof TimeoutException) {
                return new MappingAddress(SDWanProtos.MappingTypeCode.Symmetric, mappedAddress1);
            } else {
                throw e;
            }
        }
    }

    private StunPacket testChangeBind(InetSocketAddress address, boolean changeIP, boolean changePort, long timeout) {
        try {
            StunPacket response = stunClient.sendChangeBind(address, changeIP, changePort, timeout).get();
            return response;
        } catch (Throwable e) {
            return null;
        }
    }
}
