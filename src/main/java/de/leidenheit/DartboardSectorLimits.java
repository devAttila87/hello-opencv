package de.leidenheit;

public record DartboardSectorLimits(
    int radiusBullsEyeLimit,
    int radiusBullLimit,
    int radiusInnerTripleLimit,
    int radiusOuterTripleLimit,
    int radiusInnerDoubleLimit,
    int radiusOuterDoubleLimit
) {}
