package com.marionette.evolver.supermariobros.optimizationfunctions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Mitchell Skaggs on 3/24/2016.
 */
public class MarioBrosData implements Serializable {
    final List<DataPoint> dataPoints;


    public MarioBrosData() {
        dataPoints = new ArrayList<>();
    }

    @Override
    public int hashCode() {
        return dataPoints.hashCode();
    }

    public int getMaxDistance() {
        return dataPoints.stream().mapToInt(DataPoint::getMarioX).max().orElseThrow(Error::new);
    }

    public int getLastDistance() {
        return getLastDataPoint().getMarioX();
    }

    public DataPoint getLastDataPoint() {
        return dataPoints.get(dataPoints.size() - 1);
    }

    public int getLastScore() {
        return getLastDataPoint().getScore();
    }

    public int getMaxScore() {
        return dataPoints.stream().mapToInt(DataPoint::getScore).max().orElseThrow(Error::new);
    }

    public void addDataPoint(DataPoint dataPoint) {
        if (SMBNoveltySearch.FAST_NOVELTY_CALC)
            this.dataPoints.clear();
        this.dataPoints.add(dataPoint);
    }

    public static class DataPoint implements Serializable {
        final int[] values;

        @SuppressWarnings("unused")
        private DataPoint() {
            this(0, 0, 0, 0);
        }

        public DataPoint(int score, int marioX, int marioY, int marioState) {
            values = new int[]{score, marioX, marioY, marioState};
        }

        public DataPoint(DataPoint dataPoint) {
            this.values = Arrays.copyOf(dataPoint.values, 4);
        }

        public int getScore() {
            return values[0];
        }

        public int getMarioX() {
            return values[1];
        }

        public int getMarioY() {
            return values[2];
        }

        public int getMarioState() {
            return values[3];
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DataPoint dataPoint = (DataPoint) o;

            return Arrays.equals(values, dataPoint.values);

        }


        @Override
        public int hashCode() {
            return Arrays.hashCode(values);
        }


    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MarioBrosData data = (MarioBrosData) o;

        return dataPoints.equals(data.dataPoints);

    }


    @Override
    public String toString() {
        return dataPoints.toString();
    }


}
