package com.example;

import java.util.Random;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.stream.binder.servlet.MessageController;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;

@SpringBootApplication
public class VideoApplication {

    @Autowired
    private VideoRepository repo;

    @Bean
    public Function<Integer, Video> videos() {
        return id -> repo.findById(id);
    }

    @Bean
    public Function<Flux<Message<Command>>, Flux<Message<Event>>> commands() {
        return commands -> commands
                .filter(command -> "rate-video".equals(command.getPayload().getAction()))
                .map(command -> {
                    // rate it...
                    return MessageBuilder
                            .withPayload(new Event("rated-video")
                                    .data(command.getPayload().getData())
                                    .parent(command.getPayload().getId()))
                            .copyHeaders(command.getHeaders())
                            .setHeader(MessageController.ROUTE_KEY, "events").build();
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
