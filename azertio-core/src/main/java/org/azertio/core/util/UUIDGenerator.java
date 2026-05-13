package org.azertio.core.util;

import com.github.f4b6a3.ulid.UlidCreator;
import java.util.UUID;

public class UUIDGenerator {

	 private UUIDGenerator() {
	  /* This utility class should not be instantiated */
	 }


	public static UUID generateUUID() {
		return UlidCreator.getUlid().toUuid();
	}
}
