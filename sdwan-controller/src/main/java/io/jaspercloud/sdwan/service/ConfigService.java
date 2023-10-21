package io.jaspercloud.sdwan.service;

import io.jaspercloud.sdwan.controller.param.NodeDTO;
import io.jaspercloud.sdwan.controller.param.RouteDTO;

import java.util.List;

public interface ConfigService {

    void saveRoute(RouteDTO request);

    void deleteRoute(Long routeId);

    void updateRoute(RouteDTO route);

    List<RouteDTO> getRouteList();

    void updateNode(NodeDTO request);

    void deleteNode(Long id);

    List<NodeDTO> getNodeList();

    List<NodeDTO> getMeshNodeList();
}
