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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.aerospike.annotations.AerospikeEntity;
import org.springframework.data.aerospike.annotations.AerospikeTransient;
import org.springframework.data.aerospike.exceptions.AerospikeIncompatibleEntityException;
import org.springframework.data.aerospike.exceptions.AerospikePrimaryKeyNotDefinedException;
import org.springframework.data.aerospike.logger.PerformanceMonitor;
import org.springframework.data.aerospike.serializer.Serializer;
import org.springframework.stereotype.Service;

import com.aerospike.client.Bin;
import com.aerospike.client.Record;
import com.aerospike.client.query.RecordSet;

/**
 * Holds aerospike mapping related information for all Aerospike persistable
 * entities. The data from the entity will be read whenever any entity operation
 * will be performed first time for an entity.
 * <p>
 * If data is not loaded or found for an entity it can not be persisted unless
 * db related information is also passed. AerospikeOperation class is having
 * methods for passing db related information as well in order to perform db
 * operations on such entities.
 * 
 * @author fanendra
 * @createdOn 20-Oct-2014
 * @since
 */
@Service("entityMapper")
@SuppressWarnings({ "rawtypes", "unchecked" })
public class EntityMapper {
	private static final Logger											LOGGER				= LoggerFactory.getLogger(EntityMapper.class);
	/**
	 * Map to hold class structure of all the Aerospike persistable classes.
	 */
	private final ConcurrentHashMap<Class, EntityStructure>				entityStructure		= new ConcurrentHashMap<Class, EntityStructure>(100);
	/**
	 * Map to hold class and it's rejected reason while entity was read for meta
	 * information. This would specially be helpful to avoid multiple time
	 * entity structure reads if it was failed earlier due to some reason.
	 */
	private final ConcurrentHashMap<Class, StructureRejectionReason>	structureRejections	= new ConcurrentHashMap<Class, EntityMapper.StructureRejectionReason>(
																									100);

	@Autowired
	private EntityStructureReader										entityStructureReader;

	@Autowired
	private Serializer													serializer;
	/**
	 * Puts lock in order to read the structure only once.
	 */
	private Lock														structureLock		= new ReentrantLock();

	/**
	 * 
	 */
	private static enum StructureRejectionReason {
		NOT_AEROSPIKE_ENTITY, FAILED_IN_PARSING;
	}

	/**
	 * Reads an entity's class structure and records it for future usage if it
	 * hasn't been recorded earlier. If it is available already it'll return the
	 * {@link EntityStructure} for the passed class.
	 * <p>
	 * If the class is found in {@link #structureRejections}
	 * {@link AerospikeIncompatibleEntityException} will be thrown since the
	 * entity will no longer be compatible as per {@link AerospikeEntity}
	 * defintion.
	 * 
	 * @param clazz
	 * @return
	 * @throws AerospikeIncompatibleEntityException
	 */
	public EntityStructure getEntityStructure(Class clazz) throws AerospikeIncompatibleEntityException {
		EntityStructure structure = entityStructure.get(clazz);
		if (structure == null && !structureRejections.containsKey(clazz)) {
			try {
				structureLock.lock();
				if(structure == null || !structure.isStructureInitialized()) {
					structure = entityStructureReader.readAerospikeEntity(clazz);
					entityStructureReader.readStructure(clazz);
					entityStructureReader.reloadClazzPolicies(clazz);
					structure.setStructureInitialized(true);
				}
			} catch (AerospikeIncompatibleEntityException e) {
				structureRejections.put(clazz, StructureRejectionReason.NOT_AEROSPIKE_ENTITY);
				LOGGER.error("Failed to parse class: {}", clazz.getName(), e);
			} catch (Exception e) {
				structureRejections.put(clazz, StructureRejectionReason.FAILED_IN_PARSING);
				LOGGER.error("Failed to parse class: {}", clazz.getName(), e);
			} finally {
				structureLock.unlock();
			}
		}
		if (structureRejections.containsKey(clazz)) {
			throw new AerospikeIncompatibleEntityException(clazz.getName() + " is not Aerospike compatible");
		}
		return structure;
	}

	/**
	 * Adds {@link EntityStructure} for the given class in
	 * {@link #entityStructure}.
	 * 
	 * @param clazz
	 * @param structure
	 */
	public void putEntityStructure(Class clazz, EntityStructure structure) {
		this.entityStructure.put(clazz, structure);
	}

	/**
	 * Returns {@link EntityStructure} for all the classes recorded till now.
	 * 
	 * @return
	 */
	public Collection<EntityStructure> getAllEntitiesStructure() {
		return this.entityStructure.values();
	}

	/**
	 * 
	 * @param clazz
	 * @return
	 */
	public String getFirstKeyField(Class clazz) {
		EntityStructure structure = entityStructure.get(clazz);
		if (structure != null) {
			if (structure.getKey().size() > 0) {
				return structure.getKey().get(0).getName();
			} else {
				LOGGER.debug("No key field defined for class: {}", clazz.getName());
			}
		} else {
			LOGGER.debug("Structure data is not loaded for class: {}", clazz.getName());
		}
		return null;
	}

