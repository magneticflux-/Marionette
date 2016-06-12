package com.marionette.evolver.supermariobros.optimizationfunctions;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Mitchell on 3/24/2016.
 */
public class MarioBrosData implements Serializable {
    public final List<DataPoint> dataPoints;


    public MarioBrosData() {
        dataPoints = new LinkedList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MarioBrosData that = (MarioBrosData) o;

        return dataPoints.equals(that.dataPoints);

    }

    public int getMaxDistance() {
        return dataPoints.stream().mapToInt(dataPoint -> dataPoint.marioX).max().orElseThrow(Error::new);
    }

    public int getLastDistance() {
        return dataPoints.get(dataPoints.size() - 1).marioX;
    }

    public int getLastWorld() {
        return dataPoints.get(dataPoints.size() - 1).world;
    }

    public int getLastScore() {
        return dataPoints.get(dataPoints.size() - 1).score;
    }

    public int getMaxScore() {
        return dataPoints.stream().mapToInt(dataPoint -> dataPoint.score).max().orElseThrow(Error::new);
    }

    @Override
    public int hashCode() {
        return dataPoints.hashCode();
    }

    @Override
    public String toString() {
        return dataPoints.toString();
    }

    public void addDataPoint(DataPoint dataPoint) {
        this.dataPoints.add(dataPoint);
    }

    public static class DataPoint implements Serializable {
        public final int score, time, world, level, lives, marioX, marioY, marioState;

        @SuppressWarnings("unused")
        private DataPoint() {
            this(0, 0, 0, 0, 0, 0, 0, 0);
        }

        public DataPoint(int score, int time, int world, int level, int lives, int marioX, int marioY, int marioState) {
            this.score = score;
            this.time = time;
            this.world = world;
            this.level = level;
            this.lives = lives;
            this.marioX = marioX;
            this.marioY = marioY;
            this.marioState = marioState;
        }

        public DataPoint(DataPoint dataPoint) {
            this(dataPoint.score, dataPoint.time, dataPoint.world, dataPoint.level, dataPoint.lives, dataPoint.marioX, dataPoint.marioY, dataPoint.marioState);
        }

        @Override
        public String toString() {
            return "DataPoint{" +
                    "score=" + score +
                    ", time=" + time +
                    ", world=" + world +
                    ", level=" + level +
                    ", lives=" + lives +
                    ", marioX=" + marioX +
                    ", marioY=" + marioY +
                    ", marioState=" + marioState +
                    '}';
        }

        @SuppressWarnings("SimplifiableIfStatement")
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DataPoint dataPoint = (DataPoint) o;

            if (score != dataPoint.score) return false;
            if (time != dataPoint.time) return false;
            if (world != dataPoint.world) return false;
            if (level != dataPoint.level) return false;
            if (lives != dataPoint.lives) return false;
            if (marioX != dataPoint.marioX) return false;
            if (marioY != dataPoint.marioY) return false;
            return marioState == dataPoint.marioState;

        }

        @Override
        public int hashCode() {
            int result = score;
            result = 31 * result + time;
            result = 31 * result + world;
            result = 31 * result + level;
            result = 31 * result + lives;
            result = 31 * result + marioX;
            result = 31 * result + marioY;
            result = 31 * result + marioState;
            return result;
        }
    }
}
