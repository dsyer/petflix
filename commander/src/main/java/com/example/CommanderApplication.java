package com.example;

import java.util.function.Function;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;

import reactor.core.publisher.Flux;

@SpringBootApplication
public class CommanderApplication {

    @Bean
    public Function<Command, Command> commands() {
        return command -> command;
    }

    @Bean
    public Function<Flux<Event>, Flux<Event>> events() {
        return events -> events.log();
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(CommanderApplication.class).run(args);
    }
}
