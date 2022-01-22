package com.demo.multitenancy.mbean;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.demo.multitenancy.config.db.CustomRoutingDatasource;
import com.demo.multitenancy.config.db.DataSourceContextManager;
import com.demo.multitenancy.config.db.DataSourceType;
import com.demo.multitenancy.exception.DataSourceRoutingException;
import com.demo.multitenancy.util.ActiveConnectionChecker;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

@Component
@ManagedResource(objectName = "com.demo.multitenancy:category=MBeans,name=DataSourceMigrationMBean", description = "MBean to switch datasource dynamically.")
public class DataSourceMigrationMBean {

	private static final Logger LOG = LoggerFactory.getLogger(DataSourceMigrationMBean.class);

	private static final String SUCCESS_MSG = "DataSource Migration Successful.";
	private static final String FAILURE_MSG = "DataSource could not migrate. _MSG_";
	private static final String GENERIC_FAILURE_MSG = "DataSource could not migrate. Same datasource is still active.";

	@Value("${app.datasource.active-connections.timeout}")
	private Long timeout;

	@Autowired
	private DataSourceContextManager dataSourceContextManager;

	@Autowired
	private CustomRoutingDatasource dataSource;

	@ManagedOperation
	public String migrateDataSource(String lookupKey) {
		LOG.info("#######--- DATASOURCE MIGRATION PROCESS STARTED :: Lookup Key = {} ---#######", lookupKey);
		if (!isValidLookupKey(lookupKey)) {
			LOG.error("DataSource migration failed. Provided lookup key was invalid.");
			logFailureMsg(lookupKey);
			return FAILURE_MSG.replace("_MSG_", "Provided lookup key was invalid.");
		}
		if (!isSwitchingNeeded(lookupKey)) {
			LOG.warn("DataSource could not migrate. Provided lookup key {} is already active.", lookupKey);
			logFailureMsg(lookupKey);
			return FAILURE_MSG.replace("_MSG_", "Provided lookup key is already active.");
		}
		try {
			LOG.info("Lookup key before migration = {}", dataSourceContextManager.getCurrentlyActiveLookpKey());
			doMigration(lookupKey);
			LOG.info("Lookup key after migration = {}", dataSourceContextManager.getCurrentlyActiveLookpKey());
			LOG.info("#######--- DATASOURCE MIGRATION PROCESS SUCCESSFULLY COMPLETED :: Lookup Key = {} ---#######",
					lookupKey);
			return SUCCESS_MSG;
		} catch (Exception e) {
			logFailureMsg(lookupKey);
			return e.getMessage();
		}
	}

	private boolean isValidLookupKey(String key) {
		return StringUtils.hasText(key) && Objects.nonNull(getDatabaseType(key));
	}

	private void logFailureMsg(String key) {
		LOG.info("#######--- DATASOURCE MIGRATION PROCESS FAILED :: Lookup Key = {} ---#######", key);
	}
	
	private static DataSourceType getDatabaseType(String key) {
		for (DataSourceType value : DataSourceType.values()) {
			if (value.name().equalsIgnoreCase(key)) {
				return value;
			}
		}
		return null;
	}

	private boolean isSwitchingNeeded(String key) {
		return !dataSourceContextManager.getCurrentlyActiveLookpKey().equalsIgnoreCase(key);
	}

	private void doMigration(String key) {
		DataSourceType dataSourceBeforeMigration = dataSourceContextManager.getActiveDataSource();
		HikariDataSource dataSourceToBeSuspended = getHikariDataSource(dataSourceBeforeMigration);
		suspendConnectionPool(dataSourceToBeSuspended);
		DataSourceType requestedDataSource = getDatabaseType(key);
		HikariDataSource dataSourceToBeResumed = getHikariDataSource(requestedDataSource);
		if (resumeConnectionPool(dataSourceToBeResumed)) {
			dataSource.setDefaultTargetDataSource(dataSourceToBeResumed);
			dataSource.afterPropertiesSet();
			dataSourceContextManager.setActiveDataSource(requestedDataSource);
		} else {
			tryResume(dataSourceToBeSuspended);
		}
	}

