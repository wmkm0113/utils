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

package org.nervousync.test.generator;

import org.nervousync.utils.id.IDUtils;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)     // 测试吞吐量（每秒生成的ID数量）
@OutputTimeUnit(TimeUnit.SECONDS)   // 输出单位为秒
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS) // 预热3轮，每轮2秒
@Measurement(iterations = 10, time = 2, timeUnit = TimeUnit.SECONDS) // 正式测试5轮，每轮2秒
@Fork(value = 3, jvmArgsAppend = {"-Xms4g", "-Xmx4g", "-XX:+UseG1GC"}) // 分叉1个独立的JVM进程运行，防止JIT互相污染
@State(Scope.Benchmark) // 全局单例状态
@Threads(Threads.MAX) // 模拟最大并发锁竞争
public class GeneratorBenchmark {

	@Benchmark
	public String nano() {
		return IDUtils.nano();
	}

	@Benchmark
	public long snowflake() {
		return IDUtils.snowflake();
	}

	@Benchmark
	public String UUIDv1() {
		return IDUtils.UUIDv1().toString();
	}

	@Benchmark
	public String UUIDv2() {
		return IDUtils.UUIDv2().toString();
	}

	@Benchmark
	public String UUIDv3() {
		return IDUtils.UUIDv3("TestVersion3".getBytes()).toString();
	}

	@Benchmark
	public String UUIDv4() {
		return IDUtils.UUIDv4().toString();
	}

	@Benchmark
	public String UUIDv5() {
		return IDUtils.UUIDv5("TestVersion5".getBytes()).toString();
	}

	@Benchmark
	public String UUIDv6() {
		return IDUtils.UUIDv6().toString();
	}

	@Benchmark
	public String UUIDv7() {
		return IDUtils.UUIDv7().toString();
	}

	@Benchmark
	public String ulid() {
		return IDUtils.ULID().toString();
	}

	@Benchmark
	public String CUIDv1() {
		return IDUtils.CUIDv1().toString();
	}

	@Benchmark
	public String CUIDv2() {
		return IDUtils.CUIDv2().toString();
	}

	@Benchmark
	public String sparkId() {
		return IDUtils.sparkID().toString();
	}

	@Benchmark
	public String jdkUUID() {
		return UUID.randomUUID().toString();
	}

	public static void main(String[] args) throws Exception {
		Options opt = new OptionsBuilder()
				.include(GeneratorBenchmark.class.getSimpleName())
				.addProfiler(org.openjdk.jmh.profile.GCProfiler.class)
				.build();
		new Runner(opt).run();
	}
}
