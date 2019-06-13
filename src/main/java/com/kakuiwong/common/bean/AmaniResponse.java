package com.kakuiwong.common.bean;


/**
 * @author gaoyang
 * @email 785175323@qq.com
 */
public class AmaniResponse {

    private String requestId;
    private Object result;
    private Throwable t;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public Throwable getT() {
        return t;
    }

    public void setT(Throwable t) {
        this.t = t;
    }

    public boolean hasException() {
        return t != null;
    }
}
