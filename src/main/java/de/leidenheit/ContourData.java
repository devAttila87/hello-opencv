package de.leidenheit;

import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Rect;

public record ContourData(
    long approxSize,
    double area,
    MatOfPoint2f approx,
    Rect boundingBox,
    MatOfPoint contour
) {}