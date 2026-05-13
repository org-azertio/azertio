package org.azertio.core.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;



public class StringDistance {

	private StringDistance() {
		// avoid instantiation
	}


	public static List<String> closerStrings(
			String string,
			Collection<String> candidates,
			int limitResults
	) {
		Comparator<Pair<String, Double>> greaterDistance = Comparator.comparing(Pair::right);
		var stream = candidates.stream()
			.map(candidate -> Pair.of(candidate, calculateDistance(string, candidate)))
			.sorted(greaterDistance.reversed());
		if (limitResults >= 0) {
			stream = stream.limit(limitResults);
		}
		return stream.map(Pair::left).toList();
	}


	/**
	 * Returns a similarity score in [0.0, 1.0] between two strings,
	 * based on the normalized Levenshtein distance.
	 * 1.0 means identical; 0.0 means completely different.
	 */
	private static double calculateDistance(String a, String b) {
		if (a.equals(b)) return 1.0;
		int la = a.length();
		int lb = b.length();
		if (la == 0 || lb == 0) return 0.0;

		int[] prev = new int[lb + 1];
		int[] curr = new int[lb + 1];
		for (int j = 0; j <= lb; j++) prev[j] = j;

		for (int i = 1; i <= la; i++) {
			curr[0] = i;
			for (int j = 1; j <= lb; j++) {
				int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
				curr[j] = Math.min(Math.min(prev[j] + 1, curr[j - 1] + 1), prev[j - 1] + cost);
			}
			int[] tmp = prev; prev = curr; curr = tmp;
		}

		return 1.0 - (double) prev[lb] / Math.max(la, lb);
	}

}