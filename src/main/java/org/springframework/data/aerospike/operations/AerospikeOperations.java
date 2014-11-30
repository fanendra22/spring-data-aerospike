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
package org.springframework.data.aerospike.operations;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.springframework.data.aerospike.annotations.AerospikeKey;
import org.springframework.data.aerospike.exceptions.AerospikeException;
import org.springframework.data.aerospike.exceptions.AerospikeIncompatibleEntityException;
import org.springframework.data.aerospike.exceptions.AerospikePrimaryKeyNotDefinedException;

import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.query.IndexType;

/**
 * This interface defines Aerospike DB common operations in order to simplify user's work.
 * For simplicity user just has to provide class on which operation has to be performed and 
 * the bins/keys which will be used for querying.<p>
 * All entities that can be stored in Aerospike need to implement serializable interface
 * in order to transform those in bytes.
 * 
 * @author    fanendra
 * @createdOn 20-Oct-2014
 * @since     
 */
public interface AerospikeOperations {
	/**
	 * Writes an entity in Aerospike database.
	 * @param entity
	 */
	public void put(Serializable entity) throws AerospikeException;
	/**
	 * Adds the given entity into aerospike db into given namespace and set. The
	 * key for this entity would be {@link AerospikeKey} annotated field of the
	 * class. If no key is found {@link AerospikePrimaryKeyNotDefinedException} 
	 * will be thrown.
	 * @param namespace
	 * @param set
	 * @param entity
	 */
	public void put(String namespace, String set, Serializable entity) throws AerospikePrimaryKeyNotDefinedException, AerospikeException;
	/**
	 * Same as {@link #put(String, String, Serializable)} except it takes key as external input.
	 * @param namespace
	 * @param set
	 * @param key
	 * @param value
	 */
	public void put(String namespace, String set, Serializable key, Bin...bins) throws AerospikeException;
	/**
	 * It can be used to retrieve an arbitrary entity from aerospike db for which namespace and set information is
	 * externally supplied. clazz parameter will help in converting aerospike record to original class. If class
	 * structure is not found {@link AerospikeIncompatibleEntityException} will be thrown.
	 * @param namespace
	 * @param set
	 * @param key
	 * @param clazz
	 * @return
	 */
	public <T extends Serializable> T get(String namespace, String set, Serializable key, Class clazz);
	/**
	 * Search an entity on the basis of passed key. This method should be used for those entities which 
	 * are having single bin as {@link Key}. If the entity has multiple bins annotated with {@link AerospikeKey} 
	 * The bin that will be used will be  chosen on their occurrence order.
	 * 
	 * @param key
	 * @param clazz
	 * @return
	 */
	public <K extends Serializable, V extends Serializable> V get(K key, Class<? extends Serializable> clazz) throws AerospikeException;
	/**
	 * Queries for multiple keys in a single n/w I/O if sufficient number of concurrent threads has been defined.
	 * @param key
	 * @param clazz
	 * @return
	 */
	public <K extends Serializable, V extends Serializable> Map<K, V> get(K [] key, Class<? extends Serializable> clazz) throws AerospikeException;
	/**
	 * Removes the entity from the database that matches the given keys. Returns the status whether 
	 * record is deleted.
	 * 
	 * @param key
	 * @param clazz
	 * @return
	 */
	public boolean remove(Serializable key, Class<? extends Serializable> clazz) throws AerospikeException;
	/**
	 * Provides an interface to query aerospike db on the basis of secondary key. It should be ensured that the key
	 * which is being used for querying has been indexed for performance reasons.
	 * 
	 * @param clazz
	 * @param query
	 * @return
	 */
	public List<? extends Serializable> query(Class clazz, String key, Serializable start, Serializable end) throws AerospikeException;
	/**
	 * Provides an interface to range query the aerospike db on the basis of secondary key. It should be ensured that the key
	 * which is being used for querying has been indexed for performance reasons.
	 * @param rangeQuery
	 * @return
	 * @throws AerospikeException
	 */
	public List<? extends Serializable> query(Class clazz, String key, Serializable value) throws AerospikeException;
	/**
	 * Adds index for the given bin in given namespace and set. The type of index and name will also be used from the arguments.
	 * 
	 * @param policy
	 * @param namespace
	 * @param set
	 * @param bin
	 * @param indexName
	 * @param indexType
	 */
	public void addIndex(Policy policy, String namespace, String set, String binName, String indexName, IndexType indexType) throws AerospikeException;
	/**
	 * Checks whether supplied key exists in the db. DB related information will be picked 
	 * up from the clazz argument.
	 * 
	 * @param key
	 * @param clazz
	 * @return
	 * @throws AerospikeException
	 */
	public boolean exists(Serializable key, Class clazz) throws AerospikeException;
	
	/**
	 * Checks whether connection to underlying aerospike db is intact.
	 * @return
	 * @throws AerospikeException
	 */
	public boolean isConnected() throws AerospikeException;
	
}
