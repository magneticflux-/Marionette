package com.marionette.jppf.extensions;

import org.jppf.load.balancer.Bundler;
import org.jppf.load.balancer.spi.JPPFBundlerProvider;
import org.jppf.utils.TypedProperties;

/**
 * Created by Mitchell on 9/20/2016.
 */
public class NetworkIndependentProportionalBundlerProvider implements JPPFBundlerProvider<NetworkIndependentProportionalProfile> {
    @Override
    public String getAlgorithmName() {
        return "niproportional";
    }

    @Override
    public Bundler<NetworkIndependentProportionalProfile> createBundler(NetworkIndependentProportionalProfile profile) {
        return new NetworkIndependentProportionalBundler(profile);
    }

    @Override
    public NetworkIndependentProportionalProfile createProfile(TypedProperties configuration) {
        return new NetworkIndependentProportionalProfile(configuration);
    }
}
