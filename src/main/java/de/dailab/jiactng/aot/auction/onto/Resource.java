package de.dailab.jiactng.aot.auction.onto;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Different types of resources
 */
public enum Resource {
	
	/** the different resources */
	A, B, C, D, E, F, G, J, K;
	
	/** probability distribution for generating random resources */
	private static final List<Resource> DISTRIBUTION = Arrays.asList(
			A, A, A, A,
			B, B, B, B,
			C, C, D, D,
			E, F, G);
	
	/*
	 * HELPER METHODS FOR GENERATING RESOURCES AND RESOURCE BUNDLES
	 */
	
	/**
	 * Generate random resources using some probability distribution
	 */
	public static Resource getRandomResource(Random random) {
		return DISTRIBUTION.get(random.nextInt(DISTRIBUTION.size()));
	}

	/**
	 * Parse a bundle sequence in the form "ABC DEE FG ABCD ..."
	 */
	public static List<List<Resource>> parseBundleSequence(String sequence) {
		return Stream.of(sequence.split(" "))
				.map(Resource::parseBundle)
				.collect(Collectors.toList());
	}

	/**
	 * Parse a string like "ABCD" to a list of the respective resources
	 */
	public static List<Resource> parseBundle(String bundle) {
		return Stream.of(bundle.split(""))
				.map(c -> Resource.valueOf(c))
				.collect(Collectors.toList());
	}

	/**
	 * Generate a number of random resource bundles with random size and random items.
	 */
	public static List<List<Resource>> generateRandomBundles(int numBundles, int minBundleSize, int maxBundleSize, Random random) {
		return IntStream.range(0, numBundles)
				.mapToObj(i -> IntStream.range(0, random.nextInt(maxBundleSize + 1 - minBundleSize) + minBundleSize)
						.mapToObj(k -> getRandomResource(random))
						.collect(Collectors.toList()))
				.collect(Collectors.toList());
	}
	
	/**
	 * generate random bundle sequence, for testing
	 */
	public static void main(String... args) {
		List<List<Resource>> bundles = generateRandomBundles(150, 2, 4, new Random());
		
		int total = bundles.stream().mapToInt(b -> b.size()).sum();
		
		String sequence = bundles.stream()
				.map(b -> b.stream().map(Resource::toString).collect(Collectors.joining()))
				.collect(Collectors.joining(" "));
		
		Map<Resource, Long> counts = bundles.stream()
				.flatMap(List::stream)
				.collect(Collectors.groupingBy(r -> r, Collectors.counting()));

		System.out.println("Total:    " + total);
		System.out.println("Average:  " + total / (float) bundles.size());
		System.out.println("Sequence: " + sequence);
		System.out.println("Counts:   " + counts);
	}
	
}
