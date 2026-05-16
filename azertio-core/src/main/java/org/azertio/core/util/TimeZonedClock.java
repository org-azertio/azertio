package org.azertio.core.util;

import org.azertio.core.Clock;

import java.time.Instant;
import java.time.ZoneId;

public class TimeZonedClock implements Clock {

	private final ZoneId zone;

	public TimeZonedClock(ZoneId zone) {
		this.zone = zone;
	}

	@Override
	public Instant now() {
		return Instant.now();
	}

	@Override
	public ZoneId zone() {
		return zone;
	}

}