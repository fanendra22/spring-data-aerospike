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
package org.springframework.data.aerospike.configuration.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.aerospike.configuration.AerospikeConfigurations;
import org.springframework.data.aerospike.exceptions.PropertyNotFoundException;
import org.springframework.stereotype.Service;

import com.aerospike.client.Host;
import com.aerospike.client.policy.BatchPolicy;
import com.aerospike.client.policy.ClientPolicy;
import com.aerospike.client.policy.GenerationPolicy;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.Priority;
import com.aerospike.client.policy.RecordExistsAction;
import com.aerospike.client.policy.WritePolicy;

/**
 * @author fanendra
 * @createdOn 12-Nov-2014
 * @since
 */
@Service("policyConfigurations")
@SuppressWarnings("rawtypes")
public class AerospikeConfigurationsImpl implements AerospikeConfigurations {
	private static final Logger	LOGGER							= LoggerFactory.getLogger(AerospikeConfigurationsImpl.class);
	private static final int	ERROR_INT_VALUE					= -1001;
	private static final String	ERROR_STRING_VALUE				= "ERROR";
	private static final Enum	ERROR_ENUM_VALUE				= null;
	private static final String	READ_POLICY						= "readPolicy.";
	private static final String	WRITE_POLICY					= "writePolicy.";
	private static final String	BATCH_POLICY					= "batchPolicy.";

	private static final String	AEROSPIKE_HOSTS					= "aerospike.hosts";
	private static final String	AEROSPIKE_USER					= "aerospike.user";
	private static final String	AEROSPIKE_PASSWORD				= "aerospike.password";
	private static final String	AEROSPIKE_TIMEOUT				= "aerospike.timeout";
	private static final String	AEROSPIKE_MAX_THREAD			= "aerospike.maxThreads";
	private static final String	AEROSPIKE_MAX_SOCKETS_IDLE		= "aerospike.maxSocketIdle";
	private static final String	AEROSPIKE_TEND_INTERVAL			= "aerospike.tendInterval";
	private static final String	AEROSPIKE_FAIL_IF_NOT_CONNECTED	= "aerospike.failIfNotConnected";
	private static final String	AEROSPIKE_SHARED_THREAD_POOL	= "aerospike.sharedThreadPool";

	private final Properties	configProperties				= new Properties();

	private String				configFile = "aerospike.config";

	public void setConfigFile(String configFile) {
		this.configFile = configFile;
	}

	@PostConstruct
	public void loadConfig() {
		loadFile();
	}

	/**
	 * Refreshes the configurations in {@link #configProperties} from the file.
	 */
	private void loadFile() {
		try {
			LOGGER.info("Reading aerospike configurations from file: {}", configFile);
			InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(configFile);
			configProperties.load(in);
		} catch (FileNotFoundException e) {
			LOGGER.error("Failed to read aerospike.config file. Default settings will be used", e);
		} catch (IOException e) {
			LOGGER.error("Failed to read aerospike.config file. Default settings will be used", e);
		}
	}

	/**
	 * Reloads file again to refresh {@link #configProperties}.
	 */
	public void reloadPolicyConfig() {
		loadFile();
	}

	/**
	 * 
	 * @return
	 */
	public ClientPolicy clientPolicy() {
		ClientPolicy clientPolicy = new ClientPolicy();
		clientPolicy.readPolicyDefault = readPolicy();
		clientPolicy.writePolicyDefault = writePolicy();
		clientPolicy.batchPolicyDefault = batchPolicy();
		//
		clientPolicy.failIfNotConnected = readBooleanProperty(AEROSPIKE_FAIL_IF_NOT_CONNECTED);
		clientPolicy.sharedThreadPool = readBooleanProperty(AEROSPIKE_SHARED_THREAD_POOL);
		//
		int maxSocketIdle = readIntegerProperty(AEROSPIKE_MAX_SOCKETS_IDLE);
		clientPolicy.maxSocketIdle = maxSocketIdle != ERROR_INT_VALUE ? maxSocketIdle : clientPolicy.maxSocketIdle;
		//
		int maxThreads = readIntegerProperty(AEROSPIKE_MAX_THREAD);
		clientPolicy.maxThreads = maxThreads != ERROR_INT_VALUE ? maxThreads : clientPolicy.maxThreads;
		//
		int tendInterval = readIntegerProperty(AEROSPIKE_TEND_INTERVAL);
		clientPolicy.tendInterval = tendInterval != ERROR_INT_VALUE ? tendInterval : clientPolicy.tendInterval;

		int timeout = readIntegerProperty(AEROSPIKE_TIMEOUT);
		clientPolicy.timeout = timeout != ERROR_INT_VALUE ? timeout : clientPolicy.timeout;

		String userName = readTextProperty(AEROSPIKE_USER);
		clientPolicy.user = userName != ERROR_STRING_VALUE ? userName : clientPolicy.user;
		if (clientPolicy.user != null) {
			String password = readTextProperty(AEROSPIKE_PASSWORD);
			clientPolicy.password = password != ERROR_STRING_VALUE ? password : clientPolicy.password;
		}
		return clientPolicy;
	}