	/**
	 * Returns primary key value for the given entity. The primary key structure
	 * will be identified from {@link EntityStructure}.
	 * 
	 * @param clazz
	 * @param entity
	 * @return
	 * @throws AerospikePrimaryKeyNotDefinedException
	 * @throws AerospikeIncompatibleEntityException
	 */
	public Object getPrimaryKey(EntityStructure structure, Serializable entity) throws AerospikePrimaryKeyNotDefinedException,
			AerospikeIncompatibleEntityException {
		if (structure != null) {
			if (structure.getPrimaryKey() != null) {
				Method getter = structure.getPrimaryKey().getGetter();
				try {
					Object keyVal = getter.invoke(entity);
					if (keyVal == null) {
						return null;
					}
					if (!isOptimumDataType(keyVal)) {
						return serializer.doSerialize(Object.class, keyVal);
					}
					return keyVal;
				} catch (Exception e) {
					LOGGER.error("Error while reading key's value for class: {}", structure.getClazzName(), e);
				}
			} else {
				throw new AerospikePrimaryKeyNotDefinedException("No primary key defined for class: " + structure.getClazzName());
			}
		} else {
			throw new AerospikeIncompatibleEntityException("Structure data is not loaded for class: " + entity.getClass());
		}
		return null;
	}

	/**
	 * 
	 * @param clazz
	 * @return
	 */
	public String getKeyField(Class clazz) {
		return getFirstKeyField(clazz);
	}

	/**
	 * Returns bins for the passed entity. The bins will not contain any field
	 * which are annotated with {@link AerospikeTransient}.
	 * 
	 * @param entity
	 * @return
	 */
	@PerformanceMonitor
	public Bin[] getBins(Serializable entity) {
		EntityStructure structure = entityStructure.get(entity.getClass());
		if (structure != null) {
			// Get all settable fields count. Iterate and get the value from
			// specific entity.
			Bin[] bins = new Bin[structure.getPersistableFields().size()];
			int cntr = 0;
			for (PersistableField field : structure.getPersistableFields()) {
				bins[cntr++] = prepareBin(entity, field);
			}
			return bins;
		}
		return null;
	}

	/**
	 * Creates {@link Bin} for the passed field for the given entity. The name
	 * for the bin will be the programmatic name of the field in class.
	 * 
	 * @param entity
	 * @param field
	 * @return
	 */
	private Bin prepareBin(Serializable entity, PersistableField field) {
		Bin bin = null;
		try {
			EntityStructure structure = entityStructure.get(entity.getClass());
			Method getter = field.getGetter();
			Object value = getter.invoke(entity);
			if (value != null && field.isSerializerRequired()) {
				value = serializer.doSerialize(Object.class, value);
			}
			//Handle field name length restriction
			String fieldName = field.getField().getName();
			if(fieldName.length() > 14) {
				fieldName = structure.getFieldShortName().get(fieldName);
			}
			bin = new Bin(fieldName, value);
		} catch (Exception e) {
			LOGGER.error("Failed to created bin from field {} of the class {}", field.getField().getName(), entity.getClass(), e);
		} finally {

		}
		return bin;
	}

	/**
	 * Creates entities from the given {@link RecordSet} for the passed class.
	 * 
	 * @param record
	 * @param clazz
	 * @return
	 */
	@PerformanceMonitor
	public <T extends Serializable> List<T> reverseMap(RecordSet recordSet, Class clazz) {
		List<T> values = new ArrayList<T>();
		try {
			while (recordSet != null && recordSet.next()) {
				T value = (T) reverseMap(recordSet.getRecord(), clazz);
				values.add(value);
			}
		} finally {
			recordSet.close();
		}
		if (values.isEmpty()) {
			return Collections.EMPTY_LIST;
		}
		return values;
	}

	/**
	 * Creates entity from the given {@link Record} for the passed class.
	 * 
	 * @param record
	 * @param clazz
	 * @return
	 */
	@PerformanceMonitor
	public <T extends Serializable> T reverseMap(Record record, Class clazz) {
		if (record != null) {
			return (T) prepareObject(record, clazz);
		}
		return null;
	}

	/**
	 * Creates entities array from the passed {@link Record} array.
	 * 
	 * @param record
	 * @param clazz
	 * @return
	 */
	@PerformanceMonitor
	public <K extends Serializable, T extends Serializable> Map<K, T> reverseMap(K[] keys, Record[] records, Class clazz) {
		if (records.length <= 0) {
			return Collections.EMPTY_MAP;
		}
		Map<K, T> values = new HashMap<K, T>(records.length);
		for (int i = 0; i < records.length; i++) {
			if (records[i] != null) {
				T value = (T) reverseMap(records[i], clazz);
				if (value != null) {
					values.put(keys[i], value);
				}
			}
		}
		if (values.isEmpty()) {
			return Collections.EMPTY_MAP;
		}
		return values;
	}

	/**
	 * Creates object from the given {@link Record} and class.
	 * 
	 * @param record
	 * @param clazz
	 * @return
	 */
	private Object prepareObject(Record record, Class clazz) {
		Object object = null;
		try {
			object = clazz.newInstance();
			EntityStructure structure = entityStructure.get(clazz);
			for (PersistableField field : structure.getPersistableFields()) {
				//Handle field name length restriction
				String fieldName = field.getField().getName();
				if(fieldName.length() > 14) {
					fieldName = structure.getFieldShortName().get(fieldName);
				}
				Object fieldValue = record.bins.get(fieldName);
				if (fieldValue != null) {
					if (field.isSerializerRequired()) {
						fieldValue = serializer.doDeserialize((byte[]) fieldValue, Object.class);
					}
					field.getSetter().invoke(object, fieldValue);
				}
			}
		} catch (Exception e) {
			LOGGER.error("Failed to reverse map class {}", clazz.getName(), e);
		} finally {

		}
		return object;
	}

	/**
	 * 
	 * @param value
	 * @return
	 */
	private boolean isOptimumDataType(Object value) {
		return (value instanceof Integer || value instanceof Long || value instanceof String || value instanceof byte[]);
	}
}
