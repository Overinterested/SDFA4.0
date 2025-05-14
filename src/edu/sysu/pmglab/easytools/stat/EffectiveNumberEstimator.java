package edu.sysu.pmglab.easytools.stat;//package edu.sysu.pmglab.easytools.stat;
//
//import cern.colt.list.IntArrayList;
//import cern.colt.matrix.DoubleMatrix1D;
//import cern.colt.matrix.DoubleMatrix2D;
//import cern.colt.matrix.impl.DenseDoubleMatrix2D;
//import cern.colt.matrix.linalg.EigenvalueDecomposition;
//
//public enum EffectiveNumberEstimator {
//    INSTANCE;
//
//    public static double[] calculateEffectiveIndexesDiff(DoubleMatrix2D corrMat, int[] indexes) {
//        int size = indexes.length;
//        double[] effIndexes = new double[size];
//        effIndexes[0] = 1;
//        if (indexes.length == 1) {
//            return effIndexes;
//        }
//        IntArrayList selectedSampleIndex = new IntArrayList();
//        selectedSampleIndex.add(indexes[0]);
//        for (int i = 1; i < size; i++) {
//            selectedSampleIndex.add(indexes[i]);
//            effIndexes[i] = calculateEffectSampleSize(corrMat, selectedSampleIndex);
//        }
//
//        for (int i = size - 1; i > 0; i--) {
//            effIndexes[i] = (effIndexes[i] - effIndexes[i - 1]);
//            //  System.out.println(effIndexes[i]);
//        }
////        for (int i = 0; i < size; i++) {
////           effIndexes[i] /= (maxSize);
////        }
//        return (effIndexes);
//    }
//
//    public static double calculateEffectSampleSize(DoubleMatrix2D corrMat, IntArrayList selectedSampleIndex) {
//        // DoubleMatrix2D corrMat = ColtMatrixBasic.readMatrixFromFile("test.txt", originalSampleSize, originalSampleSize);
//        int newSampleSize = corrMat.rows();
//
//        if (selectedSampleIndex == null) {
//            selectedSampleIndex = new IntArrayList();
//            for (int i = 0; i < newSampleSize; i++) {
//                selectedSampleIndex.add(i);
//            }
//        }
//        if (selectedSampleIndex.isEmpty()) {
//            return 0;
//        }
//        newSampleSize = selectedSampleIndex.size();
//        double[][] subMatrix = new double[newSampleSize][newSampleSize];
//
//        // System.out.println("Removed columns and rows: " + highlyCorrIndexes.toString());
//        newSampleSize = selectedSampleIndex.size();
//
//        for (int i = 0; i < newSampleSize; i++) {
//            // tmpCorMat.setQuick(i, i, 1);
//            subMatrix[i][i] = 1;
//            for (int j = i + 1; j < newSampleSize; j++) {
////                     tmpCorMat.setQuick(i, j, poweredCorrMat.getQuick(selectedSampleIndex.getQuick(i), selectedSampleIndex.getQuick(j)));
////                    tmpCorMat.setQuick(j, i, tmpCorMat.getQuick(i, j));
//                subMatrix[i][j] = corrMat.getQuick(selectedSampleIndex.getQuick(i), selectedSampleIndex.getQuick(j));
//                subMatrix[i][j] = Math.abs(subMatrix[i][j]);
//                subMatrix[j][i] = subMatrix[i][j];
//            }
//        }
//        //  poweredCorrMat = tmpCorMat;
//        //  System.out.println(poweredCorrMat.toString());
//
//        if (newSampleSize == 1) {
//            return 1;
//        }
//
//        double effectSampleSize = newSampleSize;
//        long time = System.nanoTime();
//
//
//        //I found this function is less error-prone  than the  EigenDecompositionImpl 2.0 and slightly faster
//        // System.out.println(poweredCorrMat.toString());
//        //I found this function is the slowest
//        DoubleMatrix2D poweredCorrMat = new DenseDoubleMatrix2D(subMatrix);
//        EigenvalueDecomposition ed = new EigenvalueDecomposition(poweredCorrMat);
//        DoubleMatrix1D eVR = ed.getRealEigenvalues();
//        //DoubleMatrix1D eVI = ed.getImagEigenvalues();
//        // System.out.println(eVR.toString());
//        // System.out.println(eVI.toString());
//        //double effectSampleSize = newSampleSize;
//        for (int i = 0; i < newSampleSize; i++) {
//            if (Double.isNaN(eVR.get(i))) {
//                System.err.println("NaN error for eigen values!");
//            }
//            if (eVR.getQuick(i) > 1) {
//                effectSampleSize -= (eVR.getQuick(i) - 1);
//            }
//        }
//        time = System.nanoTime() - time;
//        //System.out.println("Clot Eigenvalues : " + eVR.toString());
//        //System.out.println("Effective sizes: " + effectSampleSize + " Time: " + time / 1000 + " us\n");
//
//
//        return (effectSampleSize);
//    }
//
//}
