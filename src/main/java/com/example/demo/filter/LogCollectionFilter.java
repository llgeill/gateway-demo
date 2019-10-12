package com.example.demo.filter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.demo.constant.GateWayLogConstant;
import com.example.demo.entity.RouteLogVo;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.BodyInserterContext;
import org.springframework.cloud.gateway.support.CachedBodyOutputMessage;
import org.springframework.cloud.gateway.support.DefaultServerRequest;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

/**
 * 收集经过网关的请求信息及响应信息
 *
 * @author liliguang
 */
@Component
public class LogCollectionFilter implements GlobalFilter, Ordered {

    private static final Log log = LogFactory.getLog(LogCollectionFilter.class);

    private final static ThreadLocal<RouteLogVo> routeLogVoThreadLocal = new ThreadLocal<>();

    /**
     * 获取本次请求的RouteLogVo，处理操作放到此内部过滤器
     */
    @Component
    class GetRouteLogVoFilter implements GlobalFilter, Ordered {

        @Override
        public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
            return chain.filter(exchange).then(
                    Mono.fromRunnable(() -> {
                        RouteLogVo routeLogVo = LogCollectionFilter.routeLogVoThreadLocal.get();
                        log.info("总响应时间: " + (routeLogVo.getEndTime() - routeLogVo.getStartTime()));
                        log.info(JSON.toJSONString(routeLogVo));
                        //TODO 日志对象操作
                        LogCollectionFilter.routeLogVoThreadLocal.remove();
                    })
            );
        }

