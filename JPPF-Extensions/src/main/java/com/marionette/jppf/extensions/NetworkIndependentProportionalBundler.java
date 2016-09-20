package com.marionette.jppf.extensions;

import org.jppf.load.balancer.impl.ProportionalBundler;
import org.jppf.load.balancer.impl.ProportionalProfile;

public class NetworkIndependentProportionalBundler extends ProportionalBundler {
    /**
     * Creates a new instance with the initial size of bundle as the start size.
     *
     * @param profile the parameters of the auto-tuning algorithm, grouped as a performance analysis profile.
     */
    public NetworkIndependentProportionalBundler(ProportionalProfile profile) {
        super(profile);
    }

    @Override
    public void feedback(int size, double totalTime, double accumulatedElapsed, double overheadTime) {
        /* Copied from AbstractAdaptiveBundler */
        int n1 = size / nbThreads;
        int n2 = size % nbThreads;
        double mean = accumulatedElapsed / size;
        double t = 0d;
        if (n1 == 0) t = mean;
        else {
            t = n1 * mean;
            if (n2 > 0) t += mean * ((double) n2 / nbThreads);
        }
        //t += overheadTime;
        feedback(size, t);
    }
}
