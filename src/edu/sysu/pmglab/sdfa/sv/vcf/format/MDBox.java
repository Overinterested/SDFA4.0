package edu.sysu.pmglab.sdfa.sv.vcf.format;

/**
 * @author Wenjie Peng
 * @create 2024-12-18 00:05
 * @description
 */
public class MDBox extends SingleIntValueBox{
    @Override
    public String getBriefName() {
        return "MD";
    }

    @Override
    public String getDescription() {
        return "Read depth of multiple alleles";
    }

    @Override
    public MDBox newInstance() {
        return new MDBox();
    }
}
