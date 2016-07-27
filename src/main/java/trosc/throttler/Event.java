package trosc.throttler;

public class Event {
	
	public final long key;
	public final long eventTime;

	public Event(long key, long eventTime) {
		this.key = key;
		this.eventTime = eventTime;
	}

}
