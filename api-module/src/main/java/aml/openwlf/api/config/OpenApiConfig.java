package aml.openwlf.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration
 */
@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Watchlist Filtering System API")
                        .version("1.0.0")
                        .description("Enterprise-grade watchlist filtering system for financial compliance. " +
                                "Screens customers against OFAC, UN, EU, and other regulatory watchlists.")
                        .contact(new Contact()
                                .name("AML Compliance Team")
                                .email("compliance@openwlf.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}
