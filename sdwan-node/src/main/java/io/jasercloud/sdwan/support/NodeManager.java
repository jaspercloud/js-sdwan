package io.jasercloud.sdwan.support;

import io.jasercloud.sdwan.CheckResult;
import io.jasercloud.sdwan.StunClient;
import io.jasercloud.sdwan.StunRule;

import java.net.InetSocketAddress;

public class NodeManager {

    private SDWanNode sdWanNode;
    private StunClient stunClient;

    public NodeManager(SDWanNode sdWanNode, StunClient stunClient) {
        this.sdWanNode = sdWanNode;
        this.stunClient = stunClient;
    }

    public InetSocketAddress getPublicAddress(String vip, String mapping, String filtering) {
        CheckResult selfCheckResult = stunClient.getSelfCheckResult();
        if (StunRule.EndpointIndependent.equals(selfCheckResult.getFiltering())) {

        } else if (StunRule.EndpointIndependent.equals(filtering)) {

        } else if (StunRule.AddressDependent.equals(selfCheckResult.getFiltering())) {

        } else if (StunRule.AddressDependent.equals(filtering)) {

        }
        return null;
    }
}
