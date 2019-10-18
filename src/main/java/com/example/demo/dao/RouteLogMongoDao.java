package com.example.demo.dao;

import com.example.demo.entity.RouteLogVo;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface RouteLogMongoDao extends MongoRepository<RouteLogVo,String> {



}
