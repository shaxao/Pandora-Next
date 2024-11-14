package com.lou.freegpt;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@SpringBootApplication
@EnableScheduling
@MapperScan(basePackages = "com.lou.freegpt.mapper")
public class FreeGptApplication implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(FreeGptApplication.class);

    @Autowired
    private DataSource mysqlDataSource;

    @Autowired
    private MongoClient mongoClient;

    public static void main(String[] args) {
        SpringApplication.run(FreeGptApplication.class, args);
    }
    // 注册插件
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor=new MybatisPlusInterceptor();
        //分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        // 防止全表更新与删除插件
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        return interceptor;
    }

    private void testMysqlConnection() {
        try (Connection connection = mysqlDataSource.getConnection()) {
            String databaseName = connection.getCatalog(); // 获取MySQL数据库名称
            logger.info("MySQL 数据库 [{}] 连接成功！", databaseName);
        } catch (SQLException e) {
            logger.error("MySQL 数据库连接失败！", e);
        }
    }

    private void testMongoConnection() {
        try {
            // 获取数据库名称，例如：chatdb
            String databaseName = mongoClient.getDatabase("chatdb").getName(); //  替换为你的MongoDB数据库名称
            MongoDatabase database = mongoClient.getDatabase(databaseName);
            // 执行一个简单的操作以测试连接，例如列出集合名称
            database.listCollectionNames();
            logger.info("MongoDB 数据库 [{}] 连接成功！", databaseName);
        } catch (Exception e) {
            logger.error("MongoDB 数据库连接失败！", e);
        }
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        testMysqlConnection();
        testMongoConnection();
    }
}
