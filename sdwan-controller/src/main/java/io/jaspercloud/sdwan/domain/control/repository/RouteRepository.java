package io.jaspercloud.sdwan.domain.control.repository;

import io.jaspercloud.sdwan.domain.control.entity.Route;

import java.util.List;

public interface RouteRepository {

    int save(Route route);

    int deleteById(Long id);

    int updateById(Route route);

    Route queryById(Long id);

    List<Route> queryList();

    long countByMeshId(Long meshId);

}
