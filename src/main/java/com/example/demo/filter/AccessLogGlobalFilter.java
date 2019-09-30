package com.example.demo.filter;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.demo.constant.GateWayLogConstant;
import com.example.demo.entity.RouteLogVo;
import com.fasterxml.jackson.databind.util.JSONPObject;
import jdk.nashorn.internal.parser.JSONParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AccessLogGlobalFilter implements GlobalFilter, Ordered {
    private static final Log log = LogFactory.getLog(AccessLogGlobalFilter.class);

    private static final String REQUEST_PREFIX = "Request Info [ ";

    private static final String REQUEST_TAIL = " ]";

    private static final String RESPONSE_PREFIX = "Response Info [ ";

    private static final String RESPONSE_TAIL = " ]";

    private StringBuilder normalMsg = new StringBuilder();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        RecorderServerHttpRequestDecorator requestDecorator = new RecorderServerHttpRequestDecorator(request);
        InetSocketAddress address = requestDecorator.getRemoteAddress();
        URI url = requestDecorator.getURI();
        Flux<DataBuffer> body = requestDecorator.getBody();
        //读取requestBody传参
        AtomicReference<String> requestBody = new AtomicReference<>("");
        body.subscribe(buffer -> {
            CharBuffer charBuffer = StandardCharsets.UTF_8.decode(buffer.asByteBuffer());
            requestBody.set(charBuffer.toString());
        });
        String requestParams = requestBody.get();
        //填充RouteLog对象
        RouteLogVo routeLogVo=new RouteLogVo();
        routeLogVo.setStartTime(System.currentTimeMillis());
        routeLogVo.setRequestParam(parseRequestParam(requestParams));
        ServerHttpResponse response = exchange.getResponse();
        DataBufferFactory bufferFactory = response.bufferFactory();
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(response) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                if (body instanceof Flux) {
                    Flux<? extends DataBuffer> fluxBody = (Flux<? extends DataBuffer>) body;
                    return super.writeWith(fluxBody.map(dataBuffer -> {
                        // probably should reuse buffers
                        byte[] content = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(content);
                        //释放掉内存
                        DataBufferUtils.release(dataBuffer);
                        String responseResult = new String(content, Charset.forName("UTF-8"));
                        //填充响应信息--
                        ServiceInstance instance= (ServiceInstance) exchange.getAttributes().get(GateWayLogConstant.SERVICE_ID_IP);
                        URI uri=(URI) exchange.getAttributes().get(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
                        routeLogVo.setUrl(uri.toString());
                        routeLogVo.setIp(address.getHostName());
                        routeLogVo.setServiceId(instance.getServiceId());
                        routeLogVo.setEndTime(System.currentTimeMillis());
                        routeLogVo.setStateCode(this.getStatusCode().value());
                        if(routeLogVo.getResponseParamStr()!=null)routeLogVo.setResponseParamStr(routeLogVo.getResponseParamStr()+responseResult);
                        else routeLogVo.setResponseParamStr(responseResult);
                        return bufferFactory.wrap(content);
                    }));
                }
                return super.writeWith(body); // if body is not a flux. never got there.
            }
        };

        return chain.filter(exchange.mutate().request(requestDecorator).response(decoratedResponse).build());
    }

    @Override
    public int getOrder() {
        return -2;
    }


    public static Map<String, String> parseRequestParam(String source){
        List<String> valList = new ArrayList<>();
        List<String> keyList = new ArrayList<>();
        Map<String,String> map = new LinkedHashMap<>();

        if(source!=null){
            //解析值
            String[] strings=source.split("\r\n");
            if(strings!=null&&strings.length>3){
                for(int i=3;i<strings.length;i+=4){
                    valList.add(strings[i]);
                }
            }
            //解析参数
            Pattern pattern = Pattern.compile("name=\"[^\\f\\n\\r\\t\\v]+\"");
            Matcher matcher = pattern.matcher(source);
            while (matcher.find()) {
                keyList.add(matcher.group().replace("\"","").replace("name=",""));
            }
            //放入map结构
            for(int i=0;i<keyList.size();i++){
                map.put(keyList.get(i),valList.get(i));
            }
        }
        return map;


    }



}
