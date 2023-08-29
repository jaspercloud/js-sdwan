package io.jaspercloud.sdwan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SpringBootApplication
public class WebTest {

    public static void main(String[] args) {
        SpringApplication.run(WebTest.class, args);
    }

    @GetMapping("/test")
    public String test() {
        return "hello world";
    }
}
