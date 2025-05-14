package edu.sysu.pmglab.sdfa.sv.vcf.quantitycontrol.gty;

/**
 * 哈迪温伯格平衡计算
 */
public class HardyWeinbergCalculator {
    /**
     * 常量, 在 plink_common.h 中定义
     */
    private static final double SMALL_EPSILON = 0.00000000000005684341886080801486968994140625d;
    private static final double EXACT_TEST_BIAS = 0.00000000000000000000000010339757656912845935892608650874535669572651386260986328125d;

    /**
     * 私有构造器，防止实例化
     */
    private HardyWeinbergCalculator() {
        throw new UnsupportedOperationException("Cannot instantiate utility class");
    }

    /**
     * Calculates the Hardy-Weinberg Equilibrium (HWE) p-value for a given set of observed genotypes.
     * <p>
     * This implementation is based on the work by Christopher Chang, which is available in the
     * PLINK-NG project (https://github.com/chrchang/plink-ng) in the `plink_stats.c` file.
     * The Java version of this code was translated by Liubin Zhang.
     * <p>
     * Reference:
     * Wigginton, J. E., Cutler, D. J., & Abecasis, G. R. (2005). A note on exact tests of Hardy-Weinberg equilibrium. The American Journal of Human Genetics, 76(5), 887-893.
     * Graffelman, J., & Moreno, V. (2013). The mid p-value in exact tests for Hardy-Weinberg equilibrium. Statistical applications in genetics and molecular biology, 12(4), 433-448.
     *
     * @param obsAA the number of observed homozygous AA genotypes
     * @param obsAB the number of observed heterozygous AB genotypes
     * @param obsBB the number of observed homozygous BB genotypes
     * @param midp  true to use the mid-p correction, false for the standard exact test
     * @return the Hardy-Weinberg Equilibrium p-value
     */
    public static double calculate(int obsAA, int obsAB, int obsBB, boolean midp) {
        long obsHomc, obsHomr;
        if (obsAA < obsBB) {
            obsHomc = obsBB;
            obsHomr = obsAA;
        } else {
            obsHomc = obsAA;
            obsHomr = obsBB;
        }
        long rareCopies = 2L * obsHomr + obsAB;
        long genotypes2 = (obsAB + obsHomc + obsHomr) * 2L;
        int tieCount = 1;
        double currHetsT2 = obsAB;
        double currHomrT2 = obsHomr;
        double currHomcT2 = obsHomc;
        double tailp = (1 - SMALL_EPSILON) * EXACT_TEST_BIAS;
        double centerp = 0;
        double lastP2 = tailp;
        double lastP1 = tailp;
        double currHetsT1;
        double currHomrT1;
        double currHomcT1;
        double preAddP;

        if (genotypes2 == 0) {
            return midp ? 0.5 : 1.0;
        }

        if (obsAB * genotypes2 > rareCopies * (genotypes2 - rareCopies)) {
            // tail 1 = upper
            while (currHetsT2 > 1.5) {
                // het_probs[curr_hets] = 1
                // het_probs[curr_hets - 2] = het_probs[curr_hets] * curr_hets * (curr_hets - 1.0)
                currHomrT2 += 1;
                currHomcT2 += 1;
                lastP2 *= (currHetsT2 * (currHetsT2 - 1)) / (4 * currHomrT2 * currHomcT2);
                currHetsT2 -= 2;
                if (lastP2 < EXACT_TEST_BIAS) {
                    if (lastP2 > (1 - 2 * SMALL_EPSILON) * EXACT_TEST_BIAS) {
                        tieCount++;
                    }
                    tailp += lastP2;
                    break;
                }
                centerp += lastP2;
                if (centerp == Double.POSITIVE_INFINITY) {
                    return 0;
                }
            }
            if ((centerp == 0) && (!midp)) {
                return 1;
            }
            while (currHetsT2 > 1.5) {
                currHomrT2 += 1;
                currHomcT2 += 1;
                lastP2 *= (currHetsT2 * (currHetsT2 - 1)) / (4 * currHomrT2 * currHomcT2);
                currHetsT2 -= 2;
                preAddP = tailp;
                tailp += lastP2;
                if (tailp <= preAddP) {
                    break;
                }
            }
            currHetsT1 = obsAB + 2;
            currHomrT1 = obsHomr;
            currHomcT1 = obsHomc;
            while (currHomrT1 > 0.5) {
                // het_probs[curr_hets + 2] = het_probs[curr_hets] * 4 * curr_homr * curr_homc / ((curr_hets + 2) * (curr_hets + 1))
                lastP1 *= (4 * currHomrT1 * currHomcT1) / (currHetsT1 * (currHetsT1 - 1));
                preAddP = tailp;
                tailp += lastP1;
                if (tailp <= preAddP) {
                    break;
                }
                currHetsT1 += 2;
                currHomrT1 -= 1;
                currHomcT1 -= 1;
            }
        } else {
            // tail 1 = lower
            while (currHomrT2 > 0.5) {
                currHetsT2 += 2;
                lastP2 *= (4 * currHomrT2 * currHomcT2) / (currHetsT2 * (currHetsT2 - 1));
                currHomrT2 -= 1;
                currHomcT2 -= 1;
                if (lastP2 < EXACT_TEST_BIAS) {
                    if (lastP2 > (1 - 2 * SMALL_EPSILON) * EXACT_TEST_BIAS) {
                        tieCount++;
                    }
                    tailp += lastP2;
                    break;
                }
                centerp += lastP2;
                if (centerp == Double.POSITIVE_INFINITY) {
                    return 0;
                }
            }
            if ((centerp == 0) && (!midp)) {
                return 1;
            }
            while (currHomrT2 > 0.5) {
                currHetsT2 += 2;
                lastP2 *= (4 * currHomrT2 * currHomcT2) / (currHetsT2 * (currHetsT2 - 1));
                currHomrT2 -= 1;
                currHomcT2 -= 1;
                preAddP = tailp;
                tailp += lastP2;
                if (tailp <= preAddP) {
                    break;
                }
            }
            currHetsT1 = obsAB;
            currHomrT1 = obsHomr;
            currHomcT1 = obsHomc;
            while (currHetsT1 > 1.5) {
                currHomrT1 += 1;
                currHomcT1 += 1;
                lastP1 *= (currHetsT1 * (currHetsT1 - 1)) / (4 * currHomrT1 * currHomcT1);
                preAddP = tailp;
                tailp += lastP1;
                if (tailp <= preAddP) {
                    break;
                }
                currHetsT1 -= 2;
            }
        }

        if (!midp) {
            return tailp / (tailp + centerp);
        } else {
            return (tailp - ((1 - SMALL_EPSILON) * EXACT_TEST_BIAS * 0.5) * tieCount) / (tailp + centerp);
        }
    }
}
