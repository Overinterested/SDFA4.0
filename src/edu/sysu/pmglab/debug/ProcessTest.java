package edu.sysu.pmglab.debug;

import ch.qos.logback.classic.Logger;
import edu.sysu.pmglab.LogBackOptions;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.container.PersistentTerminalManager;
import edu.sysu.pmglab.executor.*;

/**
 * @author Wenjie Peng
 * @create 2025-06-12 08:37
 * @description
 */
public class ProcessTest {
    public static void main(String[] args) throws InterruptedException {
        int thread = 20;
        Workflow workflow = new Workflow(thread);
        for (int i = 0; i < thread; i++) {
            ITask task = (status, context) -> {
                long startTime = System.currentTimeMillis();
                long waitTime = 3000; // 3秒
                System.out.println(Thread.currentThread());
                // 空转循环
                while (System.currentTimeMillis() - startTime < waitTime) {
                    // 空转，CPU持续运行
                }
            };
            workflow.addTask(task);
        }
        workflow.execute();
        workflow.clearTasks();
        LogBackOptions.init();
        Logger logger = LogBackOptions.getRootLogger();
        logger.info("start pipeline");
        Pipeline pipeline;
        List<ITask> tasks = new List<>();
        for (int i = 0; i < thread; i++) {
            ITask task = (status, context) -> {
                long startTime = System.currentTimeMillis();
                long waitTime = 3000; // 3秒
                LogBackOptions.getRootLogger().info(Thread.currentThread().toString());
                // 空转循环
                while (System.currentTimeMillis() - startTime < waitTime) {
                    // 空转，CPU持续运行
                }
            };
            tasks.add(task);
        }
        pipeline = new Pipeline(tasks);
        workflow.addTask(pipeline);
        workflow.execute();
        workflow.clearTasks();
    }
}
