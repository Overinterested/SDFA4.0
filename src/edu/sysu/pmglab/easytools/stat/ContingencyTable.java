package edu.sysu.pmglab.easytools.stat;//package edu.sysu.pmglab.easytools.stat;
//
//import cern.jet.random.Binomial;
//import cern.jet.random.engine.DRand;
//import cern.jet.stat.Gamma;
//import cern.jet.stat.Probability;
//
///**
// * See HELP string or run with no arguments for usage.
// * <p>
// * The code used to calculate a Fisher p-value comes originally from a
// * <a href=http://www.advancedmcode.org/myfisher22.html">Matlab program</a>
// * by Giuseppe Cardillo
// *
// * @author Miaoxin Li
// * @date 2010/09/08
// */
//public class ContingencyTable {
//
//    public static final int PHI_COEFFICIENT = 1;
//    public static final int FISHER_1TAILED = 2;
//    public static final int FISHER_2TAILED = 4;
//    private static final int WIDTH = 7;
//    private static final int DECIMALS = 3;
//
//    public static void main(String[] args) throws Exception {
//        int tests = 0;
//        String filename = null;
//        /*
//         %        control case
//         %          ___________
//         %   risk  |  A  |  B  |Rs1
//         %         |_____|_____|
//         %   wild  |  C  |  D  |Rs2
//         %         |_____|_____|____
//         %           Cs1  Cs2    N
//         */
//
//        long[][] counts = new long[][]{{2, 9582}, {460, 9582}};
//        //  counts = new long[][]{{18240, 35}, {93, 15}};
//
//        double s = ContingencyTable.pearsonChiSquared22(counts);
//        System.out.println(Probability.chiSquareComplemented(1, s));
//        System.out.println(ContingencyTable.fisherExact22(counts, 2, 2, 2));
//        System.out.println(ContingencyTable.chiSquareTest(counts));
//
//        /*
//         int cs1 = 42, cs2 = 30, A = 0, C = cs1;
//         for (int B = 1; B < 10; B++) {
//         int D = cs2 - B;
//         counts[0][0] = A;
//         counts[0][1] = B;
//         counts[1][0] = C;
//         counts[1][1] = D;
//         System.out.println(B + " " + ContingencyTable.pearsonChiSquared22(counts));
//         }
//         *
//         */
//        System.out.println(ContingencyTable.binomialPValueGreater(34, 3116, 0.011921459));
//        System.out.println(ContingencyTable.binomialPValueTwoTailed(49, 235, 1.0 / 6));
//        System.out.println(Probability.poissonComplemented(0, 0.02));
//        //long[][] counts1 = {{122, 158, 100}, {59, 125, 19}};
//        // long[][] counts1 = {{122, 158}, {59, 125}};
//        long[][] counts1 = {{460, 9582}, {460, 9582}};
//
//        System.out.println(ContingencyTable.chiSquareTest(counts1));
//        System.out.println(Probability.chiSquareComplemented(2, ContingencyTable.chiSquareTest(counts1)));
//    }
//
//    public ContingencyTable() {
//    }
//
//    public static double chiSquareTest(final long[][] counts) {
//        int nRows = counts.length;
//        int nCols = counts[0].length;
//
//        // compute row, column and total sums
//        double[] rowSum = new double[nRows];
//        double[] colSum = new double[nCols];
//        double total = 0.0d;
//        for (int row = 0; row < nRows; row++) {
//            for (int col = 0; col < nCols; col++) {
//                rowSum[row] += counts[row][col];
//                colSum[col] += counts[row][col];
//                total += counts[row][col];
//            }
//        }
//
//        // compute expected counts and chi-square
//        double sumSq = 0.0d;
//        double expected = 0.0d;
//        for (int row = 0; row < nRows; row++) {
//            for (int col = 0; col < nCols; col++) {
//                expected = (rowSum[row] * colSum[col]) / total;
//                sumSq += ((counts[row][col] - expected)
//                        * (counts[row][col] - expected)) / expected;
//            }
//        }
//        double p = Probability.chiSquareComplemented((nRows - 1) * (nCols - 1), sumSq);
//        return p;
//
//    }
//
//    /**
//     * Compute the Pearson's Chi-Squared test values for two set of data.
//     *
//     * @return The Pearson's Chi-Squared value.
//     */
//    public static final double pearsonChiSquared22(long[][] readCountsInt) {
//        double chiSquared = readCountsInt[0][0] * readCountsInt[1][1] - readCountsInt[0][1] * readCountsInt[1][0];
//        chiSquared = chiSquared * chiSquared;
//        chiSquared /= (readCountsInt[0][0] + readCountsInt[0][1]);
//        chiSquared /= (readCountsInt[1][0] + readCountsInt[1][1]);
//        chiSquared /= (readCountsInt[0][0] + readCountsInt[1][0]);
//        chiSquared /= (readCountsInt[0][1] + readCountsInt[1][1]);
//        chiSquared *= (readCountsInt[0][0] + readCountsInt[0][1] + readCountsInt[1][0] + readCountsInt[1][1]);
//        return chiSquared;
//    }
//
//    /**
//     * Fill in the count1 and count2 arrays.
//     */
//    public static final double fisherExact22(long[][] contigencyTable22, int rowNum, int colNum, int tails) {
//        //  int rowNum = contigencyTable22.length;
//        // int colNum = contigencyTable22[0].length;
//        int[] Rs = new int[rowNum];
//        int[] Cs = new int[colNum];
//        int totalAccount = 0;
//        for (int i = 0; i < rowNum; i++) {
//            for (int j = 0; j < colNum; j++) {
//                Rs[i] += contigencyTable22[i][j];
//                totalAccount += contigencyTable22[i][j];
//            }
//        }
//
//        for (int i = 0; i < colNum; i++) {
//            for (int j = 0; j < rowNum; j++) {
//                Cs[i] += contigencyTable22[j][i];
//            }
//        }
//        //sort  and rearrange matrix
//        int minIndex = 0;
//        int minData = 0;
//        long[] tmpArray = null;
//        for (int i = 0; i < rowNum; i++) {
//            minData = Rs[i];
//            minIndex = i;
//            for (int j = i; j < rowNum; j++) {
//                if (minData > Rs[j]) {
//                    minData = Rs[j];
//                    minIndex = j;
//                }
//            }
//
//            if (minIndex != i) {
//                Rs[minIndex] = Rs[i];
//                Rs[i] = minData;
//                tmpArray = new long[colNum];
//                for (int k = 0; k < colNum; k++) {
//                    tmpArray[k] = contigencyTable22[minIndex][k];
//                    contigencyTable22[minIndex][k] = contigencyTable22[i][k];
//                    contigencyTable22[i][k] = tmpArray[k];
//                }
//            }
//        }
//
//        for (int i = 0; i < colNum; i++) {
//            minData = Cs[i];
//            minIndex = i;
//            for (int j = i; j < colNum; j++) {
//                if (minData > Cs[j]) {
//                    minData = Cs[j];
//                    minIndex = j;
//                }
//            }
//
//            if (minIndex != i) {
//                Cs[minIndex] = Cs[i];
//                Cs[i] = minData;
//                tmpArray = new long[rowNum];
//                for (int k = 0; k < rowNum; k++) {
//                    tmpArray[k] = contigencyTable22[k][minIndex];
//                    contigencyTable22[k][minIndex] = contigencyTable22[k][i];
//                    contigencyTable22[k][i] = tmpArray[k];
//                }
//            }
//        }
//
//        int possibleA = Math.min(Rs[0], Cs[0]) + 1;  //all possible values for the first cell
//        //http://www.advancedmcode.org/myfisher22.html
//        double D, B, C;
//        double[] np = new double[possibleA];
//        //LOG(X!)=GAMMALN(x+1)
//        np[0] = Gamma.logGamma(Rs[1] + 1) + Gamma.logGamma(Cs[1] + 1) - Gamma.logGamma(totalAccount + 1) - Gamma.logGamma(Cs[1] - Rs[0] + 1);
//        double observedP = 0.0;
//        double tail2P = 0;
//        double tail1DownP = 0;
//        double tail1UpP = 0;
//        for (int A = 1; A < possibleA; A++) {
//            D = Math.log(Rs[1] - Cs[0] + A);//    z=[A;Rs(2)-Cs(1)+A;Rs(1)-A;Cs(1)-A;]; %all possible tables
//            B = Math.log(Rs[0] - A + 1);
//            C = Math.log(Cs[0] - A + 1);
//            np[A] = np[A - 1] + B + C - (Math.log(A) + D);
//        }
//        for (int A = 0; A < possibleA; A++) {
//            np[A] = Math.exp(np[A]);
//        }
//        observedP = np[(int) contigencyTable22[0][0]];
//        for (int A = 0; A < possibleA; A++) {
//            if (A < contigencyTable22[0][0]) {
//                tail1DownP += np[A];
//            } else if (A > contigencyTable22[0][0]) {
//                tail1UpP += np[A];
//            }
//            if (np[A] <= observedP) {
//                tail2P += np[A];
//            }
//        }
//        tail1DownP += np[(int) contigencyTable22[0][0]];
//        tail1UpP += np[(int) contigencyTable22[0][0]];
//
//        //System.out.println(tail2P);
//        //System.out.println(tail1DownP);
//        //System.out.println(tail1UpP);
//        if (tails == 2) {
//            return tail2P > 1 ? 1 : tail2P;
//        } else if (tails == 1) {
//            //warning may be not correc
//            return Math.min(tail1UpP > 1 ? 1 : tail2P, tail1UpP > 1 ? 1 : tail1UpP);
//        } else {
//            return tail2P;
//        }
//        // Lancaster?? correction
//        //tail2P = 0.5
//    }
//
//    public static double binomialPValueGreater(double k, double n, double p) {
//        if (n == 0 || p <= 0) {
//            return Double.NaN;
//        }
//        double pval = 1;
//        Binomial b = new Binomial((int) Math.ceil(n), p, new DRand());
//        pval = b.cdf(((int) Math.ceil(k)) - 1);
//        return (1 - pval);
//    }
//
//    //I do not know why this produce slightly different p-values from binom.test(51,235,(1/6),alternative="two.sided") in R
//    public static double binomialPValueTwoTailed(double k, double n, double p) {
//        if (n == 0) {
//            return Double.NaN;
//        }
//        double pval = 1;
//        Binomial b = new Binomial((int) Math.ceil(n), p, new DRand());
//        pval = 1 - b.cdf(((int) Math.ceil(k)) - 1);
//        pval += (1 - b.cdf(((int) Math.ceil(k))));
//        return (pval);
//    }
//}
