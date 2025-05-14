package edu.sysu.pmglab.easytools.threshold;//package edu.sysu.pmglab.easytools.threshold;
//
//import org.nd4j.linalg.api.ndarray.INDArray;
//import org.nd4j.linalg.factory.Nd4j;
//
///**
// * @author Wenjie Peng
// * @create 2025-03-15 17:16
// * @description
// */
//public class Test {
//    public static void main(String[] args) {
//        // 1. 数据加载与预处理
//        INDArray x = loadData("x.csv");
//        INDArray y = loadData("y.csv");
//
//        // 2. 生成候选阈值（并行化生成）
//        double a = x.percentile(20).getDouble(0);
//        double b = x.percentile(80).getDouble(0);
//        INDArray thetas = Nd4j.linspace(a, b, 100);
//
//        // 3. 并行计算每个theta的LR统计量
//        double[] maxLR = {-Double.MAX_VALUE};
//        double[] bestTheta = {Double.NaN};
//
//        Nd4j.getExecutioner().exec(new ParallelLoops(thetas, theta -> {
//            INDArray X = Nd4j.hstack(Nd4j.ones(x.length(), 1), x.gt(theta));
//            INDArray beta = Logi.fit(X, y);
//            double lr = 2 * (computeLogLik(X, beta, y) - logLikNull);
//            if (lr > maxLR[0]) {
//                maxLR[0] = lr;
//                bestTheta[0] = theta;
//            }
//        }, Runtime.getRuntime().availableProcessors()));
//
//        System.out.println("Optimal theta: " + bestTheta[0]);
//    }
//}
