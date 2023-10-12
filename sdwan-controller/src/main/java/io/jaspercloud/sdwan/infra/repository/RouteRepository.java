package io.jaspercloud.sdwan.infra.repository;

import io.jaspercloud.sdwan.domian.Route;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Component
public class RouteRepository {

    @Resource
    private JdbcTemplate jdbcTemplate;

    private RowMapper<Route> ROW_MAPPER = new RowMapper<Route>() {
        @Override
        public Route mapRow(ResultSet rs, int rowNum) throws SQLException {
            Route route = new Route();
            route.setId(rs.getLong("id"));
            route.setDestination(rs.getString("destination"));
            route.setMeshId(rs.getLong("mesh_id"));
            route.setRemark(rs.getString("remark"));
            return route;
        }
    };

    public int save(Route route) {
        String sql = "insert into static_route (destination, mesh_id, remark) values (?,?,?)";
        int result = jdbcTemplate.update(sql, route.getDestination(), route.getMeshId(), route.getRemark());
        return result;
    }

    public int deleteById(Long id) {
        String sql = "delete from static_route where id=?";
        int result = jdbcTemplate.update(sql, id);
        return result;
    }

    public int updateById(Route route) {
        String sql = "update static_route " +
                "set " +
                "destination=?," +
                "mesh_id=?," +
                "remark=?" +
                "where id=?";
        int result = jdbcTemplate.update(sql, route.getDestination(), route.getMeshId(), route.getRemark(), route.getId());
        return result;
    }

    public Route queryById(Long id) {
        String sql = "select * from static_route where id=?";
        Route route = jdbcTemplate.queryForObject(sql, ROW_MAPPER, id);
        return route;
    }

    public List<Route> queryList() {
        String sql = "select * from static_route";
        List<Route> nodeList = jdbcTemplate.query(sql, ROW_MAPPER);
        return nodeList;
    }

    public long countByMeshId(Long meshId) {
        String sql = "select count(mesh_id) from static_route where mesh_id=?";
        long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count;
    }
}
