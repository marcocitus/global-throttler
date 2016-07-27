package trosc.throttler;

import java.io.IOException;

/*
 * GlobeThrottler decides whether the rate of global events stays under a 
 * particular maximum rate.
 */
public class GlobeThrottler {

	final GlobalEventExchange broadcaster;
	final MultiEventRateLimiter counters;
	final int waitTimeMs;

	public GlobeThrottler(int maxRate) throws IOException {
		this.counters = new MultiEventRateLimiter(maxRate);
		this.broadcaster = new GlobalEventExchange(counters);
		this.broadcaster.start();
		this.waitTimeMs = 150;
	}

	/*
	 * Checks whether the event with the given key can proceed.
	 * 
	 * This function may block for up to 150ms check for other global events.
	 */
	public boolean canProceed(Event event) {
		EventRateLimiter rateLimiter = counters.getRateLimiter(event.key);

		/*
		 * Try to count the event, if the counter is already full, it means
		 * we've already seen the maximum number of events over the last second
		 * and we bail out early.
		 */
		if (!rateLimiter.canProceed(event)) {
			return false;
		}

		/*
		 * Report the event to other throttling nodes.
		 */
		broadcaster.broadcast(event);

		/*
		 * Wait for events at other throttling nodes to make their way across
		 * time and space.
		 */
		try {
			Thread.sleep(waitTimeMs);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		/*
		 * If we saw additional events that filled up the queue, then reject
		 * this event. In the future, we could apply some prioritization
		 * mechanism (e.g. lowest event time can proceed).
		 */
		return rateLimiter.rate() <= rateLimiter.maxRate;
	}

}
