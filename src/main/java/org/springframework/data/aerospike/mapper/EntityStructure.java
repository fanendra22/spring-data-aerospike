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
package org.springframework.data.aerospike.mapper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.data.aerospike.annotations.AerospikeTransient;

import com.aerospike.client.Key;
import com.aerospike.client.policy.BatchPolicy;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.client.query.IndexType;

/**
 * 
 * @author fanendra
 * @createdOn 12-Nov-2014
 * @since
 */
public class EntityStructure {
	/**
	 * Fully qualified name of the class for which data is being collected.
	 */
	private String					clazzName;
	/**
	 * Namespace of the underlying aerospike database. It can be thought of as
	 * the database name in SQL database terms.
	 */
	private String					nameSpace;
	/**
	 * Set name for this class. It can be thought of as table name for the
	 * entity.
	 */
	private String					set;
	/**
	 * Primary key for this class.
	 */
	private PersistableField		primaryKey;
	/**
	 * The fields list which will form the {@link Key}.
	 */
	private List<Field>				key						= new ArrayList<Field>();
	/**
	 * Holds actual field name to shortened version of the field name. This is
	 * required since there is a limitation of 14 characters on bin names.
	 */
	private Map<String, String>		fieldShortName			= new HashMap<String, String>();
	/**
	 * Indexes applied at different fields of a class.
	 */
	private Map<String, Index>		secondaryIndexes		= new HashMap<String, Index>();
	/**
	 * Fields that'll be persisted in db. The fields marked with
	 * {@link AerospikeTransient} annotation will be skipped.
	 */
	private List<PersistableField>	persistableFields		= new ArrayList<PersistableField>();
	/**
	 * Default policy to use for this entity for write operations.
	 */
	private WritePolicy				writePolicy;
	/**
	 * Default read policy for this entity for read operations.
	 */
	private Policy					policy;
	/**
	 * Default policy to use for batch operations related to this entity.
	 */
	private BatchPolicy				batchPolicy;
	/**
	 * Holds information whether structure for the class has been initialized.
	 */
	private boolean					structureInitialized	= false;
	/**
	 * Holds information whether indexes for this class structure has been
	 * initialized in db.
	 */
	private AtomicBoolean			indexesInitialized		= new AtomicBoolean(false);

	public EntityStructure(String clazzName) {
		this.clazzName = clazzName;
	}

	public String getClazzName() {
		return clazzName;
	}

	public void setClazzName(String clazzName) {
		this.clazzName = clazzName;
	}

	public String getNameSpace() {
		return nameSpace;
	}

	public void setNameSpace(String nameSpace) {
		this.nameSpace = nameSpace;
	}

	public String getSet() {
		return set;
	}

	public void setSet(String set) {
		this.set = set;
	}

	public List<Field> getKey() {
		return key;
	}

	public void setKey(List<Field> key) {
		this.key = key;
	}

	public Map<String, String> getFieldShortName() {
		return fieldShortName;
	}

	public void setFieldShortName(Map<String, String> fieldShortName) {
		this.fieldShortName = fieldShortName;
	}

	public Map<String, Index> getSecondaryIndexes() {
		return secondaryIndexes;
	}

	public void setSecondaryIndexes(Map<String, Index> secondaryIndexes) {
		this.secondaryIndexes = secondaryIndexes;
	}

	public WritePolicy getWritePolicy() {
		return writePolicy;
	}

	public void setWritePolicy(WritePolicy writePolicy) {
		this.writePolicy = writePolicy;
	}

	public List<PersistableField> getPersistableFields() {
		return persistableFields;
	}

	public void setPersistableFields(List<PersistableField> persistableFields) {
		this.persistableFields = persistableFields;
	}

	public Policy getPolicy() {
		return policy;
	}

	public void setPolicy(Policy policy) {
		this.policy = policy;
	}

	public BatchPolicy getBatchPolicy() {
		return batchPolicy;
	}

	public void setBatchPolicy(BatchPolicy batchPolicy) {
		this.batchPolicy = batchPolicy;
	}

	public PersistableField getPrimaryKey() {
		return primaryKey;
	}

	public void setPrimaryKey(PersistableField primaryKey) {
		this.primaryKey = primaryKey;
	}
	
	public boolean isStructureInitialized() {
		return structureInitialized;
	}
	
	public void setStructureInitialized(boolean structureInitialized) {
		this.structureInitialized = structureInitialized;
	}

	public boolean isIndexesInitialized() {
		return indexesInitialized.get();
	}

	public void setIndexesInitialized(boolean indexesInitialized) {
		this.indexesInitialized.compareAndSet(isIndexesInitialized(), indexesInitialized);
	}

	public static class Index {
		private final String	indexName;
		private final IndexType	indexType;

		public Index(String indexName, IndexType indexType) {
			this.indexName = indexName;
			this.indexType = indexType;
		}

		public String getIndexName() {
			return indexName;
		}

		public IndexType getIndexType() {
			return indexType;
		}

	}
}
