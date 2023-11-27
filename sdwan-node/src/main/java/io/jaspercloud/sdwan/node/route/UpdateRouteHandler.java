package io.jaspercloud.sdwan.node.route;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;

@FunctionalInterface
public interface UpdateRouteHandler {

    void onUpdate(SDWanProtos.RouteList routeList);
}
