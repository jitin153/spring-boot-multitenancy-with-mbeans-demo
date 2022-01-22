package com.demo.multitenancy.config.db;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.demo.multitenancy.exception.DataSourceRoutingException;

@Component
public class DataSourceContextManager {

	private static final Logger LOG = LoggerFactory.getLogger(DataSourceContextManager.class);

	@Value("${app.datasource.default-lookup-key:primary}")
	private String defaultLookupKey;

	private DataSourceType currentlyActiveDatabaseType;
	private DataSourceType defaultDatabaseType = DataSourceType.PRIMARY;
	private static final ReentrantReadWriteLock R_W_LOCK = new ReentrantReadWriteLock();
	
	@PostConstruct
	public void init() {
		if (StringUtils.hasText(defaultLookupKey)) {
			if (defaultLookupKey.equalsIgnoreCase(DataSourceType.PRIMARY.name())) {
				setDefaultDatabaseType(DataSourceType.PRIMARY);
			} else if (defaultLookupKey.equalsIgnoreCase(DataSourceType.SECONDARY.name())) {
				setDefaultDatabaseType(DataSourceType.SECONDARY);
			} else {
				LOG.warn(
						"Provided value for app.datasource.default-lookup-key property was incorrect. Falling back to default lookup key which is {}.",
						getDefaultLookpKey());
			}
		} else {
			LOG.warn(
					"app.datasource.default-lookup-key property not being set. Falling back to default lookup key which is {}.",
					getDefaultLookpKey());
		}
		setActiveDataSource(getDefaultDatabaseType());
		LOG.info("Currently active datasource lookup key: {}", getCurrentlyActiveLookpKey());
	}

	public void setActiveDataSource(DataSourceType dsType) {
		if(Objects.isNull(dsType)) {
			throw new DataSourceRoutingException("DataSource type cannot be null.");
		}
		try {
			R_W_LOCK.writeLock().lock();
			this.currentlyActiveDatabaseType = dsType;
		}finally {
			R_W_LOCK.writeLock().unlock();
		}
	}

	public DataSourceType getActiveDataSource() {
		try {
			R_W_LOCK.readLock().lock();
			return this.currentlyActiveDatabaseType;
		}finally {
			R_W_LOCK.readLock().unlock();
		}
	}

	public String getCurrentlyActiveLookpKey() {
		return getActiveDataSource().name().toLowerCase();
	}
	
	public DataSourceType getDefaultDatabaseType() {
		return defaultDatabaseType;
	}

	public void setDefaultDatabaseType(DataSourceType defaultDatabaseType) {
		this.defaultDatabaseType = defaultDatabaseType;
	}

	public String getDefaultLookpKey() {
		return getDefaultDatabaseType().name().toLowerCase();
	}

	public void resetBackToDefaultDatabaseType() {
		this.currentlyActiveDatabaseType = this.defaultDatabaseType;
	}
	
	/*private DataSourceType stringToDatabaseType(String key) {
		for (DataSourceType value : DataSourceType.values()) {
			if (value.name().equalsIgnoreCase(key)) {
				return value;
			}
		}
		return null;
	}*/
}
