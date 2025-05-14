package edu.sysu.pmglab.easytools.stat;//package edu.sysu.pmglab.easytools.stat;
//
//import cern.jet.random.Uniform;
//import cern.jet.random.engine.RandomEngine;
//
//import java.util.Arrays;
//import java.util.Comparator;
//import java.util.Random;
//import java.util.concurrent.ThreadLocalRandom;
//
//public class SpearmanRankCorrelation {
//    public static void main(String[] args) {
//        /* Read and save input */
//        double[] X = new double[]{1, 2, 3, 4.5, 5, 6, 7, 8, 9, 100};
//        double[] Y = new double[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
//
//
//        System.out.format("%.3f", spearman(X, Y));
//    }
//
//    static void shuffleArray(int[] ar) {
//        // If running on Java 6 or older, use `new Random()` on RHS here
//        Random rnd = ThreadLocalRandom.current();
//        for (int i = ar.length - 1; i > 0; i--) {
//            int index = rnd.nextInt(i + 1);
//            // Simple swap
//            int a = ar[index];
//            ar[index] = ar[i];
//            ar[i] = a;
//        }
//    }
//
//    /* Calculates Spearman's rank correlation coefficient, */
//    public static double spearman(double[] X, double[] Y) {
//        /* Error check */
//        if (X == null || Y == null || X.length != Y.length) {
//            return 0;
//        }
//        int n = X.length;
//
//        double[] X1, Y1;
//        int maxN = 100000000;
//        if (n > maxN) {
//            int[] ids = new int[n];
//            for (int i = 0; i < n; i++) {
//                ids[i] = i;
//            }
//            RandomEngine tm = new cern.jet.random.engine.MersenneTwister(new java.util.Date());
//            Uniform um = new Uniform(tm);
//            for (int i = n - 1; i > 0; i--) {
//                int index = um.nextIntFromTo(0, i);
//                // Simple swap
//                int a = ids[index];
//                ids[index] = ids[i];
//                ids[i] = a;
//            }
//            X1 = new double[maxN];
//            Y1 = new double[maxN];
//            for (int i = 0; i < maxN; i++) {
//                X1[i] = X[ids[i]];
//                Y1[i] = Y[ids[i]];
//            }
//        } else {
//            X1 = X;
//            Y1 = Y;
//        }
//
//        /* Create Rank arrays */
//        int[] rankX = getRanks(X1);
//        int[] rankY = getRanks(Y1);
//
//        /* Apply Spearman's formula */
//        n = X1.length;
//        double squaretN = Math.sqrt(n) * Math.sqrt(n - 1) * Math.sqrt(n + 1);
//        double numerator = 0;
//        for (int i = 0; i < n; i++) {
//            numerator += Math.pow((rankX[i] - rankY[i]) / squaretN, 2);
//        }
//        numerator *= 6;
//        return 1 - numerator;
//    }
//
//    /* Returns a new array with ranks. Assumes unique array values. */
//    private static int[] getRanks(double[] array) {
//        int n = array.length;
//
//        /* Create Pair[] and sort by values */
//        Pair[] pair = new Pair[n];
//        for (int i = 0; i < n; i++) {
//            pair[i] = new Pair(i, array[i]);
//        }
//        Arrays.sort(pair, new PairValueComparator());
//
//        /* Create and return ranks[] */
//        int[] ranks = new int[n];
//        int rank = 1;
//        for (Pair p : pair) {
//            ranks[p.index] = rank++;
//        }
//        return ranks;
//    }
//
//    /* A class to store 2 variables */
//    private static class Pair {
//        public final int index;
//        public final double value;
//
//        public Pair(int i, double v) {
//            index = i;
//            value = v;
//        }
//    }
//
//    /* This lets us sort Pairs based on their value field */
//    private static class PairValueComparator implements Comparator<Pair> {
//        @Override
//        public int compare(Pair p1, Pair p2) {
//            if (p1.value < p2.value) {
//                return -1;
//            } else if (p1.value > p2.value) {
//                return 1;
//            } else {
//                return 0;
//            }
//        }
//    }
//}
