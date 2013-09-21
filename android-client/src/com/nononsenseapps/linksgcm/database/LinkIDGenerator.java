package com.nononsenseapps.linksgcm.database;

import java.util.Random;

public class LinkIDGenerator {

	// All hex strings
	static final String[] chars = { "0", "1", "2", "3", "4", "5", "6", "7",
			"8", "9", "a", "b", "c", "d", "e", "f" };

	public static String generateID() {
		Random r = new Random();
		StringBuilder sb = new StringBuilder();

		// Generate 8 hexchars, e.g. a random 32-bit number
		for (int i = 0; i < 8; i++) {
			sb.append(chars[r.nextInt(16)]);
		}
		return sb.toString();
	}
}
