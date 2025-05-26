package edu.sysu.pmglab.debug;

import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

/**
 * @author Wenjie Peng
 * @create 2025-05-24 07:13
 * @description
 */
public class RTest {
    public static void main(String[] args) throws RserveException, REXPMismatchException {
        RConnection conn = new RConnection("localhost", 1111);
        double res = conn.eval("2+1").asDouble();
        System.out.println(res);
    }
}
