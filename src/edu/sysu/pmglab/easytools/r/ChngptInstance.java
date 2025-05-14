package edu.sysu.pmglab.easytools.r;

import edu.sysu.pmglab.LogBackOptions;
import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.container.list.DoubleList;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.io.reader.ReaderStream;
import org.rosuda.REngine.REXPDouble;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import java.io.IOException;
import java.util.Iterator;

/**
 * @author wenjie peng
 * @create 2025-03-12-10:23 下午
 */
public class ChngptInstance {
    private static final String DATA_FRAME_CREATE = "data <- data.frame(x = x, y = y)";

    private ChngptInstance() {

    }

    public static AdaptiveThresholdLogistic stepTestWithGLM(double[] x, double[] y,
                                                            float lowQuantileBound, float upperQuantileBound,
                                                            String testMethod, int bootB, int nOfMC) throws REngineException, InterruptedException {
        RConnection conn = RConnectionPool.getConnection();
        AdaptiveThresholdLogistic result = new AdaptiveThresholdLogistic();
        conn.assign("x", x);
        conn.assign("y", y);
        conn.eval(DATA_FRAME_CREATE);
        try {
            if (x.length != y.length) {
                throw new UnsupportedOperationException("The length of arrays doesn't match.");
            }
            try {
                String rCmd = String.format(
                        "test_result <- chngpt.test(" +
                                "formula.null = y ~ 1, " +
                                "formula.chngpt = ~ x, " +
                                "data = data, " +
                                "type = 'step', " +
                                "family = 'binomial', " +
                                "lb = %f, ub = %f, " +
                                "mc.n = %d, boot.B = %d, " +
                                "test.statistic = '%s')",
                        lowQuantileBound, upperQuantileBound, nOfMC, bootB, testMethod
                );
                conn.eval(rCmd);
            } catch (REngineException e) {
                result = new AdaptiveThresholdLogistic();
                conn.eval("fit_3 <- glm(y ~ x, family = binomial(link = 'logit'), data = data)");
                try {
                    result.pValue = conn.eval("summary(fit_3)$coefficients[2, 4]").asDouble();
                } catch (REXPMismatchException e3) {
                    result.pValue = 1;
                }
                result.method = "logistic regression";
                return result;
            }
            // 提取结果
            double pValue, threshold;
            try {
                pValue = conn.eval("test_result$p.value").asDouble();
                threshold = conn.eval("test_result$chngpt").asDouble();
                if (pValue < 0.01) {
                    try {
                        double[] xCopy = new double[x.length];
                        for (int i = 0; i < x.length; i++) {
                            xCopy[i] = x[i] > threshold ? 1 : 0;
                        }
                        conn.assign("x_1", new REXPDouble(xCopy));
                        conn.eval("fit_1 <- glm(y ~ x_1, family = binomial(link = 'logit'))");
                        result.pValue = conn.eval("summary(fit_1)$coefficients[2, 4]").asDouble();
                    } catch (REngineException e) {
                        LogBackOptions.getRootLogger().warn("Threshold p value is significant, but eval anova fails.");
                    } catch (REXPMismatchException e) {
                        result.pValue = 1;
                    }
                    result.method = "threshold logistic regression";
                    result.threshold = threshold;
                } else {
                    try {
                        conn.eval("fit_2 <- glm(y ~ x, family = binomial(link = 'logit'))");
                        result.pValue = conn.eval("summary(fit_2)$coefficients[2, 4]").asDouble();
                    } catch (REngineException e3) {
                        result.pValue = 1;
                    }
                    result.method = "logistic regression";
                }
            } catch (REXPMismatchException e) {
                result = new AdaptiveThresholdLogistic();
                conn.eval("fit_3 <- glm(y ~ x, family = binomial(link = 'logit'), data = data)");
                try {
                    result.pValue = conn.eval("summary(fit_3)$coefficients[2, 4]").asDouble();
                } catch (REXPMismatchException e3) {
                    result.pValue = 1;
                }
                result.method = "logistic regression";
                return result;
            }
        } finally {
            RConnectionPool.release(conn);
        }
        return result;
    }

