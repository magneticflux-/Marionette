package com.marionette.jppf.extensions;

import org.jppf.load.balancer.BundleDataHolder;
import org.jppf.load.balancer.BundlePerformanceSample;
import org.jppf.load.balancer.impl.ProportionalBundler;
import org.jppf.load.balancer.impl.ProportionalProfile;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.utils.TypedProperties;
import org.jppf.utils.configuration.JPPFProperties;
import org.jppf.utils.configuration.JPPFProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkIndependentProportionalBundler extends ProportionalBundler {
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
     * Creates a new instance with the initial size of bundle as the start size.
     *
     * @param profile the parameters of the auto-tuning algorithm, grouped as a performance analysis profile.
     */
    public NetworkIndependentProportionalBundler(ProportionalProfile profile) {
        super(profile);
        bundleSize = -1;
    }

    @Override
    public void feedback(int size, double totalTime, double accumulatedElapsed, double overheadTime) {
        /* Copied from AbstractAdaptiveBundler */
        int n1 = size / nbThreads;
        int n2 = size % nbThreads;
        double meanTimePerTask = accumulatedElapsed / size;
        double t;
        if (n1 == 0) t = meanTimePerTask;
        else {
            t = n1 * meanTimePerTask;
            if (n2 > 0) t += meanTimePerTask * ((double) n2 / nbThreads);
        }
        //t += overheadTime;
        feedback(size, t);
    }

    @Override
    public void setChannelConfiguration(final JPPFSystemInformation nodeConfiguration) {
        super.setChannelConfiguration(nodeConfiguration);
        TypedProperties jppf = nodeConfiguration.getJppf();
        boolean isPeer = jppf.getBoolean("jppf.peer.driver", false);
        JPPFProperty prop = isPeer ? JPPFProperties.PEER_PROCESSING_THREADS : JPPFProperties.PROCESSING_THREADS;
        nbThreads = jppf.getInt(prop.getName(), 1);
        if (bundleSize < 0)
            bundleSize = nbThreads * profile.getInitialSize();
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

    private void computeBundleSizes() {
        synchronized (getBundlers()) {
            double maxMean = Double.NEGATIVE_INFINITY;
            double minMean = Double.POSITIVE_INFINITY;
            ProportionalBundler minBundler = null;
            double meanSum = 0.0d;
            for (ProportionalBundler b : getBundlers()) {
                BundleDataHolder h = b.getDataHolder();
                double m = h.getMean();
                if (m > maxMean) maxMean = m;
                if (m < minMean) {
                    minMean = m;
                    minBundler = b;
                }
            }
            for (ProportionalBundler b : getBundlers()) {
                BundleDataHolder h = b.getDataHolder();
                meanSum += normalize(h.getMean());
            }
            int max = maxSize();
            int sum = 0;
            for (ProportionalBundler b : getBundlers()) {
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
                for (ProportionalBundler b : getBundlers()) {
                    sb.append("  bundler #").append(b.getBundlerNumber()).append(" : bundleSize=").append(b.getBundleSize()).append(", ");
                    sb.append(b.getDataHolder()).append('\n');
                }
                log.trace(sb.toString());
            }
        }
    }
}
