package br.com.cesarsants.exceptions;

/**
 * @author cesarsants
 *
 */

public class BusinessException extends Exception {
    private static final long serialVersionUID = 1L;

    public BusinessException(String msg) {
        super(msg);
    }

    public BusinessException(String msg, Throwable cause) {
        super(msg, cause);
    }
}