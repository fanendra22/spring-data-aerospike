/*
 *   Copyright (C) 2014-2015 the original author or authors.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.      
 */
package org.springframework.data.aerospike.client.impl;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.aerospike.client.Client;
import org.springframework.data.aerospike.configuration.AerospikeConfigurations;
import org.springframework.data.aerospike.mapper.EntityMapper;
import org.springframework.data.aerospike.mapper.EntityStructure;
import org.springframework.stereotype.Service;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.policy.WritePolicy;


/**
 * @author fanendra
 * @createdOn 12-Nov-2014
 * @since
 */
@Service("client")
public class ClientImpl implements Client {
	/**
	 * Aerospike client to connect and query aerospike cluster/database.
	 */
	private AerospikeClient	aerospikeClient;
	/**
	 * {@link EntityMapper} instance to hold all the related classes and their
	 * mappings/settings.
	 */
	@Autowired
	private EntityMapper	mapper;
	
	@Autowired
	private AerospikeConfigurations	aerospikeConfigurations;

	@PostConstruct
	public void initialize() {
		aerospikeClient = new AerospikeClient(aerospikeConfigurations.clientPolicy(), aerospikeConfigurations.getAerospikeHosts());
		// Create aerospike client from the settings provided in
		// spring-aerospike.xml
	}
	
	@Override
	public void reloadAerospikePolicies() {
		aerospikeConfigurations.reloadPolicyConfig();
		for(EntityStructure structure : mapper.getAllEntitiesStructure()) {
			structure.setBatchPolicy(aerospikeConfigurations.clazzBatchPolicy(structure.getClazzName()));
			structure.setWritePolicy(aerospikeConfigurations.clazzWritePolicy(structure.getClazzName()));
			structure.setPolicy(aerospikeConfigurations.clazzWritePolicy(structure.getClazzName()));
		}
	}

	@Override
	public AerospikeClient getAerospikeClient() {
		return aerospikeClient;
	}

	@Override
	public EntityMapper getEntityMapper() {
		return mapper;
	}
	
	@Override
	public WritePolicy getDefaultWritePolicy() {
		return aerospikeClient.writePolicyDefault;
	}
}
