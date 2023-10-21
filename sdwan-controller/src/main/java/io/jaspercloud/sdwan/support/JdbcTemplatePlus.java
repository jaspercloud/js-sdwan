package io.jaspercloud.sdwan.support;

import io.jaspercloud.sdwan.exception.TooManyException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.RowMapperResultSetExtractor;
import org.springframework.lang.Nullable;

import javax.sql.DataSource;
import java.util.List;

public class JdbcTemplatePlus extends JdbcTemplate {

    public JdbcTemplatePlus() {
    }

    public JdbcTemplatePlus(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    @Nullable
    public <T> T queryForObject(String sql, RowMapper<T> rowMapper, @Nullable Object... args) throws DataAccessException {
        List<T> results = query(sql, args, new RowMapperResultSetExtractor<>(rowMapper, 1));
        if (results.isEmpty()) {
            return null;
        }
        if (results.size() > 1) {
            throw new TooManyException("Incorrect result size: " + results.size());
        }
        T data = results.get(0);
        return data;
    }
}