	public Host[] getAerospikeHosts() {
		String[] hostsCluster = readTextProperty(AEROSPIKE_HOSTS).split(",");
		Host[] hosts = new Host[hostsCluster.length];
		for (int i = 0; i < hostsCluster.length; i++) {
			String[] nodeConfig = hostsCluster[i].split(":");
			hosts[i] = new Host(nodeConfig[0], Integer.parseInt(nodeConfig[1]));
		}
		return hosts;
	}

	/**
	 * 
	 * @param clazzName
	 * @return
	 */
	public Policy clazzReadPolicy(String clazzName) {
		return readPolicy(clazzName);
	}

	/**
	 * 
	 * @return
	 */
	public Policy readPolicy() {
		return readPolicy("");
	}

	/**
	 * Reads and processes Read {@link Policy} from the configuration file.
	 * 
	 * @return
	 */
	private Policy readPolicy(String clazzName) {
		String className = clazzName.isEmpty() ? "" : clazzName + ".";
		Policy policy = new Policy();
		// Read max retries
		int maxRetries = readIntegerProperty(className + READ_POLICY + "maxRetries");
		policy.maxRetries = maxRetries != ERROR_INT_VALUE ? maxRetries : policy.maxRetries;

		// Read priority
		Enum priority = readEnumProperty(className + READ_POLICY + "priority", Priority.DEFAULT);
		policy.priority = priority != ERROR_ENUM_VALUE ? (Priority) priority : policy.priority;

		// Read timeout
		int timeout = readIntegerProperty(className + READ_POLICY + "timeout");
		policy.timeout = timeout != ERROR_INT_VALUE ? timeout : policy.timeout;

		// Read sleep between retires
		int sleepBetweenRetries = readIntegerProperty(className + READ_POLICY + "sleepBetweenRetries");
		policy.sleepBetweenRetries = sleepBetweenRetries != ERROR_INT_VALUE ? sleepBetweenRetries : policy.sleepBetweenRetries;
		return policy;
	}

	/**
	 * 
	 * @param clazzName
	 * @return
	 */
	public WritePolicy clazzWritePolicy(String clazzName) {
		return writePolicy(clazzName);
	}

	/**
	 * 
	 * @return
	 */
	public WritePolicy writePolicy() {
		return writePolicy("");
	}

	/**
	 * 
	 * @param clazzName
	 * @return
	 */
	private WritePolicy writePolicy(String clazzName) {
		String className = clazzName.isEmpty() ? "" : clazzName + ".";
		WritePolicy writePolicy = new WritePolicy();
		// Read expiration time of the entity
		int expiration = readIntegerProperty(className + WRITE_POLICY + "expiration");
		writePolicy.expiration = expiration != ERROR_INT_VALUE ? expiration : writePolicy.expiration;
		// Read generation policy
		GenerationPolicy generationPolicy = (GenerationPolicy) readEnumProperty(className + WRITE_POLICY + "generationPolicy", GenerationPolicy.NONE);
		writePolicy.generationPolicy = generationPolicy != ERROR_ENUM_VALUE ? generationPolicy : writePolicy.generationPolicy;
		// Read generation
		int generation = readIntegerProperty(className + WRITE_POLICY + "generation");
		writePolicy.generation = generation != ERROR_INT_VALUE ? generation : writePolicy.generation;
		// Read max retries count.
		int maxRetries = readIntegerProperty(className + WRITE_POLICY + "maxRetries");
		writePolicy.maxRetries = maxRetries != ERROR_INT_VALUE ? maxRetries : writePolicy.maxRetries;
		// Read priority
		Priority priority = (Priority) readEnumProperty(className + WRITE_POLICY + "priority", Priority.DEFAULT);
		writePolicy.priority = priority != ERROR_ENUM_VALUE ? priority : writePolicy.priority;
		// Read action to be taken on already existing record
		RecordExistsAction recordExistsAction = (RecordExistsAction) readEnumProperty(className + WRITE_POLICY + "recordExistsAction",
				RecordExistsAction.CREATE_ONLY);
		writePolicy.recordExistsAction = recordExistsAction != ERROR_ENUM_VALUE ? recordExistsAction : writePolicy.recordExistsAction;
		// Sleep in millis between each retry operation
		int sleepBetweenRetries = readIntegerProperty(className + WRITE_POLICY + "sleepBetweenRetries");
		writePolicy.sleepBetweenRetries = sleepBetweenRetries != ERROR_INT_VALUE ? sleepBetweenRetries : writePolicy.sleepBetweenRetries;
		// Timeout for the operation
		int timeout = readIntegerProperty(className + WRITE_POLICY + "timeout");
		writePolicy.timeout = timeout != ERROR_INT_VALUE ? timeout : writePolicy.timeout;
		// Whether actual keys will be sent to aerospike db
		writePolicy.sendKey = readBooleanProperty(className + WRITE_POLICY + "sendKey");
		return writePolicy;
	}

