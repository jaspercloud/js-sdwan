package io.jaspercloud.sdwan.domain.control.repository;

import io.jaspercloud.sdwan.domain.control.entity.Node;

import java.util.List;

public interface NodeRepository {

    int save(Node node);

    int updateById(Node node);

    int deleteById(Long id);

    List<Node> queryList();

    Node queryById(Long id);

    List<Node> queryByIdList(List<Long> idList);

    Node queryByVip(String vip);

    Node queryByMacAddress(String macAddress);

    List<Node> getMeshNodeList();

}
