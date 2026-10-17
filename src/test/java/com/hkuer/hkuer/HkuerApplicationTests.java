package com.hkuer.hkuer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "mesh.mail-mode=local",
    "mesh.cloud-mode=local",
    "spring.datasource.url=jdbc:mysql://127.0.0.1:3306/hkuer_mesh_test?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai",
    "spring.data.redis.database=3",
    "mesh.redis-prefix=hkuer:mesh:test:",
    "mesh.dev.root=${HKUER_TEST_ROOT:.local/mesh-test}",
    "mesh.worker-id=2"
})
class HkuerApplicationTests {

    @Test
    void contextLoads() {
    }

}
