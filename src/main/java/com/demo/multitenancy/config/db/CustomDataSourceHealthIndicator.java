package com.demo.multitenancy.config.db;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.jdbc.DataSourceHealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
/*
 * Why we need this class?
 * Since we are using AbstractRoutingDataSource which basically maintains
 * a map of multiple actual datasources & route the call to the datasource
 * by determining the lookup key first. In our case we have only 1 datasource in resume state
 * & remaining all in suspended state. Due to this when we try to get the health of the
 * application using Mbeans exposed, it'll get stucked beacause it'll try to get the
 * health of all the configured datasources in the application. Since only 1 datasource is in
 * resume state & others are suspended, therefore, it'll failed to get the health of suspended
 * datasources. So to resolve this issue we disabled the health indicator by setting the 
 * property in application.properties file & created our own custom datasource health indicator.
 * We are calling DataSourceHealthIndicator constructor by passing abstract routing datasource 
 * & and our own validation query. Now when we try to get the health it'll run the validation
 * query on the currently active datasource & return the health.
 */
public class CustomDataSourceHealthIndicator {
	
	@Value("${db.health.validation-query}")
	String validationquery;

	@Bean("dbHealthIndicator")
	public HealthIndicator myDbHealthIndicator(@Autowired DataSource dataSource) {
		/*
		 * Datasource health output:
		 * db={status=UP, details={database=PostgreSQL, validationQuery=SELECT 1, result=1}}
		 */
		return new DataSourceHealthIndicator(dataSource, validationquery);
		/*
		 * Below statement is also valid. In this case we are not providing our own
		 * validation query still it's fine.
		 * Datasource health output when we commented out above line & uncommented below line.
		 * db={status=UP, details={database=PostgreSQL, validationQuery=isValid()}}
		 */
		//return new DataSourceHealthIndicator(ds);		
	}
}
