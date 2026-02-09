package aml.openwlf.api.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;
    private long accessTokenExpirationMs = 3600000;
    private long refreshTokenExpirationMs = 86400000;
    private String issuer = "openWLF";
}
