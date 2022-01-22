package com.demo.multitenancy.util;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

public class ActiveConnectionChecker implements Callable<Boolean> {
	
	private static final Logger LOG = LoggerFactory.getLogger(ActiveConnectionChecker.class);
	
	private boolean shouldBreakExecution = false;
	private HikariDataSource hikariDataSource;

	public ActiveConnectionChecker(HikariDataSource hikariDataSource) {
		this.hikariDataSource = hikariDataSource;
	}

	@Override
	public Boolean call() throws Exception {
		HikariPoolMXBean poolMBean = hikariDataSource.getHikariPoolMXBean();
		while (!Thread.currentThread().isInterrupted()) {
			if (shouldBreakExecution()) {
				break;
			}
			if (poolMBean.getActiveConnections() == 0) {
				LOG.info("There are no active connection on '{}' connection pool.", hikariDataSource.getPoolName());
				return true;
			}
		}
		return false;
	}

	public boolean shouldBreakExecution() {
		return shouldBreakExecution;
	}

	public void setShouldBreakExecution(boolean shouldBreakExecution) {
		this.shouldBreakExecution = shouldBreakExecution;
	}

}
