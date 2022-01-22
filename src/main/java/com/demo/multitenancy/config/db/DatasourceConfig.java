package com.demo.multitenancy.config.db;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

@Configuration
@ConfigurationProperties(prefix = "app.datasource")
@PropertySource({ "classpath:dbcp-config.properties" })
public class DatasourceConfig {
	private static final Logger LOG = LoggerFactory.getLogger(DatasourceConfig.class);

	private Map<String, DbConfigProperties> config;

	public Map<String, DbConfigProperties> getConfig() {
		return config;
	}

	public void setConfig(Map<String, DbConfigProperties> config) {
		this.config = config;
	}

	@ConfigurationProperties(prefix = "app.datasource")
	@Bean
	public HikariConfig hikariConfig() {
		return new HikariConfig();
	}

	@Bean("primary")
	public DataSource hikariDataSourcePrimary() {
		DbConfigProperties value = config.get("primary");
		HikariConfig hikariConfig = hikariConfig();
		hikariConfig.setPoolName("Pool-Primary");
		hikariConfig.setDriverClassName(value.getDriver());
		hikariConfig.setJdbcUrl(value.getUrl());
		hikariConfig.setUsername(value.getUsername());
		hikariConfig.setPassword(value.getPassword());
		return new HikariDataSource(hikariConfig);
	}

	@Bean("secondary")
	public DataSource hikariDataSourceSecondary() {
		DbConfigProperties value = config.get("secondary");
		HikariConfig hikariConfig = hikariConfig();
		hikariConfig.setPoolName("Pool-Secondary");
		hikariConfig.setDriverClassName(value.getDriver());
		hikariConfig.setJdbcUrl(value.getUrl());
		hikariConfig.setUsername(value.getUsername());
		hikariConfig.setPassword(value.getPassword());
		return new HikariDataSource(hikariConfig);
	}

	public Map<Object, Object> getTargetDataSources() {
		Map<Object, Object> targetDatasources = new HashMap<>();
		targetDatasources.put(DataSourceType.PRIMARY.name().toLowerCase(), hikariDataSourcePrimary());
		targetDatasources.put(DataSourceType.SECONDARY.name().toLowerCase(), hikariDataSourceSecondary());
		return targetDatasources;
	}

	@Bean
	@Primary
	public DataSource dataSource(DataSourceContextManager dataSourceContextManager) {
		CustomRoutingDatasource customRoutingDatasource = new CustomRoutingDatasource();
		customRoutingDatasource.setTargetDataSources(getTargetDataSources());
		customRoutingDatasource
				.setDefaultTargetDataSource(getTargetDataSources().get(dataSourceContextManager.getDefaultLookpKey()));
		/*
		 * Suspend the datasource which is not being used as of now.
		 */
		DataSourceType dataSourceTypeToBeSuspended = dataSourceContextManager.getActiveDataSource()
				.equals(DataSourceType.PRIMARY) ? DataSourceType.SECONDARY : DataSourceType.PRIMARY;
		HikariDataSource dataSourceToSuspended = (HikariDataSource) getTargetDataSources()
				.get(dataSourceTypeToBeSuspended.name().toLowerCase());
		suspendConnectionPool(dataSourceToSuspended.getHikariPoolMXBean());

		return customRoutingDatasource;
	}

	private void suspendConnectionPool(HikariPoolMXBean hikariPool) {
		try {
			hikariPool.softEvictConnections();
			hikariPool.suspendPool();
			hikariPool.softEvictConnections();
		} catch (Exception e) {
			LOG.error("Error occurred while suspending the connection pool. Error - {}", e.getMessage());
		}
	}
}
