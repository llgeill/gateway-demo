package com.example.demo;


import com.example.demo.dao.RouteLogMongoDao;
import com.example.demo.entity.RouteLogVo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class MongodbRepositoryDemo {

    @Autowired
    RouteLogMongoDao routeLogMongoDao;

    @Test
    public void save(){
        RouteLogVo routeLogVo = new RouteLogVo();
        routeLogVo.setServiceId("mongodb_test");
        routeLogVo.setStateCode(500);
        routeLogMongoDao.save(routeLogVo);
    }

    @Test
    public void delete(){
        RouteLogVo routeLogVo = new RouteLogVo();
        routeLogVo.setServiceId("mongodb_test");
        routeLogVo.setStateCode(500);
        routeLogMongoDao.delete(routeLogVo);
    }
}