    public static AdaptiveThresholdLogistic stepTest(double[] x, double[] y, String testMethod) throws InterruptedException {
        return stepTest(x, y, 0.05f, 0.95f, testMethod, 10000, 50000);
    }

    public static AdaptiveThresholdLogistic stepTest(double[] x, double[] y,
                                                     float lowQuantileBound, float upperQuantileBound,
                                                     String testMethod, int bootB, int nOfMC) throws InterruptedException {
        RConnection conn = RConnectionPool.getConnection();
        AdaptiveThresholdLogistic result = new AdaptiveThresholdLogistic();
        try {
            if (x.length != y.length) {
                throw new UnsupportedOperationException("The length of arrays doesn't match.");
            }
            conn.assign("x", x);
            conn.assign("y", y);
            conn.eval(DATA_FRAME_CREATE);
            String rCmd = String.format(
                    "test_result <- chngpt.test(" +
                            "formula.null = y ~ 1, " +
                            "formula.chngpt = ~ x, " +
                            "data = data, " +
                            "type = 'step', " +
                            "family = 'binomial', " +
                            "lb = %f, ub = %f, " +
                            "mc.n = %d, boot.B = %d, " +
                            "test.statistic = '%s')",
                    lowQuantileBound, upperQuantileBound, nOfMC, bootB, testMethod
            );
            conn.eval(rCmd);
            // 提取结果
            result.pValue = conn.eval("test_result$p.value").asDouble();
            result.threshold = conn.eval("test_result$chngpt").asDouble();
            result.method = "threshold logistic regression";
            return result;
        } catch (RserveException e) {
            LogBackOptions.getRootLogger().warn("Rserve error during test execution");
            return null;
//            throw new RuntimeException("Rserve error during test execution", e);
        } catch (Exception e) {
            LogBackOptions.getRootLogger().warn("Rserve error during test execution");
            return null;
//            throw new RuntimeException("Unexpected error during test execution", e);
        } finally {
            RConnectionPool.release(conn);
        }
    }

    public static double glm(double[] x, double[] y) throws InterruptedException {
        RConnection conn = RConnectionPool.getConnection();
        try {
            conn.assign("x", x);
            conn.assign("y", y);
            conn.eval(DATA_FRAME_CREATE);
            conn.eval("fit <- glm(y ~ x, family = binomial(link = 'logit'), data = data)");
            return conn.eval("summary(fit)$coefficients[2, 4]").asDouble();
        } catch (Exception e) {
            return 1;
        } finally {
            if (conn != null) {
                RConnectionPool.release(conn);
            }
        }
    }

    public static class AdaptiveThresholdLogistic {
        String method;
        double pValue;
        double threshold = Double.NaN;

        private AdaptiveThresholdLogistic() {

        }

        public String getMethod() {
            return method;
        }

        public double getThreshold() {
            return threshold;
        }

        public double getPValue() {
            return pValue;
        }

        protected void setMethod(String method) {
            this.method = method;
        }

        protected void setPValue(double pValue) {
            this.pValue = pValue;
        }

        protected void setThreshold(double threshold) {
            this.threshold = threshold;
        }

        @Override
        public String toString() {
            return "AdaptiveThresholdLogistic{" +
                    "method='" + method + '\'' +
                    ", pValue=" + pValue +
                    ", threshold=" + threshold +
                    '}';
        }

    }

    public static void main(String[] args) throws IOException, RserveException, InterruptedException {
        ReaderStream reader = new ReaderStream("src/edu/sysu/pmglab/easytools/r/test.txt", ReaderStream.Option.DEFAULT);
        DoubleList a = new DoubleList();
        DoubleList b = new DoubleList();
        ByteStream cache = new ByteStream();
        while (reader.readline(cache) != -1) {
            Iterator<Bytes> iterator = cache.toBytes().split(Constant.TAB);
            a.add(iterator.next().toDouble());
            b.add(iterator.next().toDouble());
            cache.clear();
        }

        RConnectionPool.addConnection("localhost", 6312);
        double glm = ChngptInstance.glm(a.toArray(), b.toArray());
        AdaptiveThresholdLogistic lr = ChngptInstance.stepTest(a.toArray(), b.toArray(), "lr");
        System.out.println(glm);
        System.out.println(lr);
    }

}
