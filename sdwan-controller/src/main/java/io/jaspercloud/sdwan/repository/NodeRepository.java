package io.jaspercloud.sdwan.repository;

import io.jaspercloud.sdwan.model.Node;
import io.jaspercloud.sdwan.support.NodeType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Component
public class NodeRepository {

    @Resource
    private JdbcTemplate jdbcTemplate;

    private static RowMapper<Node> ROW_MAPPER = new RowMapper<Node>() {
        @Override
        public Node mapRow(ResultSet rs, int rowNum) throws SQLException {
            Node node = new Node();
            node.setId(rs.getLong("id"));
            node.setNodeType(NodeType.valueOf(rs.getInt("node_type")));
            node.setVip(rs.getString("vip"));
            node.setMacAddress(rs.getString("mac_address"));
            node.setRemark(rs.getString("remark"));
            return node;
        }
    };

    public int save(Node node) {
        String sql = "insert into node (node_type, vip, mac_address, remark) values (?,?,?,?)";
        int result = jdbcTemplate.update(sql, node.getNodeType().getCode(), node.getVip(), node.getMacAddress(), node.getRemark());
        return result;
    }

    public int updateById(Node node) {
        String sql = "update node set vip=?, remark=? where id=?";
        int result = jdbcTemplate.update(sql, new Object[]{node.getVip(), node.getRemark(), node.getId()});
        return result;
    }

    public int deleteById(Long id) {
        String sql = "delete from node where id=?";
        int result = jdbcTemplate.update(sql, id);
        return result;
    }

    public List<Node> queryList() {
        String sql = "select * from node order by id desc";
        List<Node> nodeList = jdbcTemplate.query(sql, ROW_MAPPER);
        return nodeList;
    }

    public Node queryById(Long id) {
        String sql = "select * from node where id=?";
        Node node = jdbcTemplate.queryForObject(sql, ROW_MAPPER, new Object[]{id});
        return node;
    }

    public List<Node> queryByIdList(List<Long> idList) {
        if (idList.isEmpty()) {
            throw new IllegalArgumentException("idList empty");
        }
        StringBuilder builder = new StringBuilder("select * from node where id in ");
        builder.append("(");
        for (Long id : idList) {
            builder.append("?").append(",");
        }
        builder.deleteCharAt(builder.length() - 1);
        builder.append(")");
        String sql = builder.toString();
        List<Node> nodeList = jdbcTemplate.query(sql, ROW_MAPPER, idList.toArray());
        return nodeList;
    }

    public Node queryByVip(String vip) {
        String sql = "select * from node where vip=?";
        Node node = jdbcTemplate.queryForObject(sql, ROW_MAPPER, new Object[]{vip});
        return node;
    }

    public Node queryByMacAddress(String macAddress) {
        String sql = "select * from node where mac_address=?";
        Node node = jdbcTemplate.queryForObject(sql, ROW_MAPPER, new Object[]{macAddress});
        return node;
    }

    public List<Node> getMeshNodeList() {
        String sql = "select * from node where node_type=? order by id desc";
        List<Node> nodeList = jdbcTemplate.query(sql, ROW_MAPPER, new Object[]{NodeType.Mesh.getCode()});
        return nodeList;
    }
}
