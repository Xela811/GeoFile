package com.geofile.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 分页插件配置
 *
 * @author xela
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 配置MyBatis-Plus分页插件
     * 使用物理分页，不会一次性查询所有数据到内存
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 添加分页插件
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        paginationInterceptor.setMaxLimit(500L); // 设置最大单页限制数量，默认500条，设置为-1不限制
        paginationInterceptor.setOverflow(false); // 溢出总页数后是否进行处理（默认false）

        interceptor.addInnerInterceptor(paginationInterceptor);
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }
}
