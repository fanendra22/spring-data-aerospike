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

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Describes a field from Aerospike mapped class.
 * 
 * @author fanendra
 * @createdOn 21-Oct-2014
 * @since
 */
public class PersistableField {
	/**
	 * Class's {@link Field} object for which this persistable object is being
	 * created.
	 */
	private Field	field;
	/**
	 * Field's data type. If field is known java classes the performance of 
	 * serialization/de-serialization would be awesome. Knowing type in advance
	 * can identify the type and save CPU as well as time at run time.
	 */
	private Class<?>	type;
	/**
	 * Getter method for current persistable field.
	 */
	private Method	getter;
	/**
	 * Setter method for current persistable field.
	 */
	private Method	setter;
	/**
	 * If field type is other than Integer/Long/String/byte[] then 
	 * custom serializer should be used as default serializer is slow.
	 * The field signifies whether custom serializer is needed.
	 */
	private boolean serializerRequired;

	public Field getField() {
		return field;
	}

	public void setField(Field field) {
		this.field = field;
	}

	public Method getGetter() {
		return getter;
	}

	public void setGetter(Method getter) {
		this.getter = getter;
	}

	public Method getSetter() {
		return setter;
	}

	public void setSetter(Method setter) {
		this.setter = setter;
	}

	public Class<?> getType() {
		return type;
	}
	
	public void setType(Class<?> type) {
		this.type = type;
	}
	
	public boolean isSerializerRequired() {
		return serializerRequired;
	}
	
	public void setSerializerRequired(boolean serializerRequired) {
		this.serializerRequired = serializerRequired;
	}
}
