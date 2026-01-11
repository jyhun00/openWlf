package aml.openwlf.data.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "aml.openwlf.data.repository")
@EntityScan(basePackages = "aml.openwlf.data.entity")
public class JpaConfig {
}
