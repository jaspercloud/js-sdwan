package io.jaspercloud.sdwan.node.support.route;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;

import java.util.List;

@FunctionalInterface
public interface UpdateRouteHandler {

    void onUpdate(List<SDWanProtos.Route> routeList);
}
