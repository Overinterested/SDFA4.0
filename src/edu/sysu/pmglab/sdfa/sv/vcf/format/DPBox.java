package edu.sysu.pmglab.sdfa.sv.vcf.format;

/**
 * @author Wenjie Peng
 * @create 2024-12-19 19:47
 * @description
 */
public class DPBox extends SingleIntValueBox{
    @Override
    public String getBriefName() {
        return "DP";
    }

    @Override
    public String getDescription() {
        return "Approximate read depth";
    }

    @Override
    public DPBox newInstance() {
        return new DPBox();
    }
}
