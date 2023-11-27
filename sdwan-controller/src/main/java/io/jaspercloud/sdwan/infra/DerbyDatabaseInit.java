package io.jaspercloud.sdwan.infra;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.StreamUtils;

import javax.sql.DataSource;
import java.io.File;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DerbyDatabaseInit {

    private DerbyDatabaseInit() {

    }

    public static void init(DataSource dataSource, String dbPath, String sqlFile) throws Exception {
        File dbPathFile = new File(dbPath);
        boolean exists = dbPathFile.exists();
        if (exists) {
            return;
        }
        List<String> sqlList = loadResourceFileSql(sqlFile);
        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                for (String sql : sqlList) {
                    statement.execute(sql);
                }
            }
        }
    }

    private static List<String> loadResourceFileSql(String sqlFile) throws Exception {
        List<String> sqlList = new ArrayList<>();
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(sqlFile)) {
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
