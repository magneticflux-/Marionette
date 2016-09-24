package com.marionette.jppf.extensions;

import org.jppf.load.balancer.AbstractAdaptiveBundler;
import org.jppf.load.balancer.BundleDataHolder;
import org.jppf.load.balancer.BundlePerformanceSample;
import org.jppf.load.balancer.impl.ProportionalBundler;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.utils.TypedProperties;
import org.jppf.utils.configuration.JPPFProperties;
import org.jppf.utils.configuration.JPPFProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class NetworkIndependentProportionalBundler extends AbstractAdaptiveBundler<NetworkIndependentProportionalProfile> {
    /**
     * Mapping of individual bundler to corresponding performance data - global.
     */
    private static final Set<NetworkIndependentProportionalBundler> BUNDLERS = new HashSet<>();
    private static final String DEFAULT_JOB_TYPE = "DefaultJobBundleDataHolder";
    /**
     * Logger for this class.
     */
    private static Logger log = LoggerFactory.getLogger(ProportionalBundler.class);
    /**
     * Determines whether debug level is set for logging.
     */
    private static boolean debugEnabled = log.isTraceEnabled();
    /**
     * Determines whether trace level is set for logging.
     */
    private static boolean traceEnabled = log.isTraceEnabled();
    /**
     * Mapping of individual bundler to corresponding performance data - local.
     */
    private final Set<NetworkIndependentProportionalBundler> bundlers = BUNDLERS;
    /**
     * Bounded memory of the past performance updates, divided by job type
     */
    private final Map<String, BundleDataHolder> dataHolders = new HashMap<>();
    private final Map<String, Integer> bundleSizes = new HashMap<>();
    private final Supplier<BundleDataHolder> defaultBundleDataHolderSupplier;
    private final int networkOverheadDivisor;

    /**
     * Creates a new instance with the initial size of bundle as the start size.
     *
     * @param profile the parameters of the auto-tuning algorithm, grouped as a performance analysis profile.
     */
    public NetworkIndependentProportionalBundler(final NetworkIndependentProportionalProfile profile) {
        super(profile);
        defaultBundleDataHolderSupplier = () -> new BundleDataHolder(profile.getPerformanceCacheSize(), profile.getInitialMeanTime());
        dataHolders.putIfAbsent(DEFAULT_JOB_TYPE, defaultBundleDataHolderSupplier.get());
        bundleSizes.putIfAbsent(DEFAULT_JOB_TYPE, profile.getInitialSize());
        networkOverheadDivisor = profile.getNetworkOverheadDivisor();
        if (debugEnabled)
            log.debug("Bundler#" + bundlerNumber + ": Using proportional bundle size - the initial size is " + bundleSize + ", profile: " + profile);
    }

    @Override
    public void feedback(int size, double totalTime, double accumulatedElapsed, double overheadTime) {
        int n1 = size / nbThreads;
        int n2 = size % nbThreads;
        double meanTimePerTask = accumulatedElapsed / size;
        double t;
        if (n1 == 0) t = meanTimePerTask;
        else {
            t = n1 * meanTimePerTask;
            if (n2 > 0) t += meanTimePerTask * ((double) n2 / nbThreads);
        }
        t += overheadTime / networkOverheadDivisor;
        feedback(size, t);
    }

    @Override
    public void setChannelConfiguration(final JPPFSystemInformation nodeConfiguration) {
        super.setChannelConfiguration(nodeConfiguration);
        TypedProperties jppf = nodeConfiguration.getJppf();
        boolean isPeer = jppf.getBoolean("jppf.peer.driver", false);
        JPPFProperty prop = isPeer ? JPPFProperties.PEER_PROCESSING_THREADS : JPPFProperties.PROCESSING_THREADS;
        nbThreads = jppf.getInt(prop.getName(), 1);
    }

    @Override
    public void feedback(final int size, final double time) {
        if (traceEnabled)
            log.trace("Bundler#" + bundlerNumber + ": new performance sample [size=" + size + ", time=" + (long) time + ']');
        if (size <= 0) return;
        BundlePerformanceSample sample = new BundlePerformanceSample(time / size, size);
        synchronized (getBundlers()) {
            getDataHolder().addSample(sample);
            computeBundleSizes();
        }
    }

    @Override
    public int getBundleSize() {
        return bundleSizes.getOrDefault(job.getName(), 1);
    }

    /**
     * Set the current size of bundle.
     *
     * @param size the bundle size as an int value.
     */
    private void setBundleSize(final int size) {
        bundleSizes.put(job.getName(), size <= 0 ? 1 : size);
        //bundleSize = size <= 0 ? 1 : size;
    }

    private void computeBundleSizes() {
        synchronized (getBundlers()) {
            double maxMean = Double.NEGATIVE_INFINITY;
            double minMean = Double.POSITIVE_INFINITY;
            NetworkIndependentProportionalBundler minBundler = null;
            double meanSum = 0.0d;
            for (NetworkIndependentProportionalBundler b : getBundlers()) {
                BundleDataHolder h = b.getDataHolder();
                double m = h.getMean();
                if (m > maxMean) maxMean = m;
                if (m < minMean) {
                    minMean = m;
                    minBundler = b;
                }
            }
            for (NetworkIndependentProportionalBundler b : getBundlers()) {
                BundleDataHolder h = b.getDataHolder();
                meanSum += normalize(h.getMean());
            }
            int max = maxSize();
            int sum = 0;
            for (NetworkIndependentProportionalBundler b : getBundlers()) {
                BundleDataHolder h = b.getDataHolder();
                double p = normalize(h.getMean()) / meanSum;
                int size = Math.max(1, (int) (p * max));
                if (size >= max) size = max - 1;
                b.setBundleSize(size);
                sum += size;
            }
            if ((sum < max) && (minBundler != null)) {
                int size = minBundler.getBundleSize();
                minBundler.setBundleSize(size + (max - sum));
            }
            if (traceEnabled) {
                StringBuilder sb = new StringBuilder();
                sb.append("bundler info:\n");
                sb.append("  minMean=").append(minMean).append(", maxMean=").append(maxMean).append(", maxSize=").append(max).append('\n');
                for (NetworkIndependentProportionalBundler b : getBundlers()) {
                    sb.append("  bundler #").append(b.getBundlerNumber()).append(" : bundleSize=").append(b.getBundleSize()).append(", ");
                    sb.append(b.getDataHolder()).append('\n');
                }
                log.trace(sb.toString());
            }
        }
    }

    /**
     * Get local mapping of individual bundler to corresponding performance data.
     *
     * @return a {@code Set<AbstractProportionalBundler>}.
     */
    private Set<NetworkIndependentProportionalBundler> getBundlers() {
        return bundlers;
    }

    /**
     * Perform context-independent initializations.
     */
    @Override
    public void setup() {
        synchronized (bundlers) {
            bundlers.add(this);
        }
    }

    /**
     * Release the resources used by this bundler.
     */
    @Override
    public void dispose() {
        super.dispose();
        synchronized (bundlers) {
            bundlers.remove(this); // Should be enough to kill any references to this
        }
    }

    /**
     * Get the bounded memory of the past performance updates.
     *
     * @return a BundleDataHolder instance.
     */
    private BundleDataHolder getDataHolder() {
        dataHolders.putIfAbsent(job.getName(), defaultBundleDataHolderSupplier.get());
        return dataHolders.get(job.getName());
    }

    /**
     * @param x .
     * @return .
     */
    private double normalize(final double x) {
        double r = x;
        for (int i = 1; i < profile.getProportionalityFactor(); i++)
            r *= x;
        return 1 / r;
    }
}
