package com.example;

import java.util.function.Function;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;

import reactor.core.publisher.Flux;

@SpringBootApplication
public class DemoApplication {

    @Bean
    public Function<Flux<Integer>, Flux<Video>> videos() {
        return ids -> Flux.just(new Video("https://www.youtube.com/embed/Jqi6v7D4t8M"))
                .log();
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(DemoApplication.class).run(args);
    }
}

class Video {

    private String url;

    Video() {
    }

    public Video(String value) {
        this.url = value;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
