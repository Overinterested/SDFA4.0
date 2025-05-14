package edu.sysu.pmglab.easytools.r;

import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Wenjie Peng
 * @create 2025-03-13 03:15
 * @description
 */
public class RConnectionPool {
    static volatile BlockingQueue<RConnection> pool = new LinkedBlockingQueue<>(10);

    private RConnectionPool(){

    }

    public static boolean addConnection(String host, int port) {
        try {
            RConnection conn = new RConnection(host, port);
            pool.add(conn);
            return true;
        } catch (RserveException e) {
            return false;
        }
    }

    public static boolean addConnectionWithLibraries(String host, int port, String... libraries){
        try {
            RConnection conn = new RConnection(host, port);
            for (String library : libraries) {
                conn.eval("library("+library+")");
            }
            pool.add(conn);
            return true;
        } catch (RserveException e) {
            return false;
        }
    }

    public static RConnection getConnection() throws InterruptedException {
        return pool.take();
    }

    public static void release(RConnection connection) throws InterruptedException {
        pool.put(connection);
    }


}
