package com.demo.multitenancy.config.db;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.util.StringUtils;

//@Component
public class CustomRoutingDatasource extends AbstractRoutingDataSource {

	@Autowired
	private DataSourceContextManager dBContextHolder;

	@Override
	protected Object determineCurrentLookupKey() {
		String lookupKey = dBContextHolder.getCurrentlyActiveLookpKey();
		return StringUtils.hasText(lookupKey) ? lookupKey : null;
	}
}
