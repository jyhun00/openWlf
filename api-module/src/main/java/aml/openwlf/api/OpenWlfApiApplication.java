package aml.openwlf.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = {
        "aml.openwlf.api",
        "aml.openwlf.core",
        "aml.openwlf.data",
        "aml.openwlf.batch"
})
@EnableScheduling
public class OpenWlfApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenWlfApiApplication.class, args);
    }
}
