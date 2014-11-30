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
package org.springframework.data.aerospike.mapper;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.aerospike.annotations.AerospikeEntity;
import org.springframework.data.aerospike.annotations.AerospikeIndex;
import org.springframework.data.aerospike.annotations.AerospikeKey;
import org.springframework.data.aerospike.annotations.AerospikeTransient;
import org.springframework.data.aerospike.client.Client;
import org.springframework.data.aerospike.configuration.AerospikeConfigurations;
import org.springframework.data.aerospike.exceptions.AerospikeException;
import org.springframework.data.aerospike.exceptions.AerospikeIncompatibleEntityException;
import org.springframework.data.aerospike.mapper.EntityStructure.Index;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;
import org.springframework.util.ReflectionUtils.FieldFilter;

/**
 * @author fanendra
 * @createdOn 12-Nov-2014
 * @since
 */
@Service("entityStructureReader")
public class EntityStructureReader {

	private static final Logger		LOGGER	= LoggerFactory.getLogger(EntityStructureReader.class);

	@Autowired
	private Client					client;

	@Autowired
	private AerospikeConfigurations	policyConfigurations;

	/**
	 * Checks whether entity is compatible to be stored in Aerospike db
	 * (annotated with {@link AerospikeEntity}). Sets the namespace and set for
	 * the entity in {@link EntityMapper}.
	 * 
	 * @param clazz
	 * @throws AerospikeIncompatibleEntityException
	 */
	public EntityStructure readAerospikeEntity(Class clazz) throws AerospikeIncompatibleEntityException {
		return readAerospikeEntity(clazz, null);
	}

	/**
	 * 
	 * @param clazz
	 * @param structure
	 * @return
	 * @throws AerospikeIncompatibleEntityException
	 */
	public EntityStructure readAerospikeEntity(Class clazz, EntityStructure structure) throws AerospikeIncompatibleEntityException {
		AerospikeEntity aerospikeEntity = (AerospikeEntity) clazz.getAnnotation(AerospikeEntity.class);
		if (aerospikeEntity == null) {
			throw new AerospikeIncompatibleEntityException(clazz.getName() + " is not annotated with @AerospikeEntity. Can't store");
		}
		if (structure == null) {
			structure = new EntityStructure(clazz.getName());
			client.getEntityMapper().putEntityStructure(clazz, structure);
		}
		structure.setNameSpace(aerospikeEntity.nameSpace());
		structure.setSet(aerospikeEntity.setName());
		if (aerospikeEntity.setName() == null || aerospikeEntity.setName().isEmpty()) {
			structure.setSet(clazz.getName());
		}
		return structure;
	}

	/**
	 * Reads passed class for aerospike related annotations. If no annotation is
	 * found all the fields would be treated as persistable.
	 * 
	 * @param clazz
	 * @throws IntrospectionException
	 * @throws SecurityException
	 * @throws NoSuchFieldException
	 */
	public void readStructure(Class clazz) throws AerospikeException, IntrospectionException, NoSuchFieldException, SecurityException {
		EntityStructure structure = client.getEntityMapper().getEntityStructure(clazz);
		// Get information about the underlying bean
		BeanInfo info = Introspector.getBeanInfo(clazz);
		PropertyDescriptor[] props = info.getPropertyDescriptors();
		// Load all the fields to identify applied annotations
		for (Field field : getAllInstanceFields(clazz)) {
			boolean isKey = false;
			// Handle transient fields
			if (field.isAnnotationPresent(AerospikeTransient.class)) {
				LOGGER.info("Field: {} of class: {} is annotated with @AerospikeTransient. It will not be saved", field.getName(), clazz.getName());
				continue;
			}
			// Handle key fields
			if (field.isAnnotationPresent(AerospikeKey.class)) {
				structure.getKey().add(field);
				isKey = true;
			}
			// Read secondary indexed fields
			AerospikeIndex indexAnnotation = field.getAnnotation(AerospikeIndex.class);
			if (indexAnnotation != null) {
				LOGGER.info("Found secondary index: {} of type: {} on field: {} of class: {}", indexAnnotation.name(), indexAnnotation.indexType(),
						field.getName(), clazz.getName());
				structure.getSecondaryIndexes().put(field.getName(), new Index(indexAnnotation.name(), indexAnnotation.indexType()));
			}
			for (PropertyDescriptor property : props) {
				if (field.getName().equals(property.getName())) {
					LOGGER.info("Found getter: {} and setter: {} for field: {} for class: {}", property.getReadMethod().getName(), property
							.getWriteMethod().getName(), field.getName(), clazz.getName());
					PersistableField persistableField = new PersistableField();
					persistableField.setField(field);
					persistableField.setSetter(property.getWriteMethod());
					persistableField.setGetter(property.getReadMethod());
					persistableField.setType(field.getType());
					structure.getPersistableFields().add(persistableField);
					//If field is key field set it as primary key for the class.
					if (isKey) {
						structure.setPrimaryKey(persistableField);
					}
					//If field's data type is not one of Integer, Long, String or byte[] mark field as 
					//serializer required field
					if (!(Integer.TYPE == field.getType() || Long.TYPE == field.getType() ||  
							field.getType().isAssignableFrom(Integer.class) || field.getType().isAssignableFrom(Long.class) ||
							field.getType().isAssignableFrom(String.class) || field.getType().isAssignableFrom(byte[].class))) {
						persistableField.setSerializerRequired(true);
					}
					//Shorten the field name if required.
					if(field.getName().length() > 14) {
						String shortName = field.getName().substring(0, 11) + "_" + structure.getPersistableFields().size();
						structure.getFieldShortName().put(field.getName(), shortName);
					}
				}
			}
		}
	}

	/**
	 * 
	 * @param clazz
	 * @return
	 */
	private Set<Field> getAllInstanceFields(Class clazz) {
		final Set<Field> instanceFields = new HashSet<Field>();
		ReflectionUtils.doWithFields(clazz, new FieldCallback() {
			@Override
			public void doWith(final Field field) throws IllegalArgumentException, IllegalAccessException {
				instanceFields.add(field);
			}
		}, new FieldFilter() {
			@Override
			public boolean matches(final Field field) {
				final int modifiers = field.getModifiers();
				// Drop static fields
				return !Modifier.isStatic(modifiers);
			}
		});
		return instanceFields;
	}

	/**
	 * 
	 * @param clazz
	 */
	public void reloadClazzPolicies(Class clazz) throws AerospikeException {
		EntityStructure structure = client.getEntityMapper().getEntityStructure(clazz);
		if (structure != null) {
			structure.setBatchPolicy(policyConfigurations.clazzBatchPolicy(clazz.getName()));
			structure.setPolicy(policyConfigurations.clazzReadPolicy(clazz.getName()));
			structure.setWritePolicy(policyConfigurations.clazzWritePolicy(clazz.getName()));
		}
	}
}
