package de.leidenheit;

import java.rmi.UnexpectedException;
import java.util.HashMap;
import java.util.Map;

public class PolarCoordinateValueAngleRange  {
    
    private static PolarCoordinateValueAngleRange instance;

    private final HashMap<ValueRange, Integer> valueAngleRangeMap = new HashMap<>();

    public static PolarCoordinateValueAngleRange getInstance() {
        if (instance == null) {
            instance = new PolarCoordinateValueAngleRange();
        }
        return instance;
    }

    public PolarCoordinateValueAngleRange() {
        valueAngleRangeMap.put(new ValueRange(0.0001, 9.000), 6);
        valueAngleRangeMap.put(new ValueRange(9.0001, 27.000), 13);
        valueAngleRangeMap.put(new ValueRange(27.0001, 45.000), 4);
        valueAngleRangeMap.put(new ValueRange(45.0001, 63.000), 18);
        valueAngleRangeMap.put(new ValueRange(63.0001, 81.000), 1);
        valueAngleRangeMap.put(new ValueRange(81.0001, 99.000), 20);
        valueAngleRangeMap.put(new ValueRange(99.0001, 117.000), 5);
        valueAngleRangeMap.put(new ValueRange(117.0001, 135.000), 12);
        valueAngleRangeMap.put(new ValueRange(135.0001, 153.000), 9);
        valueAngleRangeMap.put(new ValueRange(153.0001, 171.000), 14);
        valueAngleRangeMap.put(new ValueRange(171.0001, 189.000), 11);
        valueAngleRangeMap.put(new ValueRange(189.0001, 207.000), 8);
        valueAngleRangeMap.put(new ValueRange(207.0001, 225.000), 16);
        valueAngleRangeMap.put(new ValueRange(225.0001, 243.000), 7);
        valueAngleRangeMap.put(new ValueRange(243.0001, 261.000), 19);
        valueAngleRangeMap.put(new ValueRange(261.0001, 279.000), 3);
        valueAngleRangeMap.put(new ValueRange(279.0001, 297.000), 17);
        valueAngleRangeMap.put(new ValueRange(297.0001, 315.000), 2);
        valueAngleRangeMap.put(new ValueRange(315.0001, 333.000), 15);
        valueAngleRangeMap.put(new ValueRange(333.0001, 351.000), 10);
        valueAngleRangeMap.put(new ValueRange(351.0001, 360.000), 6);
    }

    public Integer findValueByAngle(double angle) throws UnexpectedException{
        for (Map.Entry<ValueRange, Integer> entry :  valueAngleRangeMap.entrySet()) {
            final var inRange = entry.getKey().minValue <= angle 
                && entry.getKey().maxValue >= angle;
            if (inRange) {
                return entry.getValue();
            }
        }
        throw new UnexpectedException(String.format("Cannot find angle %s in range set", angle));
    }

    public HashMap<ValueRange, Integer> getValueAngleRangeMap() {
        return this.valueAngleRangeMap;
    }

    class ValueRange implements Comparable<ValueRange> {
        private double minValue;
        private double maxValue;

        public ValueRange(double minValue, double maxValue) {
            this.minValue = minValue;
            this.maxValue = maxValue;
        }

        public double getMinValue() {
            return minValue;
        }

        public double getMaxValue() {
            return maxValue;
        }

        @Override
        public int compareTo(ValueRange other) {
            if (this.minValue < other.minValue) {
                return -1;
            } else if (this.minValue > other.minValue) {
                return 1;
            } else {
                if (this.maxValue < other.maxValue) {
                    return -1;
                } else if (this.maxValue > other.maxValue) {
                    return 1;
                } else {
                    return 0;
                }
            }
        }

        @Override
        public String toString() {
            return "[" + minValue + ", " + maxValue + "]";
        }
    }
}