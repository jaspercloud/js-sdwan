package io.jaspercloud.sdwan;

import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DerbyTest {

    public static void main(String[] args) throws Exception {
        String driver = "org.apache.derby.jdbc.EmbeddedDriver";
        String userDir = System.getProperty("user.dir");
        String jdbcUrl = String.format("jdbc:derby:%s/%s;create=true", userDir, "derby.db");
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(driver);
        dataSource.setUrl(jdbcUrl);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.afterPropertiesSet();
        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
        transactionManager.afterPropertiesSet();
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.afterPropertiesSet();
        List<String> sqlList = loadSql("META-INF/schema.sql");
        transactionTemplate.executeWithoutResult(status -> {
            for (String sql : sqlList) {
                jdbcTemplate.execute(sql);
            }
        });
        List<Map<String, Object>> maps = jdbcTemplate.queryForList("select * from users1");
        System.out.println();
    }

    private static List<String> loadSql(String sqlFile) throws Exception {
        List<String> sqlList = new ArrayList<>();
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("META-INF/schema.sql")) {
            byte[] bytes = StreamUtils.copyToByteArray(in);
            String text = new String(bytes);
            String[] split = text.split(";");
            for (String sp : split) {
                String sql = sp.replaceAll("--.*", "").trim();
                if (StringUtils.isNotEmpty(sql)) {
                    sqlList.add(sql);
                }
            }
            return sqlList;
        }
    }
}