        @Override
        public int getOrder() {
            return GateWayLogConstant.INNER_PORIORITY;
        }
    }


    /**
     * 过滤器核心，负责拦截请求信息和响应信息
     *
     * @param exchange
     * @param chain
     * @return
     */
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //获取服务名称
        Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
        if (route == null) {
            return chain.filter(exchange);
        }
        //创建日志对象、请求对象、请求装饰器
        RouteLogVo routeLogVo = new RouteLogVo();
        routeLogVoThreadLocal.set(routeLogVo);
        ServerRequest serverRequest = new DefaultServerRequest(exchange);
        ServerHttpRequest request = exchange.getRequest();
        //填充请求信息
        routeLogVo.setStartTime(System.currentTimeMillis());
        routeLogVo.setServiceId(route.getUri().getHost());
        routeLogVo.setUrl(request.getURI().toString());
        routeLogVo.setIp(getIpAddress(request));
        routeLogVo.setQueryParams(request.getQueryParams());
        routeLogVo.setRequestHeaders(request.getHeaders());
        //根据响应信息进行回调
        ServerHttpResponse response = exchange.getResponse();
        DataBufferFactory bufferFactory = response.bufferFactory();
        ServerHttpResponseDecorator decoratedResponse = fillResponseContent(routeLogVo, response, bufferFactory);
        //请求为空或请求长度超过阈值则跳过读取
        long contentLength = request.getHeaders().getContentLength();
        if (contentLength > 0 && contentLength < GateWayLogConstant.REQUEST_CONTENT_LENGTH_MAX) {
            Mono<String> modifiedBody = serverRequest.bodyToMono(String.class)
                    .flatMap(body -> {
                        String bb = body;
                        routeLogVo.setRequestBody(parseRequestParam(new AtomicReference<>(body), request.getHeaders().getContentType()));
                        return Mono.just(body);
                    });
            BodyInserter bodyInserter = BodyInserters.fromPublisher(modifiedBody, String.class);
            HttpHeaders headers = new HttpHeaders();
            headers.putAll(exchange.getRequest().getHeaders());
            headers.remove(HttpHeaders.CONTENT_LENGTH);
            CachedBodyOutputMessage outputMessage = new CachedBodyOutputMessage(exchange, headers);
            return bodyInserter.insert(outputMessage, new BodyInserterContext())
                    .then(Mono.defer(() -> {
                        ServerHttpRequestDecorator decorator = new ServerHttpRequestDecorator(
                                exchange.getRequest()) {
                            public HttpHeaders getHeaders() {
                                long contentLength = headers.getContentLength();
                                HttpHeaders httpHeaders = new HttpHeaders();
                                httpHeaders.putAll(super.getHeaders());
                                if (contentLength > 0) {
                                    httpHeaders.setContentLength(contentLength);
                                } else {
                                    httpHeaders.set(HttpHeaders.TRANSFER_ENCODING, "chunked");
                                }
                                return httpHeaders;
                            }

                            public Flux<DataBuffer> getBody() {
                                return outputMessage.getBody();
                            }
                        };
                        return chain.filter(exchange.mutate().request(decorator).response(decoratedResponse).build());
                    }));
        } else {
            log.info("请求数据过大或不存在，跳过读取");
            return chain.filter(exchange.mutate().response(decoratedResponse).build());
        }
    }


    /**
     * 将请求内容根据请求类型进行相应的转换
     *
     * @param requestBody 请求内容
     * @param mediaType   请求类型
     * @return 请求内容键值对
     */
    private static Map<String, Object> parseRequestParam(AtomicReference<String> requestBody, MediaType mediaType) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (MediaType.APPLICATION_JSON.isCompatibleWith(mediaType)) {// 如果是json格式
            return JSONObject.parseObject(requestBody.get()).getInnerMap();
        } else if (MediaType.MULTIPART_FORM_DATA.isCompatibleWith(mediaType)) {// 如果是form-data请求
            String requestValue = requestBody.get();
            String[] requestArray = requestValue.split(mediaType.getParameter("boundary"));
            for (String string : requestArray) {
                String name = null;
                String value = null;
                String filename = null;
                String tempAray[] = null;
                if (string != null && string.length() > 4) {
                    tempAray = string.split("\r\n");
                    if (tempAray != null && tempAray.length >= 4) {
                        //收集name
                        Pattern pattern = Pattern.compile("name=\"[^\\f\\n\\r\\t\\v]+?(?=\")");
                        Matcher matcher = pattern.matcher(tempAray[1]);
                        if (matcher.find()) {
                            name = matcher.group(0).replace("\"", "").replace("name=", "");
                        }
                        //收集filename
                        Pattern pattern2 = Pattern.compile("filename=\"[^\\f\\n\\r\\t\\v]+?(?=\")");
                        Matcher matcher2 = pattern2.matcher(tempAray[1]);
                        if (matcher2.find()) {
                            filename = matcher2.group(0).replace("\"", "").replace("filename=", "");
                        }
                        //收集value
                        if (filename != null) value = "filename: " + filename;
                        else value = tempAray[tempAray.length - 2];
                    }
                    map.put(name, value);
                }

            }
        } else if (MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(mediaType)) {//如果是form-urlencodea请求
            String source = requestBody.get();
            String[] tempArray = source.split("&");
            for (String string : tempArray) {
                String[] nameAndValue = string.split("=");
                if (nameAndValue != null && nameAndValue.length > 1) map.put(nameAndValue[0], nameAndValue[1]);
                else map.put(nameAndValue[0], null);
            }
        } else if (MediaType.TEXT_PLAIN.isCompatibleWith(mediaType)) {//如果是test/plain
            map.put("TEXT/PLAIN", requestBody.get());
        } else {
            //TODO 做其他请求类型判断
        }
        return map;
    }

    /**
     * 填充有关响应的日志信息
     *
     * @param routeLogVo    日志对象
     * @param response      响应对象
     * @param bufferFactory 响应缓存对象
     * @return
     */
    private static ServerHttpResponseDecorator fillResponseContent(RouteLogVo routeLogVo, ServerHttpResponse response, DataBufferFactory bufferFactory) {
        return new ServerHttpResponseDecorator(response) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                if (body instanceof Flux) {
                    Flux<? extends DataBuffer> fluxBody = (Flux<? extends DataBuffer>) body;
                    return super.writeWith(fluxBody.map(dataBuffer -> {
                        //填充日志对象
                        routeLogVo.setEndTime(System.currentTimeMillis());
                        routeLogVo.setStateCode(this.getStatusCode().value());
                        if (response.getHeaders().getContentType().isCompatibleWith(MediaType.APPLICATION_JSON)) {//响应类型JSON
                            //读取响应信息
                            byte[] content = fillResponseParamStr(routeLogVo, dataBuffer, response.getHeaders());
                            //响应信息累加完毕则String->Map
                            try {
                                JSONObject jsonObject = JSONObject.parseObject(routeLogVo.getResponseParamStr());
                                routeLogVo.setResponseParam(jsonObject.getInnerMap());
                                String routeLogVoJSON = JSON.toJSONString(routeLogVo);
                            } catch (Exception e) {
                                log.info("响应数据正在读取");
                            }
                            //读取响应信息后需要重新包装回去
                            return bufferFactory.wrap(content);
                        } else if (response.getHeaders().getContentType().isCompatibleWith(MediaType.APPLICATION_OCTET_STREAM)) {//响应类型application/octet-stream
                            String filename = response.getHeaders().getContentDisposition().getFilename();
                            String name = response.getHeaders().getContentDisposition().getName();
                            if (filename != null && name != null) {
                                Map map = new HashMap<String, Object>();
                                map.put(name, "filename: " + filename);
                                routeLogVo.setResponseParam(map);
                            }
                        } else if (response.getHeaders().getContentType().isCompatibleWith(MediaType.TEXT_PLAIN)) {
                            //读取响应信息
                            byte[] content = fillResponseParamStr(routeLogVo, dataBuffer, response.getHeaders());
                            //读取响应信息后需要重新包装回去
                            return bufferFactory.wrap(content);
                        } else {
                            //TODO 其他类型处理
                        }
                        return dataBuffer;
                    }));
                }
                return super.writeWith(body);
            }
        };
    }

    /**
     * 将字节流中的响应信息填充到日志对象，并将字节流返回做重新包装，防止数据丢失
     *
     * @param routeLogVo 日志对象
     * @param dataBuffer 数据流
     * @return
     */
    private static byte[] fillResponseParamStr(RouteLogVo routeLogVo, DataBuffer dataBuffer, HttpHeaders headers) {
        //读取原数据
        byte[] content = new byte[dataBuffer.readableByteCount()];
        dataBuffer.read(content);
        DataBufferUtils.release(dataBuffer);
        //备份原数据引用
        byte[] wrapContent =  content;
        //判断响应信息是否为gzip压缩格式
        String hasGZIP = headers.getFirst("Content-Encoding");
        if (hasGZIP != null && hasGZIP.equals("gzip")) {
            content = unGzip(content);
        }
        String responseResult = new String(content, Charset.forName("UTF-8"));
        //响应信息累加
        if (routeLogVo.getResponseParamStr() != null) {
            routeLogVo.setResponseParamStr(routeLogVo.getResponseParamStr() + responseResult);
        } else {
            routeLogVo.setResponseParamStr(responseResult);
        }
        //返回原数据引用
        return wrapContent;
    }

    /**
     * 获取真实Ip地址
     *
     * @param request
     * @return
     */
    private static String getIpAddress(ServerHttpRequest request) {
        HttpHeaders heads = request.getHeaders();
        String Xip = heads.getFirst("X-Real-IP");
        String XFor = heads.getFirst("X-Forwarded-For");
        if (StringUtils.isNotEmpty(XFor) && !"unKnown".equalsIgnoreCase(XFor)) {
            //多次反向代理后会有多个ip值，第一个ip才是真实ip
            int index = XFor.indexOf(",");
            if (index != -1) {
                return XFor.substring(0, index);
            } else {
                return XFor;
            }
        }
        XFor = Xip;
        if (StringUtils.isNotEmpty(XFor) && !"unKnown".equalsIgnoreCase(XFor)) {
            return XFor;
        }
        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
            XFor = heads.getFirst("Proxy-Client-IP");
        }
        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
            XFor = heads.getFirst("WL-Proxy-Client-IP");
        }
        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
            XFor = heads.getFirst("HTTP_CLIENT_IP");
        }
        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
            XFor = heads.getFirst("HTTP_X_FORWARDED_FOR");
        }
        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
            XFor = request.getRemoteAddress().getHostName();
        }
        return XFor;
    }

    public static byte[] unGzip(byte[] content) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPInputStream gis = null;
        try {
            gis = new GZIPInputStream(new ByteArrayInputStream(content));
            byte[] buffer = new byte[1024];
            int n;
            while ((n = gis.read(buffer)) != -1) {
                baos.write(buffer, 0, n);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
    }



    public int getOrder() {
        return GateWayLogConstant.EXTERNAL_PORIORITY;
    }


}