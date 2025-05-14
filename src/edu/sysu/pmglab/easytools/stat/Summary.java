package edu.sysu.pmglab.easytools.stat;//package edu.sysu.pmglab.easytools.stat;
//
//import cern.colt.list.DoubleArrayList;
//import cern.jet.stat.Descriptive;
//import cern.jet.stat.Probability;
//import edu.sysu.pmglab.container.list.DoubleList;
//
///**
// * @author jianglin
// */
//public class Summary {
//
//    double mean(DoubleList vars) {
//        double vale = 0;
//        int size = vars.size();
//        for (int i = 0; i < size; i++) {
//            vale += vars.get(i);
//        }
//        return vale / size;
//    }
//
//    double stddev(DoubleList vars) {
//        double var = 0, val;
//        double mean = mean(vars);
//
//        int size = vars.size();
//        for (int i = 0; i < size; i++) {
//            val = vars.get(i) - mean;
//            val *= val;
//            var += val;
//        }
//        return var / (size - 1);
//    }
//
//    private static double sum(double[] a) {
//        double sum = 0.0;
//        for (int i = 0; i < a.length; i++) {
//            if (!Double.isNaN(a[i])) {
//                sum += a[i];
//            }
//        }
//        return sum;
//    }
//
//    public static void standWeight(double[] weight) {
//        double sumW = sum(weight);
//        for (int i = 0; i < weight.length; i++) {
//            weight[i] = weight[i] / sumW;
//        }
//    }
//
//    public static double stddev(double[] a, double[] weight) {
//
//        if (a.length == 0) {
//            return Double.NaN;
//        }
//
//        double avg = mean(a, weight, false);
//        double v2 = 0;
//        double sum = 0.0;
//        for (int i = 0; i < a.length; i++) {
//            if (!Double.isNaN(a[i])) {
//                v2 += weight[i] * weight[i];
//                sum += ((a[i] - avg) * (a[i] - avg) * weight[i]);
//            }
//        }
//        return Math.sqrt(sum / (1 - v2));
//    }
//
//    public static double mean(double[] a, double[] weight, boolean standizeWeight) {
//
//        if (a.length == 0) {
//            return Double.NaN;
//        }
//        if (standizeWeight) {
//            double sumW = sum(weight);
//            for (int i = 0; i < weight.length; i++) {
//                weight[i] = weight[i] / sumW;
//            }
//        }
//        double mean = 0;
//        for (int i = 0; i < weight.length; i++) {
//            if (!Double.isNaN(a[i])) {
//                mean += weight[i] * a[i];
//            }
//        }
//        return mean;
//    }
//
//    public static double stddevNadw(double[] a, double[] weight) {
//
//        if (a.length == 0) {
//            return Double.NaN;
//        }
//        double[] newW = new double[weight.length];
//        System.arraycopy(weight, 0, newW, 0, weight.length);
//
//        double avg = mean(a, newW, true);
//        double v2 = 0;
//        double sum = 0.0;
//        for (int i = 0; i < a.length; i++) {
//            v2 += newW[i] * newW[i];
//            sum += ((a[i] - avg) * (a[i] - avg) * newW[i]);
//        }
//        return Math.sqrt(sum / (1 - v2));
//    }
//
//    public static double mean(double[] vars) {
//        double vale = 0;
//        int size = vars.length;
//        int num = 0;
//        for (int i = 0; i < size; i++) {
//            if (Double.isNaN(vars[i])) {
//                continue;
//            }
//            vale += vars[i];
//            num++;
//        }
//        return vale / num;
//    }
//
//    public static double stddev(double[] vars) {
//        double var = 0, val;
//        double mean = mean(vars);
//
//        int size = vars.length;
//        for (int i = 0; i < size; i++) {
//            val = vars[i] - mean;
//            val *= val;
//            var += val;
//        }
//        return var / (size - 1);
//    }
//
//    public static double stddev(double[] vars, double mean) {
//        double var = 0, val;
//
//        int size = vars.length;
//        int num = 0;
//        for (int i = 0; i < size; i++) {
//            if (Double.isNaN(vars[i])) {
//                continue;
//            }
//            val = vars[i] - mean;
//            val *= val;
//            var += val;
//            num++;
//        }
//        return var / (num - 1);
//    }
//
//    //This is derived from PLINK
//    //Benjamini & Hochberg (1995)
//    public static double benjaminiHochbergFDR(double fdrThreshold, double[] sp) {
//
//        int ti = sp.length;
//        if (ti == 0) {
//            return fdrThreshold;
//        }
//        // BH
//
//        double[] pv_BH = new double[ti];
//        double t = (double) ti;
//
//        pv_BH[ti - 1] = sp[ti - 1];
//        double x = 0;
//        for (int i = ti - 2; i >= 0; i--) {
//            x = (t / (double) (i + 1)) * sp[i] < 1 ? (t / (double) (i + 1)) * sp[i] : 1;
//            pv_BH[i] = pv_BH[i + 1] < x ? pv_BH[i + 1] : x;
//        }
//        if (pv_BH[0] <= fdrThreshold) {
//            for (int i = 1; i < ti; i++) {
//                if (pv_BH[i] >= fdrThreshold) {
//                    return sp[i - 1];
//                }
//            }
//        }
//        return fdrThreshold / ti;
//    }
//
//
//    public static double MLFC(double fdrThreshold, double[] sp) {
//        int total = sp.length;
//        if (total == 0) {
//            return fdrThreshold;
//        }
//        //sp is a sorted list from 0
//        int start = 0;
//        double sum = 0;
//
//        while (!Double.isNaN(sp[start]) && sp[start] <= fdrThreshold) {
//            start++;
//        }
//
//        double effTotal = total - start;
//        int effecNum = 0;
//        double ratio;
//        int stopIndex = (int) (effTotal / 4);
//        for (; start < total; start++) {
//            if (Double.isNaN(sp[start])) {
//                continue;
//            }
//            ratio = sp[start] / ((effecNum + 1) / (effTotal + 1));
//            sum += Math.abs(Math.log(ratio) / Math.log(2.0));
//            effecNum++;
//            if (effecNum > stopIndex) {
//                break;
//            }
//        }
//        return sum / (effecNum);
//    }
//
//    public static double zScore(final double pValue) {
//        double q = 0;
//        // assume they are two-tailed I2-values
//        if (Double.isNaN(pValue)) {
//            return q;
//        }
//        q = pValue;
//        //the Probability.normalInverse could handle 1E-323 but cannot handle 1-(1E-323)
//        if (q > 0.5) {
//            q = 1 - q;
//            if (q < 1E-323) {
//                q = 1E-323;
//            }
//            q = Probability.normalInverse(q);
//            return -q;
//
//        } else {
//            if (q < 1E-323) {
//                q = 1E-323;
//            }
//            q = Probability.normalInverse(q);
//            return q;
//        }
//
//    }
//
//    public static double caculateInflationFactorDiff(double fdrThreshold, DoubleList pvalues) {
//        int start = 0;
//        while (!Double.isNaN(pvalues.get(start)) && pvalues.get(start) <= fdrThreshold) {
//            start++;
//        }
//        DoubleArrayList tmpDoubleList = new DoubleArrayList(pvalues.size() - start);
//
//        int size = pvalues.size();
//        double chiSquare = 0;
//        for (int i = start; i < size; i++) {
//            chiSquare = Summary.zScore(pvalues.get(i) / 2);// two tails to be one tail;
//            chiSquare = chiSquare * chiSquare;
//            tmpDoubleList.add(chiSquare);
//        }
//
//        tmpDoubleList.quickSort();
//        double quant = 0.5;
//        double chi = 0.4549364;
//        quant = 0.75;
//        chi = 1.323304;
//
//        double median = Descriptive.quantile(tmpDoubleList, quant);
//        tmpDoubleList.clear();
////        StringBuilder tmpBuffer = new StringBuilder();
////        tmpBuffer.append("The inflation factor(λ) is ");
////        median = median / chi;
////        tmpBuffer.append(Util.doubleToString(median, 4));
////        tmpBuffer.append("\n");
//        //System.out.println(tmpBuffer);
//        tmpDoubleList.clear();
//        //median = 1;
//        return Math.abs(median - 1);
//        /*
//        if (Math.abs(median - 1) > 1E-3) {
//            //to avoid numeric error only we only do it necessarily
//            for (int i = 0; i < size; i++) {
//                chiSquare = adjustedPValue[i];
//                chiSquare = Probability.chiSquareComplemented(1, chiSquare / median);
//                pvalues[i] = chiSquare;
//            }
//        }
//         */
//
//    }
//
//    public static double MLFC(double fdrThreshold, DoubleList sp) {
//        int total = sp.size();
//        if (total == 0) {
//            return fdrThreshold;
//        }
//        //sp is a sorted list from 0
//        int start = 0;
//        double sum = 0;
//
//        while (!Double.isNaN(sp.get(start)) && sp.get(start) <= fdrThreshold) {
//            start++;
//        }
//
//        double effTotal = total - start;
//        int effecNum = 0;
//        double ratio;
//        int stopIndex = (int) (effTotal / 4);
//        for (; start < total; start++) {
//            if (Double.isNaN(sp.get(start))) {
//                continue;
//            }
//            ratio = sp.get(start) / ((effecNum + 1) / (effTotal + 1));
//            sum += Math.abs(Math.log(ratio) / Math.log(2.0));
//            effecNum++;
//            if (effecNum > stopIndex) {
//                break;
//            }
//        }
//        return sum / (effecNum);
//    }
//
//    //This is derived from PLINK
//    //Benjamini & Hochberg (1995)
//    public static double benjaminiHochbergFDR(double fdrThreshold, DoubleList sp) {
//
//        int ti = sp.size();
//        if (ti == 0) {
//            return fdrThreshold;
//        }
//        // BH
//
//        double[] pv_BH = new double[ti];
//        double t = (double) ti;
//
//        pv_BH[ti - 1] = sp.get(ti - 1);
//        double x = 0;
//        for (int i = ti - 2; i >= 0; i--) {
//            x = (t / (double) (i + 1)) * sp.get(i) < 1 ? (t / (double) (i + 1)) * sp.get(i) : 1;
//            pv_BH[i] = pv_BH[i + 1] < x ? pv_BH[i + 1] : x;
//        }
//        if (pv_BH[0] <= fdrThreshold) {
//            for (int i = 1; i < ti; i++) {
//                if (pv_BH[i] >= fdrThreshold) {
//                    return sp.get(i - 1);
//                }
//            }
//        }
//        return fdrThreshold / ti;
//    }
//
//    //This is derived from PLINK
//    //Benjamini & Hochberg (1995)
//    public static double benjaminiHochbergFDR(double fdrThreshold, DoubleArrayList sp, DoubleArrayList adjustedP) {
//
//        int ti = sp.size();
//        if (ti == 0) {
//
//            return fdrThreshold;
//        }
//        // BH
//        adjustedP.setSize(ti);
//        double[] pv_BH = new double[ti];
//        double t = (double) ti;
//
//        pv_BH[ti - 1] = sp.getQuick(ti - 1);
//        adjustedP.setQuick(ti - 1, pv_BH[ti - 1]);
//        double x = 0;
//        for (int i = ti - 2; i >= 0; i--) {
//            x = (t / (double) (i + 1)) * sp.getQuick(i) < 1 ? (t / (double) (i + 1)) * sp.getQuick(i) : 1;
//            pv_BH[i] = pv_BH[i + 1] < x ? pv_BH[i + 1] : x;
//            adjustedP.setQuick(i, pv_BH[i]);
//        }
//        if (pv_BH[0] <= fdrThreshold) {
//            for (int i = 1; i < ti; i++) {
//                if (pv_BH[i] >= fdrThreshold) {
//
//                    return sp.getQuick(i - 1);
//                }
//            }
//        }
//        return fdrThreshold / ti;
//    }
//}
