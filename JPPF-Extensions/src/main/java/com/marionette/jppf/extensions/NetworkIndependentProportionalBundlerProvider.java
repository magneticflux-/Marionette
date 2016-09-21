package com.marionette.jppf.extensions;

import org.jppf.load.balancer.Bundler;
import org.jppf.load.balancer.impl.ProportionalProfile;
import org.jppf.load.balancer.spi.JPPFBundlerProvider;
import org.jppf.utils.TypedProperties;

/**
 * Created by Mitchell on 9/20/2016.
 */
public class NetworkIndependentProportionalBundlerProvider implements JPPFBundlerProvider<ProportionalProfile> {
    @Override
    public String getAlgorithmName() {
        return "niproportional";
    }

    @Override
    public Bundler<ProportionalProfile> createBundler(ProportionalProfile profile) {
        return new NetworkIndependentProportionalBundler(profile);
    }

    @Override
    public ProportionalProfile createProfile(TypedProperties configuration) {
        return new ProportionalProfile(configuration);
    }
}
