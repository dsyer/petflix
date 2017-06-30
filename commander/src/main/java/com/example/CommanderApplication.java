package com.example;

import java.util.function.Consumer;
import java.util.function.Function;
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
public class CommanderApplication {

    @Autowired
    private Repository<Command> commands;

    @Autowired
    private Repository<Event> events;

    @Bean
    protected Bridge<Command> commandBridge() {
        return new DefaultBridge<>();
    }

    @Bean
    protected Bridge<Event> eventBridge() {
        return new DefaultBridge<>();
    }

    @Bean
    public Consumer<Command> commands() {
        return this.commands::add;
    }

    @Bean
    @Qualifier("commands")
    public Function<String, Command> storeCommands() {
        return this.commands::get;
    }

    @Bean
    @Qualifier("commands")
    public Supplier<Flux<Command>> replayCommands() {
        return () -> Flux.fromIterable(commands.get());
    }

    @Bean
    public Supplier<Flux<Command>> exportCommands(Bridge<Command> commandBridge) {
        return () -> commandBridge.receive();
    }

    @Bean
    public Consumer<Flux<Event>> events() {
        return events -> events.log().filter( //
                event -> this.events.get(event.getParent()) != null
                        || this.commands.get(event.getParent()) != null //
        ).subscribe(this.events::add);
    }

    @Bean
    @Qualifier("events")
    public Function<String, Event> storeEvents() {
        return events::get;
    }

    @Bean
    @Qualifier("events")
    public Supplier<Flux<Event>> replayEvents() {
        return () -> Flux.fromIterable(events.get());
    }

    @Bean
    public Supplier<Flux<Event>> exportEvents(Bridge<Event> eventBridge) {
        return () -> eventBridge.receive();
    }

    @Bean
    public SimpleRepository<Command> commandRepository(Bridge<Command> bridge) {
        return new SimpleRepository<Command>(bridge);
    }

    @Bean
    public SimpleRepository<Event> eventRepository(Bridge<Event> bridge) {
        return new SimpleRepository<Event>(bridge);
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(CommanderApplication.class).run(args);
    }
}
