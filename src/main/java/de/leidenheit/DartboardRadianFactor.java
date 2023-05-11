package de.leidenheit;

/**
 * Provides the factor values that represent offical the PDC 
 * radians for BullsEye, Bull, Triple and Double fields.
 * Dartsboard generally have a size of 451mm, including the outer black part.
 * In order to determine the radians the detected ellipse should be considered 
 * by applying the following relative factors:
 * 
 * Based on a dartboard of size 451mm which has a 340mm field ellipse:
 * - 170mm ->37,6940133037% is the radius of each quadrant which defines an outer double
 * - 107mm -> 23,7250554323% is the radius of a quadrants triple multiplier which defines an outer triple
 * - 8mm -> 1,77383592017% is the radius of each quadrants multiplier fields
 * - 31,8mm -> 7,05099778270% is the diameter of outer bull and divided by two definies the outer bull
 * - 12,7mm -> 2,815964523285% is the diameter of bullseye and divided by two definies the outer bullseye  
 */
public class DartboardRadianFactor {
    static final float BULLSEYE = 2.815964523285f / 2;
    static final float BULL = 7.05099778270f / 2;

    static final float MULTIPLYER = 1.77383592017f;

    static final float QUADRANT_OUTER_TRIPLE = 23.7250554323f;
    static final float QUADRANT_INNER_TRIPLE = QUADRANT_OUTER_TRIPLE - MULTIPLYER;

    static final float QUADRANT_OUTER_DOUBLE = 37.6940133037f;
    static final float QUADRANT_INNER_DOUBLE = QUADRANT_OUTER_DOUBLE - MULTIPLYER;
}
