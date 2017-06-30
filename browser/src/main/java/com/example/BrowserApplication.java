package com.example;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.function.wiretap.Bridge;
import org.springframework.cloud.function.wiretap.DefaultBridge;
import org.springframework.context.annotation.Bean;

import reactor.core.publisher.Flux;

@SpringBootApplication
public class BrowserApplication {

    @Autowired
    private Bridge<Command> commandBridge;

    @Autowired
    private Bridge<Event> eventBridge;

    @Bean
    protected Bridge<Command> commandBridge() {
        return new DefaultBridge<>();
    }

    @Bean
    protected Bridge<Event> eventBridge() {
        return new DefaultBridge<>();
    }

    @Bean
    public Consumer<Flux<Command>> commands() {
        return commands -> commands.subscribe(command -> {
            commandBridge.send(command);
        });
    }

    @Bean
    @Qualifier("commands")
    public Supplier<Flux<Command>> exportCommands() {
        return () -> commandBridge.receive();
    }

    @Bean
    public Consumer<Flux<Event>> events() {
        return events -> events.subscribe(event -> {
            eventBridge.send(event);
        });
    }

    @Bean
    @Qualifier("events")
    public Supplier<Flux<Event>> exportEvents() {
        return () -> eventBridge.receive();
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(BrowserApplication.class).run(args);
    }
}
