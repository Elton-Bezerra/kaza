package com.br.bz.kaza.kaza;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class KazaApplication {

    public static void main(String[] args) {
        SpringApplication.run(KazaApplication.class, args);
    }

}
