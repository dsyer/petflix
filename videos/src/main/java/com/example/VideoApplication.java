package com.example;

import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;

@SpringBootApplication
public class VideoApplication {

    @Autowired
    private VideoRepository repo;

    @Bean
    public Function<Flux<Integer>, Flux<Video>> videos() {
        return ids -> ids.map(id -> repo.findById(id)).log();
    }

    @Bean
    public Consumer<Flux<Command>> commands() {
        return commands -> commands.subscribe(command -> {
            System.err.println(command);
            if ("rate-video".equals(command.getAction())) {
                // rate it...
            }
        });
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(VideoApplication.class).run(args);
    }
}

interface VideoRepository {
    Video findById(Integer id);
}

@Component
class DumbVideoRepository implements VideoRepository {

    @Override
    public Video findById(Integer id) {
        return new Video(id, "https://www.youtube.com/embed/Jqi6v7D4t8M");
    }

}

class Video {

    private String url;
    private int stars;

    Video(int id, String value, int stars) {
        url = value;
        this.stars = stars;
    }

    Video(int id, String value) {
        this.url = value;
        this.stars = calculateStars(id);
    }

    private int calculateStars(int id) {
        return new Random((long) id).nextInt(6);
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

    public Video rate(int stars) {
        this.stars = stars;
        return this;
    }
}

class Rating {

    private int stars;

    private int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getStars() {
        return stars;
    }

    public void setStars(int stars) {
        this.stars = stars;
    }
}
