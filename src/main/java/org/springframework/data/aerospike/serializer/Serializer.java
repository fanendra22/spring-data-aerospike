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
package org.springframework.data.aerospike.serializer;

import org.springframework.data.aerospike.exceptions.SerializationException;
import org.springframework.data.aerospike.logger.PerformanceMonitor;

/**
 * @author fanendra
 * @createdOn 20-Nov-2014
 * @since
 */
@PerformanceMonitor
public interface Serializer {
	public byte[] doSerialize(Class<? extends Object> classType, Object obj) throws SerializationException;

	public Object doDeserialize(byte[] data, Class<? extends Object> classType) throws SerializationException;
}
