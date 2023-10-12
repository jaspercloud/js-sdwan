package io.jaspercloud.sdwan.app;

import java.util.List;

public interface ConfigService {

    void saveRoute(RouteDTO request);

    void deleteRoute(Long routeId);

    void updateRoute(RouteDTO route);

    List<RouteDTO> getRouteList();

    void saveNode(NodeDTO request);

    void deleteNode(Long id);
}
