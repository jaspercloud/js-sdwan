package io.jaspercloud.sdwan.config;

import io.jaspercloud.sdwan.app.SDWanControllerService;
import io.jaspercloud.sdwan.infra.support.NodeManager;
import io.jaspercloud.sdwan.infra.support.SDWanControllerServer;
import io.jaspercloud.sdwan.infra.support.SDWanProcessHandler;
import io.jaspercloud.sdwan.infra.SDWanControllerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

@EnableConfigurationProperties(SDWanControllerProperties.class)
@Configuration
public class AppConfig {

    @Bean
    public DriverManagerDataSource dataSource(SDWanControllerProperties properties) {
        String driver = "org.apache.derby.jdbc.EmbeddedDriver";
        String jdbcUrl = String.format("jdbc:derby:%s;create=true", properties.getDbPath());
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(driver);
        dataSource.setUrl(jdbcUrl);
        return dataSource;
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
    public SDWanProcessHandler processHandler(SDWanControllerService controllerService) {
        return new SDWanProcessHandler(controllerService);
    }

    @Bean
    public SDWanControllerServer sdWanController(SDWanControllerProperties properties,
                                                 SDWanProcessHandler processHandler) {
        SDWanControllerServer sdWanControllerServer = new SDWanControllerServer(properties, processHandler);
        return sdWanControllerServer;
    }
}
