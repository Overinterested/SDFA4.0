package edu.sysu.pmglab.sdfa.sv.vcf.format;

/**
 * @author Wenjie Peng
 * @create 2024-12-18 00:14
 * @description this class means "PHRED-scaled genotype likelihoods" in UKBB
 */
public class PLBox extends ThreeIntValueBox {
    @Override
    public String getBriefName() {
        return "PL";
    }

    @Override
    public String getDescription() {
        return "PHRED-scaled genotype likelihoods";
    }

    @Override
    public PLBox newInstance() {
        return new PLBox();
    }

    public int getSecMin(int sampleIndex) {
        int var1 = get(sampleIndex, 0);
        int var2 = get(sampleIndex, 1);
        int var3 = get(sampleIndex, 2);
        if (var1 <= var2) {
            // var1 <= var2
            if (var1 <= var3) {
                return -1;
            }
        } else {
            return -1;
        }
        return -1;
    }
}
