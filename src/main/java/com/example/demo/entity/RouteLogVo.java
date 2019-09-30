package com.example.demo.entity;

import java.util.HashMap;
import java.util.Map;

public class RouteLogVo {
    private String ip;
    private String url;
    private String serviceId;
    private Map<String,String> requestParam;
    private Map<String,String> responseParam;
    private String responseParamStr;
    private Integer stateCode;
    private Long startTime;
    private Long endTime;


    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public Map<String, String> getRequestParam() {
        return requestParam;
    }

    public void setRequestParam(Map<String, String> requestParam) {
        this.requestParam = requestParam;
    }

    public Map<String, String> getResponseParam() {
        return responseParam;
    }

    public void setResponseParam(Map<String, String> responseParam) {
        this.responseParam = responseParam;
    }

    public Integer getStateCode() {
        return stateCode;
    }

    public void setStateCode(Integer stateCode) {
        this.stateCode = stateCode;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public String getResponseParamStr() {
        return responseParamStr;
    }

    public void setResponseParamStr(String responseParamStr) {
        this.responseParamStr = responseParamStr;
    }
}
