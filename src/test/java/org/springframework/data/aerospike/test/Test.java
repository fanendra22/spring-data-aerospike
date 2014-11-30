/*
 * Copyright (C) 2014-2015 the original author or authors.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.springframework.data.aerospike.test;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.aerospike.client.Client;
import org.springframework.data.aerospike.exceptions.AerospikeException;
import org.springframework.data.aerospike.operations.AerospikeOperations;

/**
 * @author fanendra
 * @createdOn 10-Nov-2014
 * @since
 */
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
