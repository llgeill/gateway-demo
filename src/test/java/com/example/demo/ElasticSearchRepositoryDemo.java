package com.example.demo;

import com.example.demo.dao.RouteLogDao;
import com.example.demo.entity.RouteLogVo;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.metrics.avg.InternalAvg;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ElasticSearchRepositoryDemo {
    @Autowired
    ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    RouteLogDao routeLogDao;

    /**
     * 使用spring data 添加数据
     */
    @Test
    public void save() {
        RouteLogVo routeLogVo = new RouteLogVo();
        routeLogVo.setServiceId("llg-test");
        routeLogVo.setStateCode(666);
        routeLogVo.setStartTime(System.currentTimeMillis());
        routeLogDao.save(routeLogVo);
    }


    /**
     * 批量新增
     */
    @Test
    public void indexList() {
        List<RouteLogVo> list = new ArrayList<>();
        RouteLogVo routeLogVo = new RouteLogVo();
        routeLogVo.setServiceId("llg-test-1");
        routeLogVo.setStateCode(777);
        routeLogVo.setStartTime(System.currentTimeMillis());
        list.add(routeLogVo);
        RouteLogVo routeLogVo2 = new RouteLogVo();
        routeLogVo2.setServiceId("llg-test-1");
        routeLogVo2.setStateCode(777);
        routeLogVo2.setStartTime(System.currentTimeMillis());
        list.add(routeLogVo2);
        // 接收对象集合，实现批量新增
        routeLogDao.saveAll(list);
    }

    /**
     * 查询所有数据
     */
    @Test
    public void testFind() {
        // 查询全部，并安装价格降序排序
        Iterable<RouteLogVo> items = this.routeLogDao.findAll(Sort.by(Sort.Direction.DESC, "startTime"));
        items.forEach(item -> System.out.println(item));
    }

    /**
     * 高级之基本查询
     */
    @Test
    public void testQuery() {
        // 词条查询
        MatchQueryBuilder queryBuilder = QueryBuilders.matchQuery("serviceId", "llg-test-1");
        // 执行查询
        Iterable<RouteLogVo> items = this.routeLogDao.search(queryBuilder);
        items.forEach(System.out::println);
    }

    /**
     * 高级之基本查询(分页)
     */
    @Test
    public void testQueryPage() {
        // 词条查询
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        // 添加基本的分词查询
        queryBuilder.withQuery(QueryBuilders.matchQuery("serviceId", "llg-test-1"));
        // 执行搜索，获取结果
        Page<RouteLogVo> items = this.routeLogDao.search(queryBuilder.build());
        // 打印总条数
        System.out.println(items.getTotalElements());
        // 打印总页数
        System.out.println(items.getTotalPages());
        items.forEach(System.out::println);
    }


    /**
     * 高级之基本查询之自定义分页
     */
    @Test
    public void testNativeQuery() {
        // 构建查询条件
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        // 添加基本的分词查询
        queryBuilder.withQuery(QueryBuilders.termQuery("serviceId", "llg-test-1"));

        // 初始化分页参数
        int page = 0;
        int size = 3;
        // 设置分页参数
        queryBuilder.withPageable(PageRequest.of(page, size));
        // 执行搜索，获取结果
        Page<RouteLogVo> items = this.routeLogDao.search(queryBuilder.build());
        // 打印总条数
        System.out.println(items.getTotalElements());
        // 打印总页数
        System.out.println(items.getTotalPages());
        // 每页大小
        System.out.println(items.getSize());
        // 当前页
        System.out.println(items.getNumber());
        items.forEach(System.out::println);
    }

    @Test
    public void testSort() {
        // 构建查询条件
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        // 添加基本的分词查询
        queryBuilder.withQuery(QueryBuilders.termQuery("serviceId", "llg-test-1"));
        // 排序
        queryBuilder.withSort(SortBuilders.fieldSort("startTime").order(SortOrder.DESC));
        // 执行搜索，获取结果
        Page<RouteLogVo> items = this.routeLogDao.search(queryBuilder.build());
        // 打印总条数
        System.out.println(items.getTotalElements());
        items.forEach(System.out::println);
    }


    //根据状态聚合
    @Test
    public void testAgg(){
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        // 不查询任何结果
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{""}, null));
        // 1、添加一个新的聚合，聚合类型为terms，聚合名称为brands，聚合字段为brand
        queryBuilder.addAggregation(
                AggregationBuilders.terms("stateCode").field("stateCode"));
        // 2、查询,需要把结果强转为AggregatedPage类型
        AggregatedPage<RouteLogVo> aggPage = (AggregatedPage<RouteLogVo>) this.routeLogDao.search(queryBuilder.build());
        // 3、解析
        // 3.1、从结果中取出名为serviceId的那个聚合，
        // 因为是利用String类型字段来进行的term聚合，所以结果要强转为StringTerm类型
        LongTerms agg = (LongTerms) aggPage.getAggregation("stateCode");
        // 3.2、获取桶
        List<LongTerms.Bucket> buckets = agg.getBuckets();
        // 3.3、遍历
        for (LongTerms.Bucket bucket : buckets) {
            // 3.4、获取桶中的key，即品牌名称
            System.out.println(bucket.getKeyAsString());
            // 3.5、获取桶中的文档数量
            System.out.println(bucket.getDocCount());
        }

    }


    /**
     * 根据服务名称聚合
     */
    @Test
    public void testAggService(){
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        // 不查询任何结果
        //queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{""}, null));
        // 1、添加一个新的聚合，聚合类型为terms，聚合名称为brands，聚合字段为brand
        queryBuilder.addAggregation(
                AggregationBuilders.terms("serviceId").field("serviceId"));
        // 2、查询,需要把结果强转为AggregatedPage类型
        AggregatedPage<RouteLogVo> aggPage = (AggregatedPage<RouteLogVo>) this.routeLogDao.search(queryBuilder.build());
        // 3、解析
        // 3.1、从结果中取出名为serviceId的那个聚合，
        // 因为是利用String类型字段来进行的term聚合，所以结果要强转为StringTerm类型
        StringTerms agg = (StringTerms) aggPage.getAggregation("serviceId");
        // 3.2、获取桶
        List<StringTerms.Bucket> buckets = agg.getBuckets();
        // 3.3、遍历
        for (StringTerms.Bucket bucket : buckets) {
            // 3.4、获取桶中的key，即品牌名称
            System.out.println(bucket.getKeyAsString());
            // 3.5、获取桶中的文档数量
            System.out.println(bucket.getDocCount());
        }

    }

    /**
     * 求平均值
     */
    @Test
    public void testSubAgg(){
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        // 不查询任何结果
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{""}, null));
        // 1、添加一个新的聚合，聚合类型为terms，聚合名称为brands，聚合字段为brand
        queryBuilder.addAggregation(
                AggregationBuilders.terms("stateCode").field("stateCode")
                        .subAggregation(AggregationBuilders.avg("priceAvg").field("stateCode")) // 在品牌聚合桶内进行嵌套聚合，求平均值
        );
        // 2、查询,需要把结果强转为AggregatedPage类型
        AggregatedPage<RouteLogVo> aggPage = (AggregatedPage<RouteLogVo>) this.routeLogDao.search(queryBuilder.build());
        // 3、解析
        // 3.1、从结果中取出名为brands的那个聚合，
        // 因为是利用String类型字段来进行的term聚合，所以结果要强转为StringTerm类型
        LongTerms agg = (LongTerms) aggPage.getAggregation("stateCode");
        // 3.2、获取桶
        List<LongTerms.Bucket> buckets = agg.getBuckets();
        // 3.3、遍历
        for (LongTerms.Bucket bucket : buckets) {
            // 3.4、获取桶中的key，即品牌名称  3.5、获取桶中的文档数量
            System.out.println(bucket.getKeyAsString() + "，共" + bucket.getDocCount() + "台");

            // 3.6.获取子聚合结果：
            InternalAvg avg = (InternalAvg) bucket.getAggregations().asMap().get("priceAvg");
            System.out.println("平均售价：" + avg.getValue());
        }

    }

}
