package edu.sysu.pmglab.easytools.stat;//package edu.sysu.pmglab.easytools.stat;
//
//import cern.colt.matrix.DoubleMatrix2D;
//import cern.colt.matrix.impl.DenseDoubleMatrix2D;
//import cern.colt.matrix.linalg.QRDecomposition;
//
//import java.util.Arrays;
//
//public class LinearRegression {
//    private DoubleMatrix2D beta;  // regression coefficients
//    private double sse;         // sum of squared
//    private double sst;         // sum of squared
//    double[] weights;  //
//    double[] residuals;
//    public LinearRegression() {
//
//    }
//
//    public double[] getResiduals() {
//        return residuals;
//    }
//
//    /**
//     * Performs a linear regression on the data points {@code (y[i], x[i][j])}.
//     *
//     * @param x the values of the predictor variables
//     * @param y the corresponding values of the response variable
//     * @throws IllegalArgumentException if the lengths of the two arrays are not equal
//     */
//    public LinearRegression(double[][] x, double[] y) {
//        if (x.length != y.length) {
//            throw new IllegalArgumentException("matrix dimensions don't agree");
//        }
//
//        // number of observations
//        int n = y.length;
//        DoubleMatrix2D matrixX = new DenseDoubleMatrix2D(x);
//
//        double[][] yM = new double[y.length][1];
//        for (int i = 0; i < yM.length; i++) {
//            yM[i][0] = y[i];
//        }
//        // create matrix from vector
//        DoubleMatrix2D matrixY = new DenseDoubleMatrix2D(yM);
//        // find least squares solution
//        QRDecomposition qr = new QRDecomposition(matrixX);
//        beta = qr.solve(matrixY);
//
//
//        // mean of y[] values
//        double sum = 0.0;
//        for (int i = 0; i < n; i++)
//            sum += y[i];
//        double mean = sum / n;
//
//        // total variation to be accounted for
//        for (int i = 0; i < n; i++) {
//            double dev = y[i] - mean;
//            sst += dev * dev;
//        }
//
//        // variation not accounted for
//        DoubleMatrix2D tmp = new DenseDoubleMatrix2D(1, n);
//        DoubleMatrix2D residuals = matrixX.zMult(beta, tmp);
//        for (int i = 0; i < n; i++) {
//            residuals.setQuick(0, i, y[i] - tmp.getQuick(0, i));
//        }
//
//        // sse = residuals.norm2() * residuals.norm2();
//
//    }
//
//    public boolean robustLinearRegression(double[] Y, double[][] X, int iterN, int methodID) {
//        weights = new double[Y.length];
//        Arrays.fill(weights, 1);
//        return robustLinearRegression(Y, X, weights, iterN, methodID);
//
//    }
//
//    public double[] getWeights() {
//        return weights;
//    }
//
//    //https://onlinecourses.science.psu.edu/stat501/node/353
//    public boolean robustLinearRegression(double[] Y, double[][] X, double[] W, int iterN, int methodID) {
//
//        int iterI = 0;
//        int size = W.length;
//        double MINDIFF = 1e-6;
//        double maxDiff = 0, tempDiff;
//        double sd = Summary.stddev(Y);
//        double c = 1.345 * sd;
//        //Bisquare
//        if (methodID == 2) {
//            c = 4.685 * sd;
//        }
//        boolean success = false;
//        double[] coef = new double[X[0].length];     // Coefficients
//        double[] coefSD = new double[X[0].length];    // Std Error of coefficients
//         residuals = new double[Y.length];            // Residual values of Y
//
//
//        weightedLeastSquaresArray(Y, X, W, coef, coefSD, residuals);
//        double[] tmpCoef = new double[X[0].length];
//        System.arraycopy(coef, 0, tmpCoef, 0, tmpCoef.length);
//        double[] tmpResi = new double[size];
//        while (iterI < iterN) {
//            for (int i = 0; i < size; i++) {
//                tmpResi[i] = Math.abs(residuals[i]);
//            }
//            //update the SD by a median absolute residual which is emperical
//            //sd = StdStats.medianNS(tmpResi) / 0.6745;
//            sd = Summary.stddevNadw(Y, weights);
//            //sd = StdStats.stddevNadw(residual, weight);
//            switch (methodID) {
//                case 2:
//                    c = 4.685 * sd;
//                    break;
//                case 1:
//                    c = 1.345 * sd;
//                    break;
//            }
//
//            for (int i = 0; i < size; i++) {
//                switch (methodID) {
//                    case 2:
//                        if (Math.abs(residuals[i]) <= c) {
//                            W[i] = (residuals[i] / c);
//                            W[i] = W[i] * W[i];
//                            W[i] = 1 - W[i];
//                            W[i] = W[i] * W[i];
//                        } else {
//                            W[i] = 0;
//                        }
//                        break;
//                    case 1:
//                        if (Math.abs(residuals[i]) <= c) {
//                            W[i] = 1;
//                        } else {
//                            W[i] = c / Math.abs(residuals[i]);
//                        }
//                        break;
//                    default:
//                        W[i] = 1;
//                        break;
//                }
//            }
//            weightedLeastSquaresArray(Y, X, W, coef, coefSD, residuals);
//            maxDiff = Math.abs(coef[0] - tmpCoef[0]);
//            for (int i = 1; i < tmpCoef.length; i++) {
//                tempDiff = Math.abs(coef[i] - tmpCoef[i]);
//                if (tempDiff > maxDiff) {
//                    maxDiff = tempDiff;
//                }
//                //System.out.println(coef[i]);
//            }
//            //System.out.println();
//            if (maxDiff <= MINDIFF) {
//                success = true;
//                break;
//            }
//            System.arraycopy(coef, 0, tmpCoef, 0, tmpCoef.length);
//            System.arraycopy(W, 0, weights, 0, W.length);
//            iterI++;
//        }
//        return success;
//    }
//
//    public boolean weightedLeastSquaresArray(double[] Y, double[][] X, double[] W, double[] coef, double[] coefSD, double[] residual) {
//        // Y[j]   = j-th observed data point
//        // X[i,j] = j-th value of the i-th independent varialble
//        // W[j]   = j-th weight value
//        double RYSQ;            // Multiple correlation coefficient
//        double SDV;             // Standard deviation of errors
//        double FReg;            // Fisher F statistic for regression
//        double[] fitY;         // Calculated values of Y
//
//        double[][] covar;            // Least squares and var/covar matrix
//
//
//        int M = Y.length;             // M = Number of data points
//        int N = X[0].length;         // N = Number of linear terms
//        int NDF = M - N;              // Degrees of freedom
//        fitY = new double[M];
//
//        // If not enough data, don't attempt regression
//        if (NDF < 1) {
//            return false;
//        }
//        covar = new double[N][N];
//
//        double[] B = new double[N];   // Vector for LSQ
//
//        // Clear the matrices to start out
//        for (int i = 0; i < N; i++) {
//            for (int j = 0; j < N; j++) {
//                covar[i][j] = 0;
//            }
//        }
//
//        // Form Least Squares Matrix
//        for (int i = 0; i < N; i++) {
//            for (int j = 0; j < N; j++) {
//                covar[i][j] = 0;
//                for (int k = 0; k < M; k++) {
//                    covar[i][j] = covar[i][j] + W[k] * X[k][i] * X[k][j];
//                }
//            }
//            B[i] = 0;
//            for (int k = 0; k < M; k++) {
//                B[i] = B[i] + W[k] * X[k][i] * Y[k];
//            }
//        }
//        // V now contains the raw least squares matrix
//        if (!symmetricMatrixInvert(covar)) {
//            return false;
//        }
//        // V now contains the inverted least square matrix
//        // Matrix multpily to get coefficients C = VB
//        for (int i = 0; i < N; i++) {
//            coef[i] = 0;
//            for (int j = 0; j < N; j++) {
//                coef[i] = coef[i] + covar[i][j] * B[j];
//            }
//        }
//
//        // Calculate statistics
//        double TSS = 0;
//        double RSS = 0;
//        double YBAR = 0;
//        double WSUM = 0;
//        for (int k = 0; k < M; k++) {
//            YBAR = YBAR + W[k] * Y[k];
//            WSUM = WSUM + W[k];
//        }
//        YBAR = YBAR / WSUM;
//        for (int k = 0; k < M; k++) {
//            fitY[k] = 0;
//            for (int i = 0; i < N; i++) {
//                fitY[k] = fitY[k] + coef[i] * X[k][i];
//            }
//            residual[k] = fitY[k] - Y[k];
//            TSS = TSS + W[k] * (Y[k] - YBAR) * (Y[k] - YBAR);
//            RSS = RSS + W[k] * residual[k] * residual[k];
//        }
//        double SSQ = RSS / NDF;
//        RYSQ = 1 - RSS / TSS;
//        FReg = 9999999;
//        if (RYSQ < 0.9999999) {
//            FReg = RYSQ / (1 - RYSQ) * NDF / (N - 1);
//        }
//        SDV = Math.sqrt(SSQ);
//
//        // Calculate var-covar matrix and std error of coefficients
//        for (int i = 0; i < N; i++) {
//            for (int j = 0; j < N; j++) {
//                covar[i][j] = covar[i][j] * SSQ;
//            }
//            coefSD[i] = Math.sqrt(covar[i][i]);
//        }
//
//        return true;
//    }
//
//    public boolean symmetricMatrixInvert(double[][] V) {
//        int N = V.length;
//        double[] t = new double[N];
//        double[] Q = new double[N];
//        double[] R = new double[N];
//        double AB;
//        int K, L, M;
//
//        // Invert a symetric matrix in V
//        for (M = 0; M < N; M++) {
//            R[M] = 1;
//        }
//        K = 0;
//        for (M = 0; M < N; M++) {
//            double Big = 0;
//            for (L = 0; L < N; L++) {
//                AB = Math.abs(V[L][L]);
//                if ((AB > Big) && (R[L] != 0)) {
//                    Big = AB;
//                    K = L;
//                }
//            }
//            if (Big == 0) {
//                return false;
//            }
//            R[K] = 0;
//            Q[K] = 1 / V[K][K];
//            t[K] = 1;
//            V[K][K] = 0;
//            if (K != 0) {
//                for (L = 0; L < K; L++) {
//                    t[L] = V[L][K];
//                    if (R[L] == 0) {
//                        Q[L] = V[L][K] * Q[K];
//                    } else {
//                        Q[L] = -V[L][K] * Q[K];
//                    }
//                    V[L][K] = 0;
//                }
//            }
//            if ((K + 1) < N) {
//                for (L = K + 1; L < N; L++) {
//                    if (R[L] != 0) {
//                        t[L] = V[K][L];
//                    } else {
//                        t[L] = -V[K][L];
//                    }
//                    Q[L] = -V[K][L] * Q[K];
//                    V[K][L] = 0;
//                }
//            }
//            for (L = 0; L < N; L++) {
//                for (K = L; K < N; K++) {
//                    V[L][K] = V[L][K] + t[L] * Q[K];
//                }
//            }
//        }
//        M = N;
//        L = N - 1;
//        for (K = 1; K < N; K++) {
//            M = M - 1;
//            L = L - 1;
//            for (int J = 0; J <= L; J++) {
//                V[M][J] = V[J][M];
//            }
//        }
//        return true;
//    }
//
//
//    /**
//     * Returns the least squares estimate of &beta;<sub><em>j</em></sub>.
//     *
//     * @param j the index
//     * @return the estimate of &beta;<sub><em>j</em></sub>
//     */
//    public double beta(int j) {
//        return beta.get(j, 0);
//    }
//
//    /**
//     * Returns the coefficient of determination <em>R</em><sup>2</sup>.
//     *
//     * @return the coefficient of determination <em>R</em><sup>2</sup>,
//     * which is a real number between 0 and 1
//     */
//    public double R2() {
//        return 1.0 - sse / sst;
//    }
//
//    /**
//     * Unit tests the {@code MultipleLinearRegression} data type.
//     *
//     * @param args the command-line arguments
//     */
//    public static void main(String[] args) {
//        double[][] x = {{1, 10, 20},
//                {1, 20, 40},
//                {1, 40, 15},
//                {1, 80, 100},
//                {1, 160, 23},
//                {1, 200, 18}};
//        double[] y = {243, 483, 508, 1503, 1764, 2129};
//        LinearRegression regression = new LinearRegression(x, y);
//
//        System.out.printf("%.2f + %.2f beta1 + %.2f beta2  (R^2 = %.2f)\n",
//                regression.beta(0), regression.beta(1), regression.beta(2), regression.R2());
//    }
//}
