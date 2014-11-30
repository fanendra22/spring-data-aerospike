# Spring Data Aerospike
[![Build Status](https://github.com/fanendra22/spring-data-aerospike)](https://github.com/fanendra22/spring-data-aerospike)

## README

spring-data-aerospike eases using aerospike db. it provides a mechanism to play with aerospike in form of objects without impacting the performance.

### Installation

To use spring-data-aerospike in your project you need to have spring enabled project. Simplest way 
to use it through maven by adding following dependency

	<groupId>org.springframework.data.aerospike</groupId>
		<artifactId>spring-data-aerospike</artifactId>
	<version>1.0-SNAPSHOT</version>

If you are not using maven you can simply build this project through maven and add generated jar
with required dependencies.

### Usage

Following is a simple example for using spring-data-aerospike.

1. Create an entity class which needs to be persisted in aerospike db

	import java.io.Serializable;
	import java.util.HashMap;
	import java.util.Map;
	import org.springframework.data.aerospike.annotations.AerospikeEntity;
	import org.springframework.data.aerospike.annotations.AerospikeIndex;
	import org.springframework.data.aerospike.annotations.AerospikeKey;
	import com.aerospike.client.query.IndexType;
	@AerospikeEntity(nameSpace = "test", setName = "test")
	public class Entity implements Serializable {
		@AerospikeKey
		private int					id;
		@AerospikeIndex(name = "primaryValue", indexType = IndexType.STRING)
		private String				primaryValue;
		private String				secondaryValue;
		private Map<Long, String>	data	= new HashMap<Long, String>(2);
	
		public int getId() {
			return id;
		}
	
		public void setId(int id) {
			this.id = id;
		}
	
		public String getPrimaryValue() {
			return primaryValue;
		}
	
		public void setPrimaryValue(String primaryValue) {
			this.primaryValue = primaryValue;
		}
	
		public String getSecondaryValue() {
			return secondaryValue;
		}
	
		public void setSecondaryValue(String secondaryValue) {
			this.secondaryValue = secondaryValue;
		}
	
		public Map<Long, String> getData() {
			return data;
		}
	
		public void setData(Map<Long, String> data) {
			this.data = data;
		}
	}
	
3. Create a file with name aerospike.config and put in classpath. It should have at least aerospike db configuration details. Following is the minimal data in needed 

	aerospike.hosts=localhost:3000
	aerospike.user=<user-name>
	aerospike.password=<user-password>
	
If user name and password is not required it can be left blank.
	
4. Create a basic applicationContext.xml file to enable annotation driven spring usage

	<?xml  version="1.0" encoding="UTF-8"?>
	<beans xmlns="http://www.springframework.org/schema/beans"
		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
		xmlns:context="http://www.springframework.org/schema/context"
		xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
	                        http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd">
		<context:annotation-config />
		<context:spring-configured />
		<context:component-scan base-package="org.springframework.data.aerospike" />
	</beans>
	
5. Finally create a Test class

	import java.util.UUID;
	import org.slf4j.Logger;
	import org.slf4j.LoggerFactory;
	import org.springframework.context.ApplicationContext;
	import org.springframework.context.support.ClassPathXmlApplicationContext;
	import org.springframework.data.aerospike.client.Client;
	import org.springframework.data.aerospike.exceptions.AerospikeException;
	import org.springframework.data.aerospike.operations.AerospikeOperations;
	public class Test {
		private static final Logger	LOGGER	= LoggerFactory.getLogger(Test.class);
		public static void main(String[] args) {
			ApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:applicationContext.xml");
			Client client = applicationContext.getBean(Client.class);
			LOGGER.info("Client is connected ? {}", client.getAerospikeClient().isConnected());
			final AerospikeOperations operations = applicationContext.getBean(AerospikeOperations.class);
			Entity entity = new Entity();
			entity.setId(1);
			entity.setPrimaryValue("Primary Value");
			entity.setSecondaryValue("Secondary Value");
			entity.getData().put(Long.valueOf(1), UUID.randomUUID().toString());
			entity.getData().put(Long.valueOf(2), UUID.randomUUID().toString());
			// Put data in db
			try {
				operations.put(entity);
				// Get data through pk
				entity = operations.get(1, Entity.class);
				LOGGER.info("Entity read through primary key: {}", entity);
				// Get data through secondary key
				LOGGER.info("Entity read through secondary key: {}", operations.query(Entity.class, "primaryValue", "Primary Value"));
			} catch (AerospikeException e) {
				LOGGER.error("Error while performing aerospike operation", e);
			} finally {
				
			}
		}
	}

### Notes

For any issues please report at fanendranath.tripathi@gmail.com