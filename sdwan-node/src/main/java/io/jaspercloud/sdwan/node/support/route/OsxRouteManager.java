package io.jaspercloud.sdwan.node.support.route;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.node.support.SDWanNode;
import io.jaspercloud.sdwan.node.support.tunnel.TunnelManager;
import io.jaspercloud.sdwan.tun.CheckInvoke;
import io.jaspercloud.sdwan.tun.ProcessUtil;
import io.jaspercloud.sdwan.tun.TunChannel;
import io.jaspercloud.sdwan.tun.osx.OsxTunDevice;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class OsxRouteManager extends RouteManager {

    public OsxRouteManager(SDWanNode sdWanNode, TunnelManager tunnelManager) {
        super(sdWanNode, tunnelManager);
    }

    @Override
    protected void doUpdateRouteList(TunChannel tunChannel, List<SDWanProtos.Route> oldList, List<SDWanProtos.Route> newList) throws Exception {
        OsxTunDevice tunDevice = (OsxTunDevice) tunChannel.getTunDevice();
        String ethName = tunDevice.getEthName();
        for (SDWanProtos.Route route : oldList) {
            String cmd = String.format("route -n delete -net %s -interface %s", route.getDestination(), ethName);
            int code = ProcessUtil.exec(cmd);
            CheckInvoke.check(code, 0, 2);
        }
        for (SDWanProtos.Route route : newList) {
            String cmd = String.format("route -n add -net %s -interface %s", route.getDestination(), ethName);
            int code = ProcessUtil.exec(cmd);
            CheckInvoke.check(code, 0);
        }
    }

    @Override
    public void releaseRoute(TunChannel tunChannel, List<SDWanProtos.Route> routeList) throws Exception {
        OsxTunDevice tunDevice = (OsxTunDevice) tunChannel.getTunDevice();
        String ethName = tunDevice.getEthName();
        for (SDWanProtos.Route route : routeList) {
            String cmd = String.format("route -n delete -net %s -interface %s", route.getDestination(), ethName);
            int code = ProcessUtil.exec(cmd);
            CheckInvoke.check(code, 0, 2);
        }
    }
}
