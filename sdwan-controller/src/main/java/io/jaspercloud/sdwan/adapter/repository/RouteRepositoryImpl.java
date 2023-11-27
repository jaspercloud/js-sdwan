package io.jaspercloud.sdwan.adapter.repository;

import io.jaspercloud.sdwan.domain.control.entity.Route;
import io.jaspercloud.sdwan.domain.control.repository.RouteRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class RouteRepositoryImpl implements RouteRepository {

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

    @Override
    public int save(Route route) {
        String sql = "insert into static_route (destination, mesh_id, remark) values (?,?,?)";
        int result = jdbcTemplate.update(sql, route.getDestination(), route.getMeshId(), route.getRemark());
        return result;
    }

    @Override
    public int deleteById(Long id) {
        String sql = "delete from static_route where id=?";
        int result = jdbcTemplate.update(sql, id);
        return result;
    }

    @Override
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

    @Override
    public Route queryById(Long id) {
        String sql = "select * from static_route where id=?";
        Route route = jdbcTemplate.queryForObject(sql, ROW_MAPPER, id);
        return route;
    }

    @Override
    public List<Route> queryList() {
        String sql = "select * from static_route order by id desc";
        List<Route> nodeList = jdbcTemplate.query(sql, ROW_MAPPER);
        return nodeList;
    }

    @Override
    public long countByMeshId(Long meshId) {
        String sql = "select count(mesh_id) from static_route where mesh_id=?";
        long count = jdbcTemplate.queryForObject(sql, Long.class, new Object[]{meshId});
        return count;
    }
}
