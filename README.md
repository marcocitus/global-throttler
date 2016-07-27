# GlobeThrottler

GlobeThrottler is a proof-of-concept java library that can throttle global events down to 1/s or higher with minimal decision time. It can be deployed in a set of globally distributed servers to ensure events do not exceed a pre-configured rate globally. 

When an event occurs, GlobleThrottler sends UDP packets to other nodes, which can typically be done in under 150ms using only tier-1 and tier-2 networks. Because GlobeThrottler waits 150ms to take a decision, a node with event A has a high likelihood of seeing all global events that preceeded A before taking a throttling decisions, and other nodes have a high likelihood of seeing A before taking theirs.

GlobeThrottler is probabilistic in the sense that long network delays and failures may cause it to miss certain events, temporarily allowing higher rates under certain scenarios. However, this can be made largely independent of rate violations, and redundancy can be added to minimize the probability of failure.

# Installation

To compile GlobeThrottler, use the following steps:

    $ git clone https://github.com/marcocitus/globe-throttler.git
    ...
    $ cd global-throttler
    $ mvn install
    ...

The maven builds a fat jar that can be easily copied and deployed to other machines:

    $ java -jar target/throttler-1.0.jar 
    usage: trosc.throttler.HammerGlobalThrottler -h <arg> -m <arg>
     -h,--hammer-rate <arg>   rate at which to hammer the throttler
     -m,--max-rate <arg>      maximum rate of events to allow

# Usage

To use GlobeThrottler, you need to define a nodes.txt file containing the IP addresses (DNS names not recommended due to additional latency) of other throttling nodes with one IP per line.

GlobleThrottler comes with a testing tool that generates events at a fixed rate. Below is an example of generating 4 events per second (2 per second on each node) with a limit of 3.

Node 62.0.0.1:

    $ echo 53.0.0.1 > nodes.txt
    $ java -jar target/throttler-1.0.jar -h 2 -m 3
    Proceed at 1469633438
    Proceed at 1469633438
    Proceed at 1469633439

On node 53.0.0.1:

    $ echo 62.0.0.1 > nodes.txt
    $ java -jar target/throttler-1.0.jar -h 2 -m 3
    Proceed at 1469633438
    Rejected at 1469633438
    Rejected at 1469633439
