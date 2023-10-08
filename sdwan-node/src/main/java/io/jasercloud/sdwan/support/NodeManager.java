package io.jasercloud.sdwan.support;

import io.jasercloud.sdwan.*;
import io.jasercloud.sdwan.tun.IpPacket;
import io.jaspercloud.sdwan.AsyncTask;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessException;

import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class NodeManager {

    private SDWanNode sdWanNode;
    private StunClient stunClient;

    public NodeManager(SDWanNode sdWanNode, StunClient stunClient) {
        this.sdWanNode = sdWanNode;
        this.stunClient = stunClient;
    }

    public InetSocketAddress getPublicAddress(SDWanProtos.SDArpResp sdArpResp) {
        try {
            String vip = sdArpResp.getVip();
            String stunMapping = sdArpResp.getStunMapping();
            String stunFiltering = sdArpResp.getStunFiltering();
            CheckResult self = stunClient.getSelfCheckResult();
            InetSocketAddress address = self.getMappingAddress();
            if (StunRule.EndpointIndependent.equals(self.getFiltering())
                    && StunRule.EndpointIndependent.equals(stunFiltering)) {
                InetSocketAddress resp = new InetSocketAddress(sdArpResp.getPublicIP(), sdArpResp.getPublicPort());
                return resp;
            } else if (StunRule.EndpointIndependent.equals(self.getFiltering())) {
                String tranId = UUID.randomUUID().toString();
                CompletableFuture<StunPacket> waitTask = AsyncTask.waitTask(tranId, 3000);
                sdWanNode.punching(address.getHostString(), address.getPort(), vip, tranId);
                InetSocketAddress resp = waitTask.get().recipient();
                return resp;
            } else if (StunRule.EndpointIndependent.equals(stunFiltering)) {
                InetSocketAddress target = new InetSocketAddress(sdArpResp.getPublicIP(), sdArpResp.getPublicPort());
                StunPacket stunPacket = stunClient.sendBind(target);
                AddressAttr mappedAddress = (AddressAttr) stunPacket.content().getAttrs().get(AttrType.MappedAddress);
                InetSocketAddress resp = new InetSocketAddress(mappedAddress.getIp(), mappedAddress.getPort());
                return resp;
            } else if (StunRule.AddressDependent.equals(self.getFiltering())) {

            } else if (StunRule.AddressDependent.equals(stunFiltering)) {

            }
            return null;
        } catch (Exception e) {
            throw new ProcessException(e.getMessage(), e);
        }
    }
}
