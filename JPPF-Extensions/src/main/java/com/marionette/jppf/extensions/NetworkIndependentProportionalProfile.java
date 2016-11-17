package com.marionette.jppf.extensions;

import org.jppf.load.balancer.impl.ProportionalProfile;
import org.jppf.utils.TypedProperties;

/**
 * Created by Mitchell Skaggs on 9/24/2016.
 */
public class NetworkIndependentProportionalProfile extends ProportionalProfile {

    private int networkOverheadDivisor;

    public NetworkIndependentProportionalProfile(TypedProperties config) {
        super(config);
        networkOverheadDivisor = config.getInt("networkOverheadDivisor", 1);
    }

    public int getNetworkOverheadDivisor() {
        return networkOverheadDivisor;
    }

    public void setNetworkOverheadDivisor(int networkOverheadDivisor) {
        this.networkOverheadDivisor = networkOverheadDivisor;
    }
}
