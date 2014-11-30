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
package org.springframework.data.aerospike.client;

import org.springframework.data.aerospike.mapper.EntityMapper;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.policy.WritePolicy;

/**
 * @author    fanendra
 * @createdOn 20-Oct-2014
 * @since     
 */
public interface Client {
	public EntityMapper getEntityMapper();
	public AerospikeClient getAerospikeClient();
	public void reloadAerospikePolicies();
	public void initialize();
	public WritePolicy getDefaultWritePolicy();
}
