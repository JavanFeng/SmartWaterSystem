package com.javan.smart.water.common.base;

import java.io.Serial;
import java.io.Serializable;

/**
 *
 */
public class ServerException extends RuntimeException implements Serializable {


    @Serial
    private static final long serialVersionUID = -1;
    private String error;
    private String message;

    public ServerException() {
    }

    public ServerException(IBaseCode code) {
        this.error = code.getCode();
        this.message = code.getMessage();
    }

    public ServerException(String error) {
        this.error = error;
    }

    public ServerException(String error, String message) {
        this.error = error;
        this.message = message;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}