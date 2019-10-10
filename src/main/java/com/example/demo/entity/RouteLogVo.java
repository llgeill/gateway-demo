package com.example.demo.entity;

import com.alibaba.fastjson.annotation.JSONField;
import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.Map;

public class RouteLogVo {
    //请求地址
    private String ip;
    //请求url
    private String url;
    //服务名称
    private String serviceId;
    private MultiValueMap<String, String> queryParams;
    private HttpHeaders requestHeaders;
    private Map<String,Object> requestBody;
    private Map<String,Object> responseParam;
    @JSONField(serialize = false)
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

    public MultiValueMap<String, String> getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(MultiValueMap<String, String> queryParams) {
        this.queryParams = queryParams;
    }

    public HttpHeaders getRequestHeaders() {
        return requestHeaders;
    }

    public void setRequestHeaders(HttpHeaders requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    public Map<String, Object> getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(Map<String, Object> requestBody) {
        this.requestBody = requestBody;
    }

    public Map<String, Object> getResponseParam() {
        return responseParam;
    }

    public void setResponseParam(Map<String, Object> responseParam) {
        this.responseParam = responseParam;
    }

    public String getResponseParamStr() {
        return responseParamStr;
    }

    public void setResponseParamStr(String responseParamStr) {
        this.responseParamStr = responseParamStr;
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
}
