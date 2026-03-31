package com.cmbchina.datadirect.caliber.infrastructure.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI dataDirectOpenApi(@Value("${info.app.version:0.1.0}") String version) {
        return new OpenAPI()
                .info(new Info()
                        .title("数据直通车 API")
                        .description("数据直通车前后端契约输出，覆盖知识生产、数据地图、运行决策与系统配置接口。")
                        .version(version)
                        .license(new License().name("Internal Use Only")))
                .servers(List.of(new Server().url("/").description("Current environment")));
    }
}
