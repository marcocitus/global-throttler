/**
 * 
 */
package trosc.throttler;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Class to stress test a global throttler by periodically kicking off mock
 * events.
 */
public class HammerGlobeThrottler {

	final ScheduledExecutorService scheduler;
	final GlobeThrottler throttler;
	final int hammerRate;

	public HammerGlobeThrottler(int hammerRate, GlobeThrottler throttler) throws IOException {
		this.scheduler = Executors.newScheduledThreadPool(16);
		this.throttler = throttler;
		this.hammerRate = hammerRate;
	}

	public void run() {
		if (hammerRate <= 0) {
			/* nothing to do */
			return;
		}
		
		long delay = (long) (1000. / hammerRate);
		
		/* 
		 * Try to fire off exactly at the start of the second
		 * to increase the chance of concurrent requests.
		 */
		long now = System.currentTimeMillis();
		long start = 1000 - now % 1000;
		
		scheduler.scheduleAtFixedRate(hitThrottler, start, delay, TimeUnit.MILLISECONDS);
	}

	final Runnable hitThrottler = new Runnable() {
		public void run() {
			long eventTime = System.currentTimeMillis();

			/* always hammer key 0 */
			Event event = new Event(0, eventTime);

			if (throttler.canProceed(event)) {
				System.out.printf("Proceed at %d\n", eventTime / 1000);
			} else {
				System.out.printf("Rejected at %d\n", eventTime / 1000);
			}
		}
	};

	public static void main(String[] argv) {
		Options options = new Options();

		Option maxRateOption = new Option("m", "max-rate", true, "maximum rate of events to allow");
		maxRateOption.setRequired(true);
		options.addOption(maxRateOption);

		Option hammerRateOption = new Option("h", "hammer-rate", true, "rate at which to hammer the throttler");
		hammerRateOption.setRequired(true);
		options.addOption(hammerRateOption);

		try {

			CommandLineParser parser = new DefaultParser();
			CommandLine commandLine = parser.parse(options, argv);

			int maxRate = 2;
			int hammerRate = 1;
			
			if(commandLine.hasOption("max-rate")) {
				maxRate = Integer.parseInt(commandLine.getOptionValue("max-rate"));
			}
			
			if(commandLine.hasOption("hammer-rate")) {
				hammerRate = Integer.parseInt(commandLine.getOptionValue("hammer-rate"));
			}

			GlobeThrottler throttler = new GlobeThrottler(maxRate);
			HammerGlobeThrottler hammer = new HammerGlobeThrottler(hammerRate, throttler);
			hammer.run();

		} catch (ParseException e) {
			HelpFormatter help = new HelpFormatter();
			help.printHelp(HammerGlobeThrottler.class.getName(), options, true);
			System.exit(1);
		} catch (IOException e) {
			System.err.println(e);
			System.exit(2);
		}
	}

}
