package org.springframework.cloud.gateway.filter;


import java.net.URI;
import java.util.Map;

import com.example.demo.constant.GateWayLogConstant;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.http.server.HttpServer;

public class LoadBalancerClientFilter implements GlobalFilter, Ordered {
    private static final Log log = LogFactory.getLog(LoadBalancerClientFilter.class);
    public static final int LOAD_BALANCER_CLIENT_FILTER_ORDER = 10100;
    private final LoadBalancerClient loadBalancer;

    public LoadBalancerClientFilter(LoadBalancerClient loadBalancer) {
        this.loadBalancer = loadBalancer;
    }

    public int getOrder() {
        return LOAD_BALANCER_CLIENT_FILTER_ORDER;
    }

    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        URI url = (URI)exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
        String schemePrefix = (String)exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_SCHEME_PREFIX_ATTR);
        if (url == null || !"lb".equals(url.getScheme()) && !"lb".equals(schemePrefix)) {
            return chain.filter(exchange);
        } else {
            ServerWebExchangeUtils.addOriginalRequestUrl(exchange, url);
            log.trace("LoadBalancerClientFilter url before: " + url);
            ServiceInstance instance = this.loadBalancer.choose(url.getHost());
            if (instance == null) {
                throw new NotFoundException("Unable to find instance for " + url.getHost());
            } else {
                URI uri = exchange.getRequest().getURI();
                String overrideScheme = null;
                if (schemePrefix != null) {
                    overrideScheme = url.getScheme();
                }
                URI requestUrl = this.loadBalancer.reconstructURI(new LoadBalancerClientFilter.DelegatingServiceInstance(instance, overrideScheme), uri);
                log.trace("LoadBalancerClientFilter url chosen: " + requestUrl);
                exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, requestUrl);
                //重写Filter，将ServiceInstance写到路由上
                exchange.getAttributes().put(GateWayLogConstant.SERVICE_ID_IP, instance);
                return chain.filter(exchange);
            }
        }
    }

    class DelegatingServiceInstance implements ServiceInstance {

        final ServiceInstance delegate;
        private String overrideScheme;

        DelegatingServiceInstance(ServiceInstance delegate, String overrideScheme) {
            this.delegate = delegate;
            this.overrideScheme = overrideScheme;
        }

        public String getServiceId() {
            return this.delegate.getServiceId();
        }

        public String getHost() {
            return this.delegate.getHost();
        }

        public int getPort() {
            return this.delegate.getPort();
        }

        public boolean isSecure() {
            return this.delegate.isSecure();
        }

        public URI getUri() {
            return this.delegate.getUri();
        }

        public Map<String, String> getMetadata() {
            return this.delegate.getMetadata();
        }

        public String getScheme() {
            String scheme = this.delegate.getScheme();
            return scheme != null ? scheme : this.overrideScheme;
        }
    }
}
