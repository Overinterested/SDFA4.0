package edu.sysu.pmglab.easytools.stat.threshold;

import edu.sysu.pmglab.container.interval.FloatInterval;

/**
 * @author Wenjie Peng
 * @create 2024-12-19 01:25
 * @description
 */
public class SimpleUpperHinge {
    float[] x;
    float[] y;
    float betaX;
    float intercept;
    float step = 500;
    float estimateForThreshold;
    FloatInterval searchIntervalForThreshold;


    public static SimpleUpperHinge init(float[] x, float[] y) {
        SimpleUpperHinge simpleUpperHinge = new SimpleUpperHinge();
        simpleUpperHinge.x = x;
        simpleUpperHinge.y = y;

        return simpleUpperHinge;
    }

    public static SimpleUpperHinge init(float[] x, float[] y, FloatInterval searchIntervalForThreshold) {
        SimpleUpperHinge simpleUpperHinge = new SimpleUpperHinge();
        simpleUpperHinge.x = x;
        simpleUpperHinge.y = y;
        simpleUpperHinge.searchIntervalForThreshold = searchIntervalForThreshold;
        return simpleUpperHinge;
    }

    public SimpleUpperHinge setStep(float step) {
        this.step = step;
        return this;
    }
}
