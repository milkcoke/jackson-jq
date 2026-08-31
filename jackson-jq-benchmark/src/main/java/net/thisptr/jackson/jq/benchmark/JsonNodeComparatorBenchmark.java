package net.thisptr.jackson.jq.benchmark;

import java.util.Comparator;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;

import net.thisptr.jackson.jq.internal.misc.JsonNodeComparator;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
@Warmup(iterations = 8, time = 1)
@Measurement(iterations = 7, time = 1)
@Fork(7)
public class JsonNodeComparatorBenchmark {

	public enum Impl {
		hashMap(JsonNodeComparator.getInstance()),
		enumMap(EnumMapJsonNodeComparator.getInstance()),
		intArray(IntArrayJsonNodeComparator.getInstance());

		final Comparator<JsonNode> comparator;

		Impl(final Comparator<JsonNode> comparator) {
			this.comparator = comparator;
		}
	}

	@Param
	public Impl impl;

	private JsonNode[] scalars;
	private JsonNode[] arrays;
	private JsonNode[] objects;

	@Setup(Level.Trial)
	public void setUp() {
		scalars = JsonNodeSamples.scalars();
		arrays = JsonNodeSamples.arrays();
		objects = JsonNodeSamples.objects();
	}

	@Benchmark
	public void comparisonOperator(final Blackhole bh) {
		final Comparator<JsonNode> c = impl.comparator;
		for (final JsonNode lhs : scalars)
			for (final JsonNode rhs : scalars)
				bh.consume(BooleanNode.valueOf(c.compare(lhs, rhs) < 0));
	}

	@Benchmark
	public void compareArrayNode(final Blackhole bh) {
		final Comparator<JsonNode> c = impl.comparator;
		for (final JsonNode lhs : arrays)
			for (final JsonNode rhs : arrays)
				bh.consume(c.compare(lhs, rhs));
	}

	@Benchmark
	public void compareObjectNode(final Blackhole bh) {
		final Comparator<JsonNode> c = impl.comparator;
		for (final JsonNode lhs : objects)
			for (final JsonNode rhs : objects)
				bh.consume(c.compare(lhs, rhs));
	}
}
