package edu.sysu.pmglab.sdfa.sv.vcf.format;

/**
 * @author Wenjie Peng
 * @create 2024-12-18 00:13
 * @description this class means "Number of reads that support non-reference haplotype that are proper pairs" in UKBB
 */
public class PPBox extends SingleIntValueBox{
    @Override
    public String getBriefName() {
        return "PP";
    }

    @Override
    public String getDescription() {
        return "Number of reads that support non-reference haplotype that are proper pairs";
    }

    @Override
    public SingleIntValueBox newInstance() {
        return new PPBox();
    }
}
