package com.example.demo;

import com.example.demo.entity.RouteLogVo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.Fields;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class MongodbTemplateDemo {
    @Autowired
    private MongoTemplate mongoTemplate;

    //添加数据
    @Test
    public void testAddUser() {
        RouteLogVo routeLogVo= new RouteLogVo();
        routeLogVo.setStateCode(600);
        routeLogVo.setStartTime(System.currentTimeMillis());
        mongoTemplate.save(routeLogVo);
    }

    //通过id查找数据
    @Test
    public void testFindOne() {
        RouteLogVo routeLogVo= mongoTemplate.findById("5da7d42a8f5b6875b88f9b8b",RouteLogVo.class);
        System.out.println(routeLogVo);
    }


    //查找所有数据
    @Test
    public void testFindAll() {
        List<RouteLogVo> users = mongoTemplate.findAll(RouteLogVo.class);
        System.out.println(users);
        //构建条件查询响应数据
        List<RouteLogVo> result = mongoTemplate.find(query(where("stateCode").gt(601)
                .and("startTime").gt(1571279914715L)), RouteLogVo.class);
    }

    //通过statecode更新某些数据,返回旧数据
    @Test
    public void testUpdate(){
        //更新前的where操作
        Query query = new Query(where("stateCode").is(601));
        //需要进行更新的操作set
        Update update = new Update().inc("stateCode", 1);
        //返回旧数据
        //RouteLogVo p = mongoTemplate.findAndModify(query, update, RouteLogVo.class); // return's old person object
        //返回新数据
        RouteLogVo p = mongoTemplate.findAndModify(query, update, new FindAndModifyOptions().returnNew(true), RouteLogVo.class);
    }

    //流操作
    @Test
    public void aggOne(){
        Aggregation agg = newAggregation(
                //可以筛选默写需要的字段，id默认返回
                project("serviceId","stateCode","requestHeaders"),
                //对数组进行相应切分（一星期切成7日）
                unwind("requestHeaders"),
                //获取serviceID分组相应次数，名称叫做stateCode
                group("serviceId").count().as("stateCode"),
                //匹配某些数据，类似having操作
                match(where("stateCode").gte(0)),
                project("stateCode").and("serviceId").previousOperation(),
                sort(DESC,"serviceId")
        );

        AggregationResults<RouteLogVo> results = mongoTemplate.aggregate(agg, "routeLogVo", RouteLogVo.class);
        List<RouteLogVo> tagCount = results.getMappedResults();
    }

    //流操作
    @Test
    public void aggTwo(){
        Aggregation agg = newAggregation(
                //可以筛选默写需要的字段，id默认返回
                project().and("serviceId").as("url")

        );

        AggregationResults<RouteLogVo> results = mongoTemplate.aggregate(agg, "routeLogVo", RouteLogVo.class);
        List<RouteLogVo> tagCount = results.getMappedResults();
    }

    @Test
    public void testDist(){
        TypedAggregation<RouteLogVo> aggregation = newAggregation(RouteLogVo.class,
                group("serviceId"),
                sort(ASC, "startTime")
//                group("state")
//                        .last("city").as("biggestCity")
//                        .last("pop").as("biggestPop")
//                        .first("city").as("smallestCity")
//                        .first("pop").as("smallestPop"),
//                project()
//                        .and("state").previousOperation()
//                        .and("biggestCity")
//                        .nested(bind("name", "biggestCity").and("population", "biggestPop"))
//                        .and("smallestCity")
//                        .nested(bind("name", "smallestCity").and("population", "smallestPop")),
//                sort(ASC, "state")
        );

        AggregationResults<RouteLogVo> result = mongoTemplate.aggregate(aggregation, RouteLogVo.class);
        RouteLogVo firstZipInfoStats = result.getMappedResults().get(0);
    }


}
