package aml.openwlf.data.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA 설정 클래스
 *
 * JPA Auditing을 활성화하여 @CreatedDate, @LastModifiedDate 자동 관리
 */
@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "aml.openwlf.data.repository")
@EntityScan("aml.openwlf.data.entity")
public class JpaConfig {
}
