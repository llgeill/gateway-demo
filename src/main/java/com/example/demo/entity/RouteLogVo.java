package com.example.demo.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;


import java.util.Map;


@Document(indexName = "log", type = "routelog",shards = 1, replicas = 1)
public class RouteLogVo {
    @Id
    private String id;
    //请求地址
    @Field(type = FieldType.Keyword)
    private String ip;
    //请求url
    @Field(type = FieldType.Keyword)
    private String url;

    //服务名称
//    @Field(type = FieldType.Text,analyzer = "ik_max_word")
    @Field(type = FieldType.Keyword)
    private String serviceId;

    //url参数
    private Map<String, String> queryParams;

    //请求头
    private Map<String,String> requestHeaders;

    //请求体
    private Map<String,Object> requestBody;

    //响应体
    private Map<String,Object> responseParam;

    //状态码
    @Field(type = FieldType.Integer)
    private Integer stateCode;

    //开始时间
    @Field(type = FieldType.Long)
    private Long startTime;

    //结束时间
    @Field(type = FieldType.Long)
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

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(Map<String, String> queryParams) {
        this.queryParams = queryParams;
    }

    public Map getRequestHeaders() {
        return requestHeaders;
    }

    public void setRequestHeaders(Map requestHeaders) {
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


    @Override
    public String toString() {
        return "RouteLogVo{" +
                "ip='" + ip + '\'' +
                ", url='" + url + '\'' +
                ", serviceId='" + serviceId + '\'' +
                ", queryParams=" + queryParams +
                ", requestHeaders=" + requestHeaders +
                ", requestBody=" + requestBody +
                ", responseParam=" + responseParam +
                ", stateCode=" + stateCode +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }
}
