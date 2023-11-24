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

@Slf4j
public class WindowsRouteManager extends RouteManager {

    public WindowsRouteManager(SDWanNode sdWanNode, ConnectionManager connectionManager) {
        super(sdWanNode, connectionManager);
    }

    @Override
    protected void addRoute(TunChannel tunChannel, SDWanProtos.Route route) throws Exception {
        TunAddress tunAddress = (TunAddress) tunChannel.localAddress();
        NetworkInterfaceInfo interfaceInfo = NetworkInterfaceUtil.findNetworkInterfaceInfo(tunAddress.getVip());
        String cmd = String.format("route add %s %s if %s", route.getDestination(), tunAddress.getVip(), interfaceInfo.getIndex());
        int code = ProcessUtil.exec(cmd);
        CheckInvoke.check(code, 0);
    }

    @Override
    protected void deleteRoute(TunChannel tunChannel, SDWanProtos.Route route) throws Exception {
        TunAddress tunAddress = (TunAddress) tunChannel.localAddress();
        NetworkInterfaceInfo interfaceInfo = NetworkInterfaceUtil.findNetworkInterfaceInfo(tunAddress.getVip());
        String cmd = String.format("route delete %s %s", route.getDestination(), tunAddress.getVip());
        int code = ProcessUtil.exec(cmd);
        CheckInvoke.check(code, 0);
    }
}
