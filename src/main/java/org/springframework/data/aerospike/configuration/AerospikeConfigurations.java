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
package org.springframework.data.aerospike.configuration;

import com.aerospike.client.Host;
import com.aerospike.client.policy.BatchPolicy;
import com.aerospike.client.policy.ClientPolicy;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.WritePolicy;

/**
 * @author fanendra
 * @createdOn 12-Nov-2014
 * @since
 */
public interface AerospikeConfigurations {

	public void loadConfig();
	/**
	 * Reloads file again to refresh {@link #configProperties}.
	 */
	public void reloadPolicyConfig();

	/**
	 * 
	 * @return
	 */
	public ClientPolicy clientPolicy();

	public Host[] getAerospikeHosts();

	/**
	 * 
	 * @param clazzName
	 * @return
	 */
	public Policy clazzReadPolicy(String clazzName);

	/**
	 * 
	 * @return
	 */
	public Policy readPolicy();
	/**
	 * 
	 * @param clazzName
	 * @return
	 */
	public WritePolicy clazzWritePolicy(String clazzName);
	/**
	 * 
	 * @return
	 */
	public WritePolicy writePolicy();
	/**
	 * 
	 * @param clazzName
	 * @return
	 */
	public BatchPolicy clazzBatchPolicy(String clazzName);

	/**
	 * 
	 * @return
	 */
	public BatchPolicy batchPolicy();
}
