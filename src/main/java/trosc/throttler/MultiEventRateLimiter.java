package trosc.throttler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*
 * MultiEventRateLimiter is a data structure to store and retrieve
 * EventRateLimiter instances in a thread-safe manner.
 */
public class MultiEventRateLimiter {

	final Map<Long,EventRateLimiter> counters;
	final int maxRate;
	
	public MultiEventRateLimiter(int maxRate) {
		this.counters = new ConcurrentHashMap<Long,EventRateLimiter>();
		this.maxRate = maxRate;
	}

	public EventRateLimiter getRateLimiter(long key) {
		EventRateLimiter counter = counters.get(key);
		
		if (counter == null) {
			counter = addRateLimiter(key);
		}
		
		return counter;
	}
	
	synchronized EventRateLimiter addRateLimiter(long key) {
		EventRateLimiter counter = counters.get(key);
		
		if (counter == null) {
			counter = new EventRateLimiter(maxRate);
			counters.put(key, counter);
		}
		
		return counter;
	}

}