	/**
	 * 
	 * @param clazzName
	 * @return
	 */
	public BatchPolicy clazzBatchPolicy(String clazzName) {
		return batchPolicy(clazzName);
	}

	/**
	 * 
	 * @return
	 */
	public BatchPolicy batchPolicy() {
		return batchPolicy("");
	}

	/**
	 * 
	 * @param clazzName
	 * @return
	 */
	private BatchPolicy batchPolicy(String clazzName) {
		String className = clazzName.isEmpty() ? "" : clazzName + ".";
		BatchPolicy policy = new BatchPolicy();
		// Read max retries
		int maxRetries = readIntegerProperty(className + BATCH_POLICY + "maxRetries");
		policy.maxRetries = maxRetries != ERROR_INT_VALUE ? maxRetries : policy.maxRetries;

		// Read priority
		Enum priority = readEnumProperty(className + BATCH_POLICY + "priority", Priority.DEFAULT);
		policy.priority = priority != ERROR_ENUM_VALUE ? (Priority) priority : policy.priority;

		// Read timeout
		int timeout = readIntegerProperty(className + BATCH_POLICY + "timeout");
		policy.timeout = timeout != ERROR_INT_VALUE ? timeout : policy.timeout;

		// Read sleep between retires
		int sleepBetweenRetries = readIntegerProperty(className + BATCH_POLICY + "sleepBetweenRetries");
		policy.sleepBetweenRetries = sleepBetweenRetries != ERROR_INT_VALUE ? sleepBetweenRetries : policy.sleepBetweenRetries;

		int maxConcurrentThreads = readIntegerProperty(className + BATCH_POLICY + "maxConcurrentThreads");
		policy.maxConcurrentThreads = maxConcurrentThreads != ERROR_INT_VALUE ? maxConcurrentThreads : policy.maxConcurrentThreads;
		return policy;
	}

	/**
	 * 
	 * @param propertyName
	 * @param type
	 * @return
	 */
	private Enum readEnumProperty(String propertyName, Enum type) {
		try {
			checkPropertyExists(propertyName);
			if (type instanceof RecordExistsAction) {
				return RecordExistsAction.valueOf(configProperties.getProperty(propertyName));
			} else if (type instanceof GenerationPolicy) {
				return GenerationPolicy.valueOf(configProperties.getProperty(propertyName));
			} else if (type instanceof Priority) {
				return Priority.valueOf(configProperties.getProperty(propertyName));
			}
		} catch (Exception e) {
			LOGGER.warn("Failed to parse property: {}. Error was: {}", propertyName, e.getMessage());
		}
		return null;
	}

	/**
	 * 
	 * @param propertyName
	 * @return
	 * @throws PropertyNotFoundException
	 */
	private String readTextProperty(String propertyName) {
		try {
			checkPropertyExists(propertyName);
			return configProperties.getProperty(propertyName);
		} catch (Exception e) {
			LOGGER.warn("Failed to parse property: {}. Error was: {}", propertyName, e.getMessage());
		}
		return ERROR_STRING_VALUE;
	}

	/**
	 * 
	 * @param propertyName
	 * @return
	 * @throws PropertyNotFoundException
	 */
	private int readIntegerProperty(String propertyName) {
		try {
			checkPropertyExists(propertyName);
			return Integer.parseInt(configProperties.getProperty(propertyName));
		} catch (Exception e) {
			LOGGER.warn("Failed to parse property: {}. Error was: {}", propertyName, e.getMessage());
		}
		return -1001;
	}

	/**
	 * 
	 * @param propertyName
	 * @return
	 */
	private boolean readBooleanProperty(String propertyName) {
		try {
			checkPropertyExists(propertyName);
			return Boolean.valueOf(configProperties.getProperty(propertyName));
		} catch (Exception e) {
			LOGGER.warn("Failed to parse property: {}. Error was: {}", propertyName, e.getMessage());
		}
		return false;
	}

	/**
	 * 
	 * @param propertyName
	 * @throws PropertyNotFoundException
	 */
	private boolean checkPropertyExists(String propertyName) throws PropertyNotFoundException {
		if (!configProperties.containsKey(propertyName) || configProperties.get(propertyName) == null) {
			throw new PropertyNotFoundException("Property: [" + propertyName + "] not found in configuration file");
		}
		return true;
	}
}
