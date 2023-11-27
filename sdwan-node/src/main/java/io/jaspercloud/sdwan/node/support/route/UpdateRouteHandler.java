package io.jaspercloud.sdwan.node.support.route;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;

@FunctionalInterface
public interface UpdateRouteHandler {

    void onUpdate(SDWanProtos.RouteList routeList);
}
