package com.example;

import java.util.Random;
import java.util.function.Function;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;

import reactor.core.publisher.Flux;

@SpringBootApplication
public class DemoApplication {

    @Bean
    public Function<Flux<Integer>, Flux<Video>> videos() {
        return ids -> ids.map(id -> new Video(id, "https://www.youtube.com/embed/Jqi6v7D4t8M"))
                .log();
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(DemoApplication.class).run(args);
    }
}

class Video {

    private String url;
    private int stars;

    Video(int id, String value) {
        this.url = value;
        this.stars = calculateStars(id);
    }

    private int calculateStars(int id) {
        return new Random((long)id).nextInt(6);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
    
    public int getStars() {
        return stars;
    }

    public void setStars(int stars) {
        this.stars = stars;
    }
}
