package com.rifas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Punto de entrada del sistema de rifas.
 *
 * @EnableJpaAuditing habilita los campos automáticos
 * createdAt / updatedAt en las entidades JPA.
 */
@SpringBootApplication
@EnableJpaAuditing
public class RifasApplication {

    public static void main(String[] args) {
        SpringApplication.run(RifasApplication.class, args);
    }
}
