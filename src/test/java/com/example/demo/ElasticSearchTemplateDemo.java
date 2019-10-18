package com.example.demo;

import com.alibaba.fastjson.JSON;
import com.example.demo.dao.RouteLogDao;
import com.example.demo.entity.RouteLogVo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ElasticSearchTemplateDemo {
    @Autowired
    ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    RouteLogDao routeLogDao;


    /**
     * ElasticsearchTemplate创建索引
     */
    @Test
    public void contextLoads() {
        elasticsearchTemplate.deleteIndex(RouteLogVo.class);
        elasticsearchTemplate.createIndex(RouteLogVo.class);
        elasticsearchTemplate.putMapping(RouteLogVo.class);
    }

    /**
     * ElasticsearchTemplate增加数据
     * 使用构造器模式进行参数的填充
     */
    @Test
    public void addData(){
        //不设置id
        RouteLogVo routeLogVo=new RouteLogVo();
        routeLogVo.setServiceId("core-auth1");
        IndexQuery indexQuery=new IndexQueryBuilder().withObject(routeLogVo).build();
        elasticsearchTemplate.index(indexQuery);

        //设置id
        RouteLogVo routeLogVo2=new RouteLogVo();
        routeLogVo2.setServiceId("core-gateway1");
        IndexQuery indexQuery2=new IndexQueryBuilder().withId("2323").withObject(routeLogVo2).build();
        elasticsearchTemplate.index(indexQuery2);
        //增加多条数据
        List<IndexQuery> indexQueries= new ArrayList<>();
        RouteLogVo routeLogVo3=new RouteLogVo();
        routeLogVo3.setServiceId("core-llgssss");
        routeLogVo3.setEndTime(System.currentTimeMillis());
        Map map = new HashMap<String,String>();
        map.put("1","2");
        routeLogVo3.setQueryParams(map);
        IndexQuery indexQuery3=new IndexQueryBuilder().withObject(routeLogVo3).build();
        RouteLogVo routeLogVo4=new RouteLogVo();
        routeLogVo4.setServiceId("core-route2");
        IndexQuery indexQuery4=new IndexQueryBuilder().withObject(routeLogVo4).build();
        indexQueries.add(indexQuery3);
        indexQueries.add(indexQuery4);
        elasticsearchTemplate.bulkIndex(indexQueries);

    }

    /**
     * ElasticsearchTemplate删除数据
     */
    @Test
    public void deleteData(){
        //根据id查询
        elasticsearchTemplate.delete(RouteLogVo.class,"AW3S8brqYvu9np7fnsbS");
    }

    /**
     * 分词查询 + 按访问时间排序
     */
    @Test
    public void getData(){
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.queryStringQuery("127.0.0.1"))
                .withPageable(PageRequest.of(0,10,new Sort(Sort.Direction.DESC, "startTime")))
                .build();
        List<RouteLogVo> list = elasticsearchTemplate.queryForList(searchQuery, RouteLogVo.class);
    }

    /**
     * 使用spring data查询 和使用template 查询
     *
     */
    @Test
    public void getById(){
        Optional<RouteLogVo> routeLogVo =routeLogDao.findById("AW3TzBybYvu9np7fnsb5");
        RouteLogVo rv=routeLogVo.get();
        GetQuery getQuery = new GetQuery();
        getQuery.setId("AW3TzBybYvu9np7fnsb5");
        RouteLogVo routeLogVo2=elasticsearchTemplate.queryForObject(getQuery,RouteLogVo.class);
    }

    @Test
    public void testToObject(){
        String js="{\"ip\":null,\"url\":null,\"serviceId\":\"core-auth1\",\"queryParams\":null,\"requestHeaders\":null,\"requestBody\":null,\"responseParam\":null,\"stateCode\":null,\"startTime\":null,\"endTime\":null}";
        RouteLogVo routeLogVo=JSON.parseObject(js,RouteLogVo.class);
    }


    /**
     * 将 json字符串 转换为 java对象
     */
    @Test
    public  void conventJsonToObject() {
        String js="{\"ip\":null,\"url\":null,\"serviceId\":\"core-auth1\",\"queryParams\":null,\"requestHeaders\":null,\"requestBody\":null,\"responseParam\":null,\"stateCode\":null,\"startTime\":null,\"endTime\":null}";
        ObjectMapper objectMapper = new ObjectMapper();

        RouteLogVo user = null;
        try {
            user = objectMapper.readValue(js, RouteLogVo.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(user);
    }

    /**
     * 将 java对象 转换成 json 字符串
     */
    @Test
    public  void conventObjectToJson(){
        RouteLogVo routeLogVo=new RouteLogVo();
        routeLogVo.setEndTime(System.currentTimeMillis());
        //新建ObjectMapper对象
        ObjectMapper objectMapper = new ObjectMapper();
        //使用ObjectMapper对象对 User对象进行转换
        String json = null;
        RouteLogVo temp;
        try {
            json = objectMapper.writeValueAsString(routeLogVo);
            temp = objectMapper.readValue(json, RouteLogVo.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(json);
    }


}
