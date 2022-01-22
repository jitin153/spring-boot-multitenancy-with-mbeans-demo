package com.demo.multitenancy.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.demo.multitenancy.config.db.CustomRoutingDatasource;
import com.demo.multitenancy.config.db.DataSourceContextManager;
import com.demo.multitenancy.config.db.DataSourceType;
import com.demo.multitenancy.config.db.DatasourceConfig;
import com.demo.multitenancy.exception.DataSourceRoutingException;
import com.demo.multitenancy.model.Student;
import com.demo.multitenancy.rowmapper.StudentRowMapper;
import com.demo.multitenancy.util.Queries;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

@RestController
public class StudentController {

	private static final Logger LOG = LoggerFactory.getLogger(StudentController.class);

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DataSourceContextManager dataSourceContextManager;

	@PostMapping("/student")
	@Transactional
	public Student save(@RequestBody Student student) {
		LOG.info("### -- SAVE CALLED -- ####");
		student.setName(student.getName() + "-" + getRandomNumber());
		long sid = runInsert(Queries.INSERT_STUDENT, student.getName());
		if (sid > 0) {
			student.setId(sid);
			student.getAddress().setStudentId(sid);
			long addressId = runInsert(Queries.INSERT_ADDRESS, sid, student.getAddress().getAddress());
			if (addressId > 0) {
				student.getAddress().setId(addressId);
				LOG.info("Successfully saved student {}[{}] with address {}[{}] - Active DataSource: {}", student.getName(),
						student.getId(), student.getAddress().getAddress(), student.getAddress().getId(),
						dataSourceContextManager.getCurrentlyActiveLookpKey());
				return student;
			} else {
				throw new DataSourceRoutingException("Something went wrong while saving address for the student "
						+ student.getName() + "[" + sid + "] into database.");
			}
		} else {
			throw new DataSourceRoutingException(
					"Something went wrong while saving student " + student.getName() + " into database.");
		}

	}

	private int getRandomNumber() {
		int min = 100000;
		int max = 999999;
		return (int) ((Math.random() * (max - min)) + min);
	}

	@GetMapping("/student")
	public List<Student> findAll() {
		LOG.info("### -- FIND ALL CALLED -- ####");
		List<Student> result = jdbcTemplate.query(Queries.FIND_ALL, new StudentRowMapper());
		LOG.info("Number of student in the table = {} - Active DataSource: {}", result.size(),
				dataSourceContextManager.getCurrentlyActiveLookpKey());
		return result;
	}

	@GetMapping("/student/{id}")
	public Student findById(@PathVariable Long id) {
		Student student = new Student();
		try {
			LOG.info("### -- FIND BY ID CALLED -- ####");
			student = jdbcTemplate.queryForObject(Queries.FIND_BY_ID, new Object[] { id }, new StudentRowMapper());
			LOG.info("Record found - Active DataSource: {}", dataSourceContextManager.getCurrentlyActiveLookpKey());
		} catch (Exception e) {
			LOG.error(e.getMessage());
		}
		return student;
	}

	@GetMapping("/count")
	public String count() {
		try {
			LOG.info("### -- COUNT CALLED -- ####");
			long totalStudents = jdbcTemplate.queryForObject(Queries.COUNT_STUDENT_RECORDS, Long.class);
			long totalAddresses = jdbcTemplate.queryForObject(Queries.COUNT_ADDRESS_RECORDS, Long.class);
			LOG.info("Total students = {}, Total addresses = {} - Active DataSource: {}", totalStudents, totalAddresses,
					dataSourceContextManager.getCurrentlyActiveLookpKey());
			return "SUCCESS: Total students = " + totalStudents + ", Total addresses = " + totalAddresses;
		} catch (Exception e) {
			LOG.error(e.getMessage());
		}
		return "FAILED: Total students = -1, Total addresses = -1";
	}

	@GetMapping("/reset")
	public String resetEverything() {
		try {
			jdbcTemplate.update(Queries.DROP_ADDRESS);
			jdbcTemplate.update(Queries.DROP_STUDENT);
			jdbcTemplate.update(Queries.CREATE_STUDENT);
			jdbcTemplate.update(Queries.CREATE_ADDRESS);
			return "SUCCESS";
		} catch (Exception e) {
			LOG.error(e.getMessage());
			return "FAILED";
		}
	}

	private long runInsert(String query, Object... args) {
		KeyHolder keyHolder = new GeneratedKeyHolder();
		int rowUpdated = jdbcTemplate.update(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
				PreparedStatement ps = connection.prepareStatement(query, new String[] { "id" });
				if (args.length == 1) {
					ps.setString(1, (String) (args[0]));
				}
				if (args.length == 2) {
					ps.setLong(1, (long) args[0]);
					ps.setString(2, (String) args[1]);
				}
				return ps;
			}
		}, keyHolder);
		return rowUpdated >= 1 ? keyHolder.getKey().longValue() : rowUpdated;
	}
	
	/*@PutMapping("/student")
	public String updateStudent(@RequestBody Student student) {
		LOG.info("### -- UPDATE CALLED -- ####");
		int r = jdbcTemplate.update(Queries.UPDATE_STUDENT, student.getName());
		if(r > 0) {
			LOG.info("{} RECORDS UPDATED SUCCESSFULLY FOR NAME {} - Active DataSource: {} , Pool: {}", r, student.getName(), dataSourceContextManager.getCurrentlyActiveLookpKey(), getActivePoolName());
			return "SUCCESS";
		}else {
			LOG.info("{} RECORDS UPDATED SUCCESSFULLY FOR NAME {} - Active DataSource: {}, Pool: {}",r , student.getName(), dataSourceContextManager.getCurrentlyActiveLookpKey(), getActivePoolName());
		}
		return "FAILED";
	}*/
}
