/*
 * Licensed to the Nervousync Studio (NSYC) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.nervousync.test.utils;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.nervousync.beans.location.GeoPoint;
import org.nervousync.test.BaseTest;
import org.nervousync.utils.LocationUtils;

public final class LocationTest extends BaseTest {

	private static final GeoPoint PEKING = GeoPoint.gpsPoint(116.4074d, 39.9042d);
	private static final GeoPoint CANTON = GeoPoint.gpsPoint(113.2644d, 23.1291d);

	@Test
	@Order(10)
	public void calculate() throws Exception {
		this.logger.info("Location_Calculate_Distance", LocationUtils.calcDistance(PEKING, CANTON));
	}

	@Test
	@Order(20)
	public void convert() throws Exception {
		GeoPoint gcj02 = LocationUtils.anyToGCJ02(PEKING);
		this.logger.info("Location_Convert_GCJ02",
				Double.toString(gcj02.getLongitude()), Double.toString(gcj02.getLatitude()));
		GeoPoint bd09 = LocationUtils.anyToBD09(gcj02);
		this.logger.info("Location_Convert_BD09",
				Double.toString(bd09.getLongitude()), Double.toString(bd09.getLatitude()));
	}
}
