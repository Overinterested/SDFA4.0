package edu.sysu.pmglab.easytools.stat;//package edu.sysu.pmglab.easytools.stat;
//
//import edu.sysu.pmglab.container.list.List;
//import edu.sysu.pmglab.executor.ThreadQueue;
//
//import java.util.Arrays;
//import java.util.Comparator;
//import java.util.HashSet;
//import java.util.Set;
//
//public enum RobustRegression {
//    INSTANCE;
//
//    class DoubleArrayListComparator implements Comparator<double[]> {
//
//        int index;
//
//        public DoubleArrayListComparator(int index) {
//            this.index = index;
//        }
//
//        @Override
//        public int compare(final double[] arg0, final double[] arg1) {
//            return Double.compare(arg0[index], arg1[index]);
//        }
//    }
//
//    public boolean iterativeWeighter(double[] data, double[] finalWeights, int maxIterN) {
//        int tissueSizeOrg = data.length;
//        int tissueSize;
//
//
//        double[][] x;
//        double[] y;
//        LinearRegression linReg = new LinearRegression();
//        double median, sd;
//        boolean success = false;
//        List<double[]> indexY = new List<>();
//        DoubleArrayListComparator cp = new DoubleArrayListComparator(0);
//        double[] coef = new double[2];
//        double diff, maxDiff, MINDIFF = 1e-8;
//
//        Arrays.fill(coef, 0);
//        for (int j = 0; j < tissueSizeOrg; j++) {
//            if (!Double.isNaN(data[j])) {
//                indexY.add(new double[]{data[j], j});
//            }
//        }
//
//        //because of missing values the tissue number may be diffrent
//        tissueSize = indexY.size();
//        if (tissueSize < 2) {
//            return success;
//        }
//        indexY.sort(cp);
//
//        x = new double[tissueSize][2];
//        y = new double[tissueSize];
//        double[] weights0 = new double[tissueSize];
//        double[] weights1 = new double[tissueSize];
//        Arrays.fill(weights0, 1);
//
//        for (int j = 0; j < tissueSize; j++) {
//            y[j] = indexY.get(j)[0];
//            x[j][0] = 1;
//            x[j][1] = j + 1;
//        }
//        int iter = 0;
//        do {
//            for (int j = 0; j < tissueSize; j++) {
//                y[j] = indexY.get(j)[0];
//            }
//            //iteratively standize the expression values untile the coefficients are converged
//            median = Summary.mean(y, weights0, true);
//            //sd = StdStats.stddev(residual);
//            sd = Summary.stddev(y, weights0);
//            for (int j = 0; j < tissueSize; j++) {
//                y[j] = (y[j] - median) / sd;
//                //convert to uniform distribution
//                if (y[j] <= 0) {
//                    // y[j] = Probability.normal(y[j]);
//                } else {
//                    // y[j] = 1 - Probability.normal(-y[j]);
//                }
//            }
//
//            success = linReg.robustLinearRegression(y, x, 100, 1);
//            if (!success) {
//                System.out.println(success);
//            }
//            //System.arraycopy(linReg.coef, 0, coef, 0, coef.length);
//            System.arraycopy(linReg.getWeights(), 0, weights1, 0, weights1.length);
//            Summary.standWeight(weights1);
//            maxDiff = Math.abs(weights0[0] - weights1[0]);
//            for (int i = 1; i < weights0.length; i++) {
//                diff = Math.abs(weights0[i] - weights1[i]);
//                if (diff > maxDiff) {
//                    maxDiff = diff;
//                }
//            }
//            if (maxDiff < MINDIFF) {
//                break;
//            }
//            System.arraycopy(weights1, 0, weights0, 0, weights0.length);
//            iter++;
//        } while (iter < maxIterN);
//        if (iter >= maxIterN) {
//            System.out.println("Over");
//        }
//        System.arraycopy(linReg.getWeights(), 0, weights0, 0, weights0.length);
//        double[] residual = linReg.getResiduals();
//        for (int j = 0; j < tissueSize; j++) {
//            // residues[(int) (indexY.get(j)[1])] = -residual[j];
//            finalWeights[(int) (indexY.get(j)[1])] = weights0[j];
//            //data[(int) (indexY.get(j)[1])] = y[j];
//        }
//
//        return success;
//    }
//
//    public void removeOutlierRow(List<String> regionLabelTrunc, List<double[]> scoreListTrunc, int startColIndex, int threadNum) {
//        int size = scoreListTrunc.size(), colNum = scoreListTrunc.get(0).length;
//        Set<Integer> outlierIndexes = new HashSet<>();
//        ThreadQueue threadPool = new ThreadQueue(threadNum);
//        double baseWeight = 0.01;
//        for (int j = startColIndex; j < colNum; j++) {
//            int finalJ = j;
//            threadPool.addTasks((status, context1) -> {
//                double[] weights = new double[size];
//                double[] values = new double[size];
//                Arrays.fill(weights, Double.NaN);
//                for (int i = 0; i < size; i++) {
//                    values[i] = scoreListTrunc.get(i)[finalJ];
//                }
//                RobustRegression.INSTANCE.iterativeWeighter(values, weights, 100);
//                for (int i = 0; i < size; i++) {
//                    if (weights[i] > baseWeight) {
//                        continue;
//                    }
//                    synchronized (threadPool) {
//                        outlierIndexes.add(i);
//                    }
//                }
//            });
//        }
//        threadPool.close();
//        if (!outlierIndexes.isEmpty()) {
//            List<String> regionLabelTrunc0 = new List<>();
//            List<double[]> scoreListTrunc0 = new List<>();
//            for (int i = 0; i < size; i++) {
//                if (outlierIndexes.contains(i)) {
//                    continue;
//                }
//                regionLabelTrunc0.add(regionLabelTrunc.get(i));
//                scoreListTrunc0.add(scoreListTrunc.get(i));
//            }
//            regionLabelTrunc.clear();
//            scoreListTrunc.clear();
//            regionLabelTrunc.addAll(regionLabelTrunc0);
//            scoreListTrunc.addAll(scoreListTrunc0);
//            System.out.println(outlierIndexes.size()+" removed: "+ List.wrap(outlierIndexes.toArray()).toString(","));
//        }
//    }
//
//}
