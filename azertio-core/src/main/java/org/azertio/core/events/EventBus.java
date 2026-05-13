package org.azertio.core.events;

import org.azertio.core.contributors.EventObserver;
import java.util.ArrayList;
import java.util.List;

public class EventBus {

	private final List<EventObserver> observers = new ArrayList<>();

	public void registerObserver(EventObserver observer) {
		observers.add(observer);
	}


	public void publish(Event event) {
		for (EventObserver observer : observers) {
			observer.onEvent(event);
		}
	}


}