	private void suspendConnectionPool(HikariDataSource dataSourceToBeSuspended) {
		String poolName = dataSourceToBeSuspended.getPoolName();
		HikariPoolMXBean pool = dataSourceToBeSuspended.getHikariPoolMXBean();
		try {
			pool.softEvictConnections();
			LOG.info("Suspending currently active connection pool '{}'...",poolName);
			pool.suspendPool();
			LOG.info("Currently active connection pool '{}' suspended.",poolName);
		} catch (Exception e) {
			LOG.error("Cound not suspend connection pool '{}'.", poolName);
			throw new DataSourceRoutingException(GENERIC_FAILURE_MSG);
		}
		if (isNoActiveConnection(dataSourceToBeSuspended)) {
			try {
				LOG.info("Softly evicting connections from the suspended connection pool...");
				pool.softEvictConnections();
				LOG.info("Softly evicted connections from the suspended connection pool...");
			} catch (Exception e) {
				LOG.error("Cound not softly evict connections on suspended connection pool '{}'. Trying to resume back the same connection pool...", poolName);
				tryResume(dataSourceToBeSuspended);
			}
		} else {
			LOG.error("Suspending connection pool '{}' timedout. Trying to resume back the same connection pool...", poolName);
			tryResume(dataSourceToBeSuspended);
		}
	}

	private void tryResume(HikariDataSource dataSourceToBeResumed) {
		if (resumeConnectionPool(dataSourceToBeResumed)) {
			LOG.info("Connection pool '{}' resumed back.", dataSourceToBeResumed.getPoolName());
			throw new DataSourceRoutingException(GENERIC_FAILURE_MSG);
		} else {
			LOG.error("Could not resume back connection pool '{}'.", dataSourceToBeResumed.getPoolName());
			throw new DataSourceRoutingException(FAILURE_MSG.replace("_MSG_", "\nCurrently active datasource get suspended but after that something went wrong.\nTried to resume back same datasource but get failed.\nCurrently no datasource is active.\nPlease restart the application."));
		}
	}

	private boolean resumeConnectionPool(HikariDataSource dataSourceToBeResumed) {
		try {
			HikariPoolMXBean pool = dataSourceToBeResumed.getHikariPoolMXBean();
			LOG.info("Resuming the requested connection pool '{}'...", dataSourceToBeResumed.getPoolName());
			pool.resumePool();
			LOG.info("Requested connection pool resumed.");
			return true;
		} catch (Exception e) {
			LOG.error("Could not resume connection pool '{}'.", dataSourceToBeResumed.getPoolName());
		}
		return false;
	}

	private boolean isNoActiveConnection(HikariDataSource dataSource) {
		LOG.info("Checking for the active connections on suspended connection pool. Will wait for the next {} minutes for all the connections to be released.", timeout);
		ActiveConnectionChecker callable = new ActiveConnectionChecker(dataSource);
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<Boolean> future = executor.submit(callable);
		try {
			if (future.get(timeout, TimeUnit.SECONDS)) {
				LOG.info("Released all the active connections on the suspended connection pool.");
				return true;
			}
		} catch (Exception e) {
			LOG.error("Could not release all active connections on the suspended connection pool.");
			callable.setShouldBreakExecution(true);
			future.cancel(true);
		} finally {
			executor.shutdown();
			if (!executor.isShutdown()) {
				executor.shutdownNow();
			}
		}
		return false;
	}

	private HikariDataSource getHikariDataSource(DataSourceType dbType) {
		return (HikariDataSource) dataSource.getResolvedDataSources().get(dbType.name().toLowerCase());
	}

	@ManagedAttribute(description = "Current lookup key")
	public String getCurrentLookupKey() {
		return dataSourceContextManager.getCurrentlyActiveLookpKey();
	}
}
