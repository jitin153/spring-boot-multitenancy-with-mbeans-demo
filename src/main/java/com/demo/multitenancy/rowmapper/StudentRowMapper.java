package com.demo.multitenancy.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import com.demo.multitenancy.model.Address;
import com.demo.multitenancy.model.Student;

public class StudentRowMapper implements RowMapper<Student> {

	@Override
	public Student mapRow(ResultSet rs, int rowNum) throws SQLException {
		Student student = new Student();
		student.setId(rs.getLong("ID"));
		student.setName(rs.getString("NAME"));
		
		Address address = new Address();
		address.setId(rs.getLong("ADDRESS_ID"));
		address.setStudentId(rs.getLong("SID"));
		address.setAddress(rs.getString("ADDRESS"));
		
		student.setAddress(address);	
		
		return student;
	}
}
