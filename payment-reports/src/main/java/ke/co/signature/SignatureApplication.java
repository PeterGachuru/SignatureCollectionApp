package ke.co.signature;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class SignatureApplication {
    public static void main(String[] args) {
        SpringApplication.run(SignatureApplication.class, args);
    }
}
