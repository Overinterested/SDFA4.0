package edu.sysu.pmglab.sdfa.nagf;

/**
 * @author Wenjie Peng
 * @create 2024-11-15 01:19
 * @description
 */
public enum NAGFMode {
    SV_Level(false),
    Multi_VCF(true),
    One_Population_VCF(true);

    boolean mapSample;

    NAGFMode(boolean mapSample) {
        this.mapSample = mapSample;
    }

    public boolean mapSample() {
        return mapSample;
    }
}
