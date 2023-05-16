package com.example.minio;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MinioApplicationTests {

    @Test
    void contextLoads() {
        String a="http://49.234.54.56:9000/testy/2023/05/15/e38826a2a0214b8892b7c013eb5f2998.png?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=18227668465%2F20230515%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20230515T071948Z&X-Amz-Expires=86400&X-Amz-SignedHeaders=host&X-Amz-Signature=dbc3518744305353315e571d91ba5951f79f576c5a9cafbdf699d12dbc483563";

        int i = a.indexOf("?");
        String substring = a.substring(0, i);
        System.out.println(substring);
    }

    @Test
    void demo(){
        String property = System.getProperty("user.dir");
        System.out.println(property);
    }

}
