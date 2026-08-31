package net.thisptr.jackson.jq.benchmark;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

/**
 * The sample data for the comparator benchmarks, read from {@code comparator-samples.json}.
 *
 * <p>
 * The values live in a JSON file rather than in a generator, so what the benchmark compares can be
 * read directly instead of reconstructed from code. Nothing here is random.
 */
public final class JsonNodeSamples {

	private static final String RESOURCE = "/comparator-samples.json";

	private static final ObjectReader objectReader = new ObjectMapper().readerFor(JsonNode.class);
	private static final JsonNode SAMPLES = load();

	private JsonNodeSamples() {
	}

	public static JsonNode[] scalars() {
		return corpus("scalars");
	}

	public static JsonNode[] arrays() {
		return corpus("arrays");
	}

	public static JsonNode[] objects() {
		return corpus("objects");
	}

	private static JsonNode[] corpus(final String name) {
		final JsonNode array = SAMPLES.get(name);
		if (array == null || !array.isArray())
			throw new IllegalStateException(RESOURCE + " has no array named " + name);

		final JsonNode[] nodes = new JsonNode[array.size()];
		for (int i = 0; i < nodes.length; i++)
			nodes[i] = array.get(i);
		return nodes;
	}

	private static JsonNode load() {
		try (final InputStream in = JsonNodeSamples.class.getResourceAsStream(RESOURCE)) {
			if (in == null) {
				throw new IllegalStateException(RESOURCE + " is missing from the classpath");
			}
			return objectReader.readTree(in);
		} catch (final IOException e) {
			throw new IllegalStateException("Failed to read " + RESOURCE, e);
		}
	}
}
