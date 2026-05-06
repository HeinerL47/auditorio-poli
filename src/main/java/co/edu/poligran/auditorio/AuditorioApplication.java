package co.edu.poligran.auditorio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AuditorioApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuditorioApplication.class, args);
    }
}
