package com.marionette.evolver.supermariobros.optimizationfunctions;

import org.jppf.node.protocol.DataProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Mitchell Skaggs on 6/24/2016.
 */
public class HashMapDataProvider implements DataProvider {
    private final Map<Object, Object> parameters = new HashMap<>();

    @Override
    public <T> T getParameter(final Object key) {
        //noinspection unchecked
        return (T) parameters.get(key);
    }

    @Override
    public <T> T getParameter(final Object key, final T defaultValue) {
        //noinspection unchecked
        T res = (T) parameters.get(key);
        return res == null ? defaultValue : res;
    }

    @Override
    public void setParameter(final Object key, final Object value) {
        parameters.put(key, value);
    }

    @Override
    public <T> T removeParameter(final Object key) {
        //noinspection unchecked
        return (T) parameters.remove(key);
    }

    @Override
    public Map<Object, Object> getAll() {
        return parameters;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + parameters;
    }

    @Override
    public void clear() {
        parameters.clear();
    }
}
