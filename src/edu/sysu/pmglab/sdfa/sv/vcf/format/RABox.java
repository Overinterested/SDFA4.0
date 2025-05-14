package edu.sysu.pmglab.sdfa.sv.vcf.format;

/**
 * @author Wenjie Peng
 * @create 2024-12-18 00:13
 * @description Total read depth of the reference allele and all alternative alleles, including reads that support more than one allele.
 */
public class RABox extends TwoIntValueBox{

    @Override
    public String getBriefName() {
        return "RA";
    }

    @Override
    public String getDescription() {
        return "Total read depth of the reference allele and all alternative alleles, including reads that support more than one allele";
    }

    @Override
    public TwoIntValueBox newInstance() {
        return new RABox();
    }
}
