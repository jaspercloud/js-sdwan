package io.jaspercloud.sdwan.config;

import io.jaspercloud.sdwan.adapter.server.ControlHandler;
import io.jaspercloud.sdwan.adapter.server.ControlServer;
import io.jaspercloud.sdwan.adapter.server.RelayHandler;
import io.jaspercloud.sdwan.adapter.server.RelayServer;
import io.jaspercloud.sdwan.domain.control.service.SDWanControllerService;
import io.jaspercloud.sdwan.domain.control.service.SDWanNodeManager;
import io.jaspercloud.sdwan.domain.control.service.SDWanSignalService;
import io.jaspercloud.sdwan.domain.relay.service.RelayNodeManager;
import io.jaspercloud.sdwan.infra.DerbyDatabaseInit;
import io.jaspercloud.sdwan.infra.JdbcTemplatePlus;
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
    public SDWanNodeManager sdWanNodeManager() {
        return new SDWanNodeManager();
    }

    @Bean
    public RelayNodeManager nodeManager(SDWanRelayProperties properties) {
        return new RelayNodeManager(properties);
    }

    @Bean
    public ControlHandler controlHandler(SDWanControllerService controllerService,
                                         SDWanSignalService sdWanSignalService) {
        return new ControlHandler(controllerService, sdWanSignalService);
    }

    @Bean
    public ControlServer controlServer(SDWanControllerProperties properties,
                                       ControlHandler controlHandler) {
        ControlServer controlServer = new ControlServer(properties, controlHandler);
        return controlServer;
    }

    @Bean
    public RelayHandler relayHandler() {
        return new RelayHandler();
    }

    @Bean
    public RelayServer relayServer(SDWanRelayProperties properties,
                                   RelayHandler relayHandler) {
        return new RelayServer(properties, relayHandler);
    }
}
