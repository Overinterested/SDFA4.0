package edu.sysu.pmglab.easytools.stat;//package edu.sysu.pmglab.easytools.stat;//package edu.sysu.pmglab.stat;
////
////import java.io.File;
////import java.io.FileNotFoundException;
////import java.util.ArrayList;
////import java.util.Scanner;
////
////import static org.bytedeco.openblas.global.openblas.*;
////
////
////public class Logit {
////    int n;
////    int m;
////    float[] coef;
////    float[] Y;
////    float[] X;
////    double priorCorrectionFactor = 1.0;
////
////    public Logit() {
////    }
////
////
////    public void setX(float[] X, int n, int m) {
////        this.X = X;
////        this.n = n;
////        this.m = m;
////    }
////
////
////    public void setY(float[] Y) {
////        this.Y = Y;
////    }
////
////    public void fitLM1() {
////        coef = new float[m];
////        float[] error;
////        float[] yPre = new float[n];
////        float[] coefSum = new float[n];
////        float[] EMat = getEMatrix(n);
////        float learningRate = 0.02f;
////        int maxCycle = 1000;
////        float[] delta = new float[m];
////        float tolerateDiff = 1e-6F;
////        boolean flag = false;
////        int count = 0;
////        while (!flag) {
////            // coefSum[n * 1] = 1 * x[n * m] * coef [m * 1] + 0 * coefSum[n*1]
////            cblas_sgemv(CblasRowMajor, CblasNoTrans, n, m, 1.0f, X, m, coef, 1, 0.0f, coefSum, 1);
////
////            sigmoid(yPre, coefSum);
////
////            error = yPre.clone();
////            // error[n * 1] = y[n * 1] - yPre[n * 1]
////            cblas_sgemv(CblasRowMajor, CblasNoTrans, n, n, 1.0f, EMat, n, Y, 1, -1.0f, error, 1);
////
////            // delta[m * 1] = T(x[n * m]) * error[n*1]
////            cblas_sgemv(CblasRowMajor, CblasTrans, n, m, 1.0f, X, m, error, 1, 0.0f, delta, 1);
////
////            flag = checkTolerateDiff(delta, coef, tolerateDiff, learningRate);
////            count += 1;
////            if (maxCycle <= count || flag) break;
////        }
////    }
////
////    public boolean checkTolerateDiff(float[] deltas, float[] coef, float tolerateDiff, float learningRate) {
////        boolean flag = false;
////        float delta = 0;
////        int nP = deltas.length;
////        for (int j = 0; j < nP; j++) {
////            delta += Math.abs(deltas[j]);
////            coef[j] += learningRate * deltas[j];
////        }
////        if (delta < tolerateDiff) {
////            flag = true;
////        }
////        return flag;
////    }
////
////
////    public void sigmoid(float[] yPre, float[] coefSum) {
////        for (int i = 0; i < yPre.length; i++) {
////            yPre[i] = (float) (Math.exp(coefSum[i]) / (1 + Math.exp(coefSum[i])));
////        }
////    }
////
////    public float[] getEMatrix(int n) {
////        float[] matrix = new float[n * n];
////        for (int i = 0; i < n; i++) {
////            matrix[i * (n + 1)] = 1;
////        }
////        return matrix;
////    }
////
////    public double getGivenProbability(float[] scores) {
////        double p = 0;
////        for (int i = 0; i < m; i++) {
////            p += (coef[i] * scores[i]);
////        }
////        p = 1 + priorCorrectionFactor * Math.exp(-p);
////        return 1.0 / p;
////    }
////
////
////    public static void main(String[] args) throws FileNotFoundException {
////        long start = System.currentTimeMillis();
////        Scanner scanner = new Scanner(new File("/Volumes/Data/Downloads/dataset.txt"));
////        ArrayList<double[]> xArray = new ArrayList<>();
////        ArrayList<Integer> yArray = new ArrayList<>();
////        while (scanner.hasNextLine()) {
////            String line = scanner.nextLine();
////            if (line.startsWith("#")) {
////                continue;
////            }
////            String[] columns = line.split("\\s+");
////
////            // skip first column and last column is the label
////            int i = 1;
////            double[] data = new double[columns.length - 2];
////            for (i = 1; i < columns.length - 1; i++) {
////                data[i - 1] = Double.parseDouble(columns[i]);
////            }
////            int label = Integer.parseInt(columns[i]);
////            xArray.add(data);
////            yArray.add(label);
////        }
////        float[] y = new float[yArray.size()];
////        for (int i = 0; i < y.length; i++) {
////            y[i] = yArray.get(i);
////        }
////
////        int n = xArray.size();
////        int m = xArray.get(0).length;
////        float[] x = new float[xArray.size() * xArray.get(0).length];
////        for (int i = 0; i < n; i++) {
////            for (int j = 0; j < m; j++) {
////                x[i * m + j] = (float) xArray.get(i)[j];
////            }
////        }
////        Logit logit = new Logit();
////        logit.setX(x, n, m);
////        logit.setY(y);
////        logit.fitLM1();
////        double result = logit.getGivenProbability(new float[]{1, 0, 1, 0, 0});
////        System.out.println(result);
////        System.out.println(System.currentTimeMillis() - start);
////    }
////}
////
////
