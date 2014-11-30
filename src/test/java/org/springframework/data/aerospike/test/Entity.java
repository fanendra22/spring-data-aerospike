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
package org.springframework.data.aerospike.test;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.aerospike.annotations.AerospikeEntity;
import org.springframework.data.aerospike.annotations.AerospikeIndex;
import org.springframework.data.aerospike.annotations.AerospikeKey;

import com.aerospike.client.query.IndexType;

/**
 * 
 * @author fanendra
 * @createdOn 10-Nov-2014
 * @since
 */
@AerospikeEntity(nameSpace = "promo", setName = "test")
public class Entity implements Serializable {
	@AerospikeKey
	private int					id;
	@AerospikeIndex(name = "primaryValue", indexType = IndexType.STRING)
	private String				primaryValue;
	private String				secondaryValue;
	private Map<Long, String>	data	= new HashMap<Long, String>(2);

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getPrimaryValue() {
		return primaryValue;
	}

	public void setPrimaryValue(String primaryValue) {
		this.primaryValue = primaryValue;
	}

	public String getSecondaryValue() {
		return secondaryValue;
	}

	public void setSecondaryValue(String secondaryValue) {
		this.secondaryValue = secondaryValue;
	}

	public Map<Long, String> getData() {
		return data;
	}

	public void setData(Map<Long, String> data) {
		this.data = data;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Entity [id=").append(id).append(", primaryValue=").append(primaryValue).append(", secondaryValue=").append(secondaryValue)
				.append(", data=").append(data).append("]");
		return builder.toString();
	}

}
