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
package org.springframework.data.aerospike.operations.impl;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.aerospike.client.Client;
import org.springframework.data.aerospike.exceptions.AerospikeException;
import org.springframework.data.aerospike.exceptions.AerospikePrimaryKeyNotDefinedException;
import org.springframework.data.aerospike.logger.PerformanceMonitor;
import org.springframework.data.aerospike.mapper.EntityMapper;
import org.springframework.data.aerospike.mapper.EntityStructure;
import org.springframework.data.aerospike.mapper.EntityStructure.Index;
import org.springframework.data.aerospike.operations.AerospikeOperations;
import org.springframework.stereotype.Service;

import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.Value;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.query.Filter;
import com.aerospike.client.query.IndexType;
import com.aerospike.client.query.Statement;

/**
 * 
 * @author fanendra
 * @createdOn 20-Oct-2014
 * @since
 */
@PerformanceMonitor
@Service("aerospikeOperations")
public class AerospikeOperationsImpl implements AerospikeOperations {
	/**
	 * 
	 */
	private static final Logger	LOGGER			= LoggerFactory.getLogger(AerospikeOperations.class);

	@Autowired
	private EntityMapper		entityMapper;

	@Autowired
	private Client				client;

	@Override
	public void put(Serializable entity) throws AerospikeException {
		EntityStructure structure = entityMapper.getEntityStructure(entity.getClass());
		// Create indexes in database
		if (!structure.isIndexesInitialized()) {
			addEntityIndex(structure);
		}
		client.getAerospikeClient().put(structure.getWritePolicy(), createKey(structure, entityMapper.getPrimaryKey(structure, entity)),
				entityMapper.getBins(entity));
	}

	@Override
	public void put(String namespace, String set, Serializable entity) throws AerospikePrimaryKeyNotDefinedException, AerospikeException {
		EntityStructure structure = entityMapper.getEntityStructure(entity.getClass());
		// Create indexes in database
		if (!structure.isIndexesInitialized()) {
			addEntityIndex(structure);
		}
		client.getAerospikeClient().put(structure.getWritePolicy(), createKey(namespace, set, entityMapper.getPrimaryKey(structure, entity)),
				entityMapper.getBins(entity));
	}

	@Override
	public void put(String namespace, String set, Serializable key, Bin... bins) throws AerospikeException {
		client.getAerospikeClient().put(client.getDefaultWritePolicy(), createKey(namespace, set, key), bins);

	}

	@Override
	public <T extends Serializable> T get(String namespace, String set, Serializable key, Class clazz) {
		Record record = client.getAerospikeClient().get(client.getAerospikeClient().readPolicyDefault, createKey(namespace, set, key));
		return entityMapper.reverseMap(record, clazz);
	}

	@Override
	public <K extends Serializable, V extends Serializable> V get(K key, Class<? extends Serializable> clazz) throws AerospikeException {
		EntityStructure structure = entityMapper.getEntityStructure(clazz);
		Record record = client.getAerospikeClient().get(structure.getPolicy(), createKey(structure, key));
		return entityMapper.reverseMap(record, clazz);
	}

	@Override
	public <K extends Serializable, V extends Serializable> Map<K, V> get(K[] key, Class<? extends Serializable> clazz) throws AerospikeException {
		EntityStructure structure = entityMapper.getEntityStructure(clazz);
		Key[] keys = new Key[key.length];
		for (int i = 0; i < key.length; i++) {
			keys[i] = createKey(structure, key[i]);
		}
		Record[] records = client.getAerospikeClient().get(structure.getBatchPolicy(), keys);
		if (records != null) {
			return entityMapper.reverseMap(key, records, clazz);
		}
		return null;
	}

	@Override
	public List<? extends Serializable> query(Class clazz, String key, Serializable start, Serializable end) throws AerospikeException {
		EntityStructure structure = entityMapper.getEntityStructure(clazz);
		Index index = structure.getSecondaryIndexes().get(key);
		if (index == null) {
			LOGGER.warn("Querying key: {} for class: {} hasn't been indexed. It is advised to add index before querying...", key, clazz);
			return null;
		}
		Statement stmt = new Statement();
		stmt.setNamespace(structure.getNameSpace());
		stmt.setSetName(structure.getSet());
		if (start instanceof Integer || start instanceof Long) {
			stmt.setFilters(Filter.range(key, ((Number) start).longValue(), ((Number) end).longValue()));
		} else {
			stmt.setFilters(Filter.range(key, Value.get(start), Value.get(end)));
		}
		// Execute the query and return results
		return entityMapper.reverseMap(client.getAerospikeClient().query(null, stmt), clazz);
	}

