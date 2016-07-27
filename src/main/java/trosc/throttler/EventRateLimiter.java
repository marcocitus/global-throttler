/**
 * 
 */
package trosc.throttler;

import java.util.LinkedList;
import java.util.Queue;

/**
 * EventRateLimiter decides whether an event can proceed based on a configured
 * maximum rate (events per second).
 */
public class EventRateLimiter {

	final Queue<Event> events;
	final int maxRate;

	public EventRateLimiter(int maxRate) {
		this.events = new LinkedList<Event>();
		this.maxRate = maxRate;
	}

	/*
	 * Force an event to be counted even if the queue is greater than maxRate.
	 * This is used to count external events.
	 */
	public synchronized void forceCount(Event event) {
		events.add(event);
	}

	/*
	 * Add an event to the queue if it still has space and return whether the
	 * event was added.
	 */
	public synchronized boolean canProceed(Event event) {
		decay();

		if (events.size() < maxRate) {
			events.add(event);
			return true;
		}

		return false;
	}

	/*
	 * Clear all events that are older than a second.
	 */
	public void decay() {
		long oneSecondAgo = System.currentTimeMillis() - 1000;

		decay(oneSecondAgo);
	}

	/*
	 * Clears all events up to decayTime.
	 */
	public synchronized void decay(long decayTime) {
		while (!events.isEmpty() && events.peek().eventTime <= decayTime) {
			events.remove();
		}
	}

	/*
	 * Return the number of events from the last second.
	 */
	public synchronized int rate() {
		decay();

		return events.size();
	}

}
