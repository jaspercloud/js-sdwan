package io.jaspercloud.sdwan.config;

import io.jaspercloud.sdwan.service.NodeManager;
import io.jaspercloud.sdwan.service.SDWanControllerService;
import io.jaspercloud.sdwan.service.SDWanSignalService;
import io.jaspercloud.sdwan.support.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

@EnableConfigurationProperties({
        SDWanControllerProperties.class,
        SDWanRelayProperties.class
})
@Configuration
public class AppConfig {

    @Bean
    public DriverManagerDataSource dataSource(SDWanControllerProperties properties) throws Exception {
        String driver = "org.apache.derby.jdbc.EmbeddedDriver";
        String jdbcUrl = String.format("jdbc:derby:%s;create=true", properties.getDbPath());
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(driver);
        dataSource.setUrl(jdbcUrl);
        DerbyDatabaseInit.init(dataSource, properties.getDbPath(), "META-INF/schema.sql");
        return dataSource;
    }

    @Bean
    public JdbcTemplatePlus jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplatePlus(dataSource);
    }

    @Bean
    public TransactionTemplate transactionTemplate(DataSourceTransactionManager transactionManager) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        return transactionTemplate;
    }

    @Bean
    public NodeManager nodeManager() {
        return new NodeManager();
    }

    @Bean
    public SDWanProcessHandler processHandler(SDWanControllerService controllerService,
                                              SDWanSignalService sdWanSignalService) {
        return new SDWanProcessHandler(controllerService, sdWanSignalService);
    }

    @Bean
    public SDWanControllerServer sdWanController(SDWanControllerProperties properties,
                                                 SDWanProcessHandler processHandler) {
        SDWanControllerServer sdWanControllerServer = new SDWanControllerServer(properties, processHandler);
        return sdWanControllerServer;
    }

    @Bean
    public RelayServer relayServer(SDWanRelayProperties properties) {
        return new RelayServer(properties);
    }
}
