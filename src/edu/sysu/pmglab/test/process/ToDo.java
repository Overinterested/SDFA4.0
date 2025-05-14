package edu.sysu.pmglab.test.process;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Wenjie Peng
 * @create 2025-05-10 07:56
 * @description Marks a test class with pending tasks.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ToDo {
    /** Description of the task to be completed */
    String value();

    /** Optional: Assignee responsible for completion */
    String assignee() default "";

    /** Optional: Deadline in YYYY-MM-DD format */
    String deadline() default "";
}
