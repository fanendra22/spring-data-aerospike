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
package org.springframework.data.aerospike.exceptions;

/**
 * @author    fanendra
 * @createdOn 21-Oct-2014
 * @since     
 */
public class AerospikeConnectionFailedException extends AerospikeException {
	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;

	public AerospikeConnectionFailedException(String message) {
		super(message);
	}
	
	public AerospikeConnectionFailedException(String message, Throwable t) {
		super(message, t);
	}

}
