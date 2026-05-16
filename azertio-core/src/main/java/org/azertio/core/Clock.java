package org.azertio.core;

import java.time.Instant;
import java.time.ZoneId;

public interface Clock {

	Instant now();

	default ZoneId zone() {
		return ZoneId.systemDefault();
	}

}
