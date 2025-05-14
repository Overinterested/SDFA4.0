package edu.sysu.pmglab.sdfa.sv.vcf.format;

/**
 * @author Wenjie Peng
 * @create 2024-12-18 00:14
 * @description
 */
public class GQBox extends SingleIntValueBox{
    @Override
    public String getBriefName() {
        return "GQ";
    }

    @Override
    public String getDescription() {
        return "Genotype Quality";
    }

    @Override
    public GQBox newInstance() {
        return new GQBox();
    }
}
