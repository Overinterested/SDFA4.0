package edu.sysu.pmglab.sdfa.sv.vcf.format;

/**
 * @author Wenjie Peng
 * @create 2024-12-18 00:16
 * @description which means "High-quality variant reads"
 */
public class DVBox extends SingleIntValueBox {
    @Override
    public String getBriefName() {
        return "DV";
    }

    @Override
    public String getDescription() {
        return "High-quality variant reads";
    }

    @Override
    public DVBox newInstance() {
        return new DVBox();
    }
}
