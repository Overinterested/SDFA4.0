package edu.sysu.pmglab.sdfa;

/**
 * @author Wenjie Peng
 * @create 2024-08-30 08:28
 * @description
 */
public class SDFComponentException extends RuntimeException {
    public SDFComponentException() {
    }

    public SDFComponentException(String message) {
        super(message);
    }

    public SDFComponentException(String message, Throwable cause) {
        super(message, cause);
    }

    public SDFComponentException(Throwable cause) {
        super(cause);
    }

    protected SDFComponentException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

