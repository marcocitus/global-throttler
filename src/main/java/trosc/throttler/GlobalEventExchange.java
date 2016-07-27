package trosc.throttler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * GlobalEventExchange broadcasts events to and receives events from instances
 * of GlobalEventExchange on other servers using UDP messages.
 * 
 * The list of other servers need to be defined in nodes.txt as plain text
 * IPs (recommended) or host names.
 */
public class GlobalEventExchange {

	final MultiEventRateLimiter counters;
	final DatagramSocket serverSocket;
	final List<String> hostNames;
	final int portNumber;

	volatile boolean active;

	public GlobalEventExchange(MultiEventRateLimiter counters) throws IOException {
		this.counters = counters;
		this.portNumber = 12033;
		this.serverSocket = new DatagramSocket(portNumber);
		this.active = false;
		this.hostNames = new ArrayList<String>();
		loadNodes();
	}

	void loadNodes() throws IOException {
		Path path = Paths.get("nodes.txt");

		try (Stream<String> lines = Files.lines(path)) {
			lines.forEach(host -> {
				if (!host.trim().isEmpty()) {
					hostNames.add(host);
				}
			});
		}
	}

	/*
	 * Start a background thread to listen for UDP messages. 
	 */
	public void start() {
		this.active = true;

		new Thread(receiver).start();
	}

	final Runnable receiver = new Runnable() {

		public void run() {
			byte[] receiveData = new byte[1024];

			while (active) {
				try {
					DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
					serverSocket.receive(receivePacket);

					byte[] rawData = receivePacket.getData();
					processMessage(rawData);

				} catch (IOException e) {
					System.err.println(e.getMessage());
				}
			}
		}
	};

	/*
	 * Stop listening for UDP messages.
	 */
	public void stop() {
		this.active = false;

		serverSocket.close();
	}

	/*
	 * Broadcast an event to other nodes.
	 */
	public void broadcast(Event event) {
		broadcast(buildMessage(event));
	}

	void broadcast(byte[] data) {
		for (String host : hostNames) {
			send(data, host, portNumber);
		}
	}

	void send(byte[] data, String host, int port) {
		try {
			InetAddress address = InetAddress.getByName(host);
			DatagramPacket sendPacket = new DatagramPacket(data, data.length, address, port);
			serverSocket.send(sendPacket);
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}

	byte[] buildMessage(Event event) {
		try {
			ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
			DataOutputStream dataOutput = new DataOutputStream(byteOutput);

			dataOutput.writeLong(event.key);
			dataOutput.writeLong(event.eventTime);

			byte[] rawData = byteOutput.toByteArray();
			return rawData;
		} catch (IOException e) {
			throw new Error(e);
		}
	}

	void processMessage(byte[] rawData) throws IOException {
		ByteArrayInputStream byteInput = new ByteArrayInputStream(rawData);
		DataInputStream dataInput = new DataInputStream(byteInput);

		long key = dataInput.readLong();
		long eventTime = dataInput.readLong();

		EventRateLimiter rateLimiter = counters.getRateLimiter(key);
		long currentTime = System.currentTimeMillis();
		Event remoteEvent = new Event(key, currentTime);

		/*
		 * An event happened somewhere else in the world, we should add this to
		 * the queue regardless of whether it is full, since we don't know for
		 * sure whether it occurred.
		 */
		rateLimiter.forceCount(remoteEvent);
	}

	public InetAddress getAddress() {
		return serverSocket.getLocalAddress();
	}
}
