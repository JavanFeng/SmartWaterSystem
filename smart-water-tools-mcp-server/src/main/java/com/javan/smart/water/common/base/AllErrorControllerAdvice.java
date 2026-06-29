package com.javan.smart.water.common.base;

import cn.hutool.core.util.StrUtil;
import com.javan.smart.water.common.MarkConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.beans.PropertyEditorSupport;
import java.util.Date;
import java.util.List;

/**
 * 统一错误处理
 *
 */
@RestControllerAdvice
public class AllErrorControllerAdvice {

    private static final Logger LOG = LoggerFactory.getLogger(AllErrorControllerAdvice.class);

    @ExceptionHandler(ServerException.class)
    public ApiResponse<String> serverExceptionHandler(ServerException e) {
        LOG.error("",e);
        return DT.error("SERVER EXCEPTION", e.getMessage());
    }


    @ExceptionHandler(BindException.class)
    public ApiResponse<String> bind2ExceptionHandler(BindException e) {
        List<ObjectError> allErrors = e.getBindingResult().getAllErrors();
        String err = getBindErrorInfo(allErrors);
        LOG.error("", e);
        return DT.error("BIND EXCEPTION", err);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<String> bindMethodArgumentNotValidExceptionHandler(MethodArgumentNotValidException e) {
        List<ObjectError> allErrors = e.getBindingResult().getAllErrors();
        String err = getBindErrorInfo(allErrors);
        LOG.warn("", e);
        return DT.error("BIND EXCEPTION", err);
    }

    /**
     * 错误信息
     */
    private String getBindErrorInfo(List<ObjectError> allErrors) {
        StringBuilder builder = new StringBuilder();
        for (ObjectError error : allErrors) {
            error.getCodes();
            if (error.getCodes().length > 0) {
                builder.append(error.getCodes()[0]);
            } else {
                builder.append(error.getObjectName());
                builder.append(MarkConstant.POINT);
            }
            builder.append(MarkConstant.COLON);
            builder.append(MarkConstant.SPACE);
            builder.append(error.getDefaultMessage());
            builder.append(MarkConstant.COMMA);
            builder.append(MarkConstant.SPACE);
        }
        return builder.toString();
    }


    @ExceptionHandler(Exception.class)
    public ApiResponse<String> exceptionHandler(Exception e) throws Exception {
        if (e instanceof HttpRequestMethodNotSupportedException) {
            throw e;
        }
        LOG.error("未知错误", e);
        return DT.error(ErrorCode.API_ERROR);
    }


    @InitBinder
    protected void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(Date.class, new TimeStampConvert());
    }

    /**
     * 前端时间戳 -> 转化时间
     */
    private static class TimeStampConvert extends PropertyEditorSupport {
        @Override
        public void setAsText(String timeStamp) throws IllegalArgumentException {
            // empty
            if (StrUtil.isBlank(timeStamp)) {
                return;
            }
            try {
                long lt = Long.parseLong(timeStamp);
                Date date = new Date(lt);
                setValue(date);
            } catch (Exception e) {
                LOG.error("时间转化错误", e);
                throw e;
            }
        }
    }
}
