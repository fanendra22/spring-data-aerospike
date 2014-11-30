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
package org.springframework.data.aerospike.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import com.aerospike.client.query.IndexType;

/**
 * Maps secondary indexes for this entry. The secondary indexes can be used
 * in aql (Aerospike Query Language) queries.
 * 
 * @author fanendra
 * @createdOn 20-Oct-2014
 * @since
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface AerospikeIndex {
	/**
	 * @return the index name of this secondary index.
	 */
	public String name();
	/**
	 * @return the index type of this secondary index.
	 */
	public IndexType indexType();
}
