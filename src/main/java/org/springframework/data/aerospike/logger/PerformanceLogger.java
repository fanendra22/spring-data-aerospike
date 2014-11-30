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
package org.springframework.data.aerospike.logger;

import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author fanendra
 * @createdOn 13-Nov-2014
 * @since
 */
@Aspect
@Component
public class PerformanceLogger {
	private static final Logger								LOGGER					= LoggerFactory.getLogger(PerformanceLogger.class);
	private static ConcurrentHashMap<String, MethodStats>	methodStats				= new ConcurrentHashMap<String, MethodStats>();
	private static long										statLogFrequency		= 1000;
	private static long										methodWarningThreshold	= 500;

	@Pointcut("within(@org.springframework.data.aerospike.logger.PerformanceMonitor *)")
	public void logPerformance() {
	}

	@Around("logPerformance()")
	public Object profile(ProceedingJoinPoint joinPoint) throws Throwable {
		long start = System.currentTimeMillis();
		try {
			return joinPoint.proceed();
		} finally {
			long time = System.currentTimeMillis() - start;
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("{}, execution time: {}ms", joinPoint.getSignature(), time);
			}
			updateStats(joinPoint.getSignature().toLongString(), time);
		}
	}

	/**
	 * Logs the method stats at defined frequency.
	 * 
	 * @param methodName
	 * @param elapsedTime
	 */
	private void updateStats(String methodName, long elapsedTime) {
		MethodStats stats = methodStats.get(methodName);
		if (stats == null) {
			stats = new MethodStats(methodName);
			methodStats.put(methodName, stats);
		}
		stats.count++;
		stats.totalTime += elapsedTime;
		if (elapsedTime > stats.maxTime) {
			stats.maxTime = elapsedTime;
		}

		if (elapsedTime > methodWarningThreshold) {
			LOGGER.warn("method warning: {}, cnt = {}, lastTime = {}, maxTime = {}", methodName, stats.count, elapsedTime, stats.maxTime);
		}

		if (stats.count % statLogFrequency == 0) {
			long avgTime = stats.totalTime / stats.count;
			long runningAvg = (stats.totalTime - stats.lastTotalTime) / statLogFrequency;
			LOGGER.info("method: {}, cnt = {}, lastTime = {}, avgTime = {}, runningAvg = {}, maxTime = {}", methodName, stats.count, elapsedTime,
					avgTime, runningAvg, stats.maxTime);
			// reset the last total time
			stats.lastTotalTime = stats.totalTime;
		}
	}

	class MethodStats {
		public String	methodName;
		public long		count;
		public long		totalTime;
		public long		lastTotalTime;
		public long		maxTime;

		public MethodStats(String methodName) {
			this.methodName = methodName;
		}
	}
}
