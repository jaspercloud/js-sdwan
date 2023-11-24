package io.jaspercloud.sdwan.node.support.route;

import io.jaspercloud.sdwan.NetworkInterfaceInfo;
import io.jaspercloud.sdwan.NetworkInterfaceUtil;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.node.support.connection.ConnectionManager;
import io.jaspercloud.sdwan.node.support.node.SDWanNode;
import io.jaspercloud.sdwan.tun.CheckInvoke;
import io.jaspercloud.sdwan.tun.ProcessUtil;
import io.jaspercloud.sdwan.tun.TunAddress;
import io.jaspercloud.sdwan.tun.TunChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class LinuxRouteManager extends RouteManager {

    public LinuxRouteManager(SDWanNode sdWanNode, ConnectionManager connectionManager) {
        super(sdWanNode, connectionManager);
    }

    @Override
    protected void doUpdateRouteList(TunChannel tunChannel, List<SDWanProtos.Route> oldList, List<SDWanProtos.Route> newList) throws Exception {
        TunAddress tunAddress = (TunAddress) tunChannel.localAddress();
        NetworkInterfaceInfo interfaceInfo = NetworkInterfaceUtil.findNetworkInterfaceInfo(tunAddress.getVip());
        for (SDWanProtos.Route route : oldList) {
            String cmd = String.format("ip route delete %s via %s", route.getDestination(), tunAddress.getVip());
            int code = ProcessUtil.exec(cmd);
            CheckInvoke.check(code, 0, 2);
        }
        for (SDWanProtos.Route route : newList) {
            String cmd = String.format("ip route add %s via %s dev %s", route.getDestination(), tunAddress.getVip(), interfaceInfo.getName());
            int code = ProcessUtil.exec(cmd);
            CheckInvoke.check(code, 0);
        }
    }
}
