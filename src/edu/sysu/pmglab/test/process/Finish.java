package edu.sysu.pmglab.test.process;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Wenjie Peng
 * @create 2025-05-10 07:50
 * @description mark a test class as fully implemented and error-free upon successful execution
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Finish {
    /**
     * 可选属性：记录完成日期
     */
    String date() default "";

    /**
     * 可选属性：记录完成者
     */
    String author() default "";

    /**
     * 可选属性：添加额外备注信息
     */
    String comment() default "";
}
