package edu.sysu.pmglab.sdfa.sv.vcf.format;

/**
 * @author Wenjie Peng
 * @create 2024-12-18 00:03
 * @description
 */
public class ADBox extends TwoIntValueBox{

    @Override
    public String getBriefName() {
        return "AD";
    }

    @Override
    public String getDescription() {
        return "Allelic depths for the ref and alt alleles in the order listed";
    }

    @Override
    public ADBox newInstance() {
        return new ADBox();
    }

}
