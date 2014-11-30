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
package org.springframework.data.aerospike.serializer.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;
import org.springframework.data.aerospike.exceptions.SerializationException;
import org.springframework.data.aerospike.logger.PerformanceMonitor;
import org.springframework.data.aerospike.serializer.Serializer;
import org.springframework.stereotype.Service;

/**
 * @author fanendra
 * @createdOn 20-Nov-2014
 * @since
 */
@PerformanceMonitor
@Service("fstSerializer")
public class FSTSerializer implements Serializer {
	/**
	 * 
	 */
	private final FSTConfiguration	conf	= FSTConfiguration.createFastBinaryConfiguration();

	/**
	 * 
	 * @param classType
	 * @param obj
	 * @return
	 * @throws SerializationException
	 */
	@Override
	public byte[] doSerialize(Class<? extends Object> classType, Object obj) throws SerializationException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		FSTObjectOutput out = conf.getObjectOutput(byteArrayOutputStream);
		byte[] responseData = null;
		try {
			out.writeObject(obj, Object.class);
			out.flush();
			responseData = byteArrayOutputStream.toByteArray();
			byteArrayOutputStream.close();
		} catch (IOException e) {
			throw new SerializationException("IO Exception occured during FST Serialization ", e);
		} catch (Exception e) {
			throw new SerializationException("Exception occured during FST Serialization ", e);
		}
		return responseData;
	}

	/**
	 * 
	 * @param data
	 * @param classType
	 * @return
	 * @throws SerializationException
	 */
	@Override
	public Object doDeserialize(byte[] data, Class<? extends Object> classType) throws SerializationException {
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
		FSTObjectInput in = conf.getObjectInput(byteArrayInputStream);
		Object result = null;
		try {
			result = (Object) in.readObject(Object.class);
			byteArrayInputStream.close();
		} catch (IOException e) {
			throw new SerializationException("IO Exception occured during FST De-Serialization ", e);
		} catch (Exception e) {
			throw new SerializationException("Exception occured during FST De-Serialization ", e);
		}
		return result;
	}

	/**
	 * 
	 * @param obj
	 * @return
	 * @throws SerializationException
	 */
	public byte[] doSerialize(Object obj) throws SerializationException {
		try {
			return conf.asByteArray(obj);
		} catch (Exception e) {
			throw new SerializationException("Exception occured during FST Serialization ", e);
		}
	}

	/**
	 * 
	 * @param data
	 * @return
	 * @throws SerializationException
	 */
	public Object doDeserialize(byte[] data) throws SerializationException {
		try {
			return conf.asObject(data);
		} catch (Exception e) {
			throw new SerializationException("Exception occured during FST De-Serialization ", e);
		}
	}
}
