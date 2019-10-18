package com.example.demo.dao;

import com.example.demo.entity.RouteLogVo;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface RouteLogDao extends ElasticsearchRepository<RouteLogVo,String> {

}
