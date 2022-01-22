package com.demo.multitenancy.util;

public class Queries {
	private Queries() {}
	
	public static final String FIND_BY_ID = "select s.id, s.name, a.id as address_id, a.sid, a.address from student s join address a on s.id = a.sid where s.id = ?";
	public static final String FIND_ALL = "select s.id, s.name, a.id as address_id, a.sid, a.address from student s join address a on s.id = a.sid";
	public static final String INSERT_STUDENT = "insert into student(name) values(?)";
	public static final String INSERT_ADDRESS = "insert into address(sid, address) values(?, ?)";
	public static final String COUNT_STUDENT_RECORDS = "select count(1) as total from student";
	public static final String COUNT_ADDRESS_RECORDS = "select count(1) as total from address";
	//public static final String UPDATE_STUDENT = "update student set name = ?";
	public static final String DROP_STUDENT = "drop table IF EXISTS student";
	public static final String DROP_ADDRESS = "drop table IF EXISTS address";
	public static final String CREATE_STUDENT = "CREATE TABLE student (id INT primary KEY GENERATED ALWAYS AS IDENTITY, name VARCHAR NOT NULL)";
	public static final String CREATE_ADDRESS = "CREATE TABLE address (id INT GENERATED ALWAYS AS IDENTITY, sid INT NOT NULL, address VARCHAR, FOREIGN KEY (sid) REFERENCES student (id))";
}