	@Override
	public List<? extends Serializable> query(Class clazz, String key, Serializable value) throws AerospikeException {
		EntityStructure structure = entityMapper.getEntityStructure(clazz);
		Index index = structure.getSecondaryIndexes().get(key);
		if (index == null) {
			LOGGER.warn("Querying key: {} for class: {} hasn't been indexed. It is advised to add index before querying...", key, clazz);
			return null;
		}
		Statement stmt = new Statement();
		stmt.setNamespace(structure.getNameSpace());
		stmt.setSetName(structure.getSet());
		if (value instanceof Integer || value instanceof Long) {
			stmt.setFilters(Filter.equal(key, ((Number) value).longValue()));
		} else if (value instanceof String) {
			stmt.setFilters(Filter.equal(key, (String) value));
		} else {
			stmt.setFilters(Filter.equal(key, Value.get(value)));
		}
		// Execute the query and return results
		return entityMapper.reverseMap(client.getAerospikeClient().query(null, stmt), clazz);
	}

	@Override
	public boolean remove(Serializable key, Class<? extends Serializable> clazz) throws AerospikeException {
		EntityStructure structure = entityMapper.getEntityStructure(clazz);
		return client.getAerospikeClient().delete(structure.getWritePolicy(), createKey(structure, key));
	}

	@Override
	public void addIndex(Policy policy, String namespace, String set, String binName, String indexName, IndexType indexType) {
		client.getAerospikeClient().createIndex(policy, namespace, set, indexName, binName, indexType);
	}

	@Override
	public boolean exists(Serializable key, Class clazz) throws AerospikeException {
		EntityStructure structure = entityMapper.getEntityStructure(clazz);
		return client.getAerospikeClient().exists(structure.getPolicy(), createKey(structure, key));
	}

	@Override
	public boolean isConnected() throws AerospikeException {
		return client.getAerospikeClient().isConnected();
	}

	/**
	 * Replicates all the indexes defined in the entity to database.
	 * 
	 * @param structure
	 */
	private void addEntityIndex(EntityStructure structure) {
		for (Entry<String, Index> entry : structure.getSecondaryIndexes().entrySet()) {
			//Check if indexes have already been done. This is done using a new set
			// and using entity namespace and index set. If index is already set no need 
			//to do any thing. Otherwise create index and set in the database
			String indexName = entry.getValue().getIndexName();
			Record record = client.getAerospikeClient().get(null, createKey(structure.getNameSpace(), "indexes", indexName));
			if(record != null && 1 == record.getInt(indexName)) {
				LOGGER.warn("Index: {} for class: {} has already been created. Skipping index creation for this field", entry.getValue().getIndexName(), structure.getClazzName());
				continue;
			}
			addIndex(structure.getPolicy(), structure.getNameSpace(), structure.getSet(), entry.getKey(), entry.getValue().getIndexName(), entry
					.getValue().getIndexType());	
			client.getAerospikeClient().put(null, createKey(structure.getNameSpace(), "indexes", indexName), new Bin(indexName, 1));
			LOGGER.info("Index: {} for class: {} has been created", entry.getValue().getIndexName(), structure.getClazzName());
		}
		structure.setIndexesInitialized(true);
	}

	/**
	 * 
	 * @param namespace
	 * @param set
	 * @param key
	 * @return
	 */
	private Key createKey(String namespace, String set, Object key) {
		if (key instanceof Integer) {
			return new Key(namespace, set, (Integer) key);
		} else if (key instanceof Long) {
			return new Key(namespace, set, (Long) key);
		} else if (key instanceof String) {
			return new Key(namespace, set, (String) key);
		} else if (key instanceof byte[]) {
			return new Key(namespace, set, (byte[]) key);
		} else {
			return new Key(namespace, set, Value.get(key));
		}
	}

	/**
	 * 
	 * @param structure
	 * @param key
	 * @return
	 */
	private Key createKey(EntityStructure structure, Object key) {
		return createKey(structure.getNameSpace(), structure.getSet(), key);
	}

}
