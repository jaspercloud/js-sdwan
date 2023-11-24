package io.jaspercloud.sdwan.node.support.route;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.node.support.connection.ConnectionManager;
import io.jaspercloud.sdwan.node.support.node.SDWanNode;
import io.jaspercloud.sdwan.tun.CheckInvoke;
import io.jaspercloud.sdwan.tun.ProcessUtil;
import io.jaspercloud.sdwan.tun.TunChannel;
import io.jaspercloud.sdwan.tun.osx.OsxTunDevice;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OsxRouteManager extends RouteManager {

    public OsxRouteManager(SDWanNode sdWanNode, ConnectionManager connectionManager) {
        super(sdWanNode, connectionManager);
    }

    @Override
    protected void addRoute(TunChannel tunChannel, SDWanProtos.Route route) throws Exception {
        OsxTunDevice tunDevice = (OsxTunDevice) tunChannel.getTunDevice();
        String ethName = tunDevice.getEthName();
        String cmd = String.format("route -n add -net %s -interface %s", route.getDestination(), ethName);
        int code = ProcessUtil.exec(cmd);
        CheckInvoke.check(code, 0);
    }

    @Override
    protected void deleteRoute(TunChannel tunChannel, SDWanProtos.Route route) throws Exception {
        OsxTunDevice tunDevice = (OsxTunDevice) tunChannel.getTunDevice();
        String ethName = tunDevice.getEthName();
        String cmd = String.format("route -n delete -net %s -interface %s", route.getDestination(), ethName);
        int code = ProcessUtil.exec(cmd);
        CheckInvoke.check(code, 0, 2);
    }
}
