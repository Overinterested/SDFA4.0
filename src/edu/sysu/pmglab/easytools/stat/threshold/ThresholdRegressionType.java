package edu.sysu.pmglab.easytools.stat.threshold;

/**
 * @author Wenjie Peng
 * @create 2024-12-19 01:18
 * @description
 */
public enum ThresholdRegressionType {
    HINGE,
    M01,
    M02,
    M03,
    UPPER_HINGE,
    M10,
    M20,
    M30,
    SEGMENTED,
    M11,
    M21,
    M31,
    M22,
    M22c,
    M33c;
    String description;

    public ThresholdRegressionType setDescription(String description) {
        this.description = description;
        return this;
    }
}
