package edu.sysu.pmglab.sdfa.sv.vcf.format;

/**
 * @author Wenjie Peng
 * @create 2024-12-18 00:32
 * @description which means "High-quality reference reads"
 */
public class DRBox extends SingleIntValueBox {
    @Override
    public String getBriefName() {
        return "DR";
    }

    @Override
    public String getDescription() {
        return "High-quality reference reads";
    }

    @Override
    public DRBox newInstance() {
        return new DRBox();
    }
}
