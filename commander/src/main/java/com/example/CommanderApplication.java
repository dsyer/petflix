package com.example;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;

import reactor.core.publisher.Flux;

@SpringBootApplication
public class CommanderApplication {

    private List<Command> commands = new ArrayList<>();

    private Map<String, Command> commandsById = new HashMap<>();

    private List<Event> events = new ArrayList<>();

    private Map<String, Event> eventsById = new HashMap<>();

    @Bean
    public Consumer<Flux<Command>> commands() {
        return commands -> commands.subscribe(command -> {
            if (commandsById.computeIfAbsent(command.getId(), id -> command) != null) {
                this.commands.add(command);
            }
        });
    }

    @Bean
    public Function<Flux<String>, Flux<Command>> storeCommands() {
        return ids -> ids.map(id -> commandsById.get(id));
    }

    @Bean
    public Supplier<Flux<Command>> replayCommands() {
        return () -> Flux.fromIterable(commands);
    }

    @Bean
    public Consumer<Flux<Event>> events() {
        return events -> events.filter( //
                event -> eventsById.containsKey(event.getParent())
                        || commandsById.containsKey(event.getParent()) //
        ).subscribe(event -> {
            if (eventsById.computeIfAbsent(event.getId(), id -> event) != null) {
                this.events.add(event);
            }
        });
    }

    @Bean
    public Function<Flux<String>, Flux<Event>> storeEvents() {
        return ids -> ids.map(id -> eventsById.get(id));
    }

    @Bean
    public Supplier<Flux<Event>> replayEvents() {
        return () -> Flux.fromIterable(events);
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(CommanderApplication.class).run(args);
    }
}

@JsonInclude(JsonInclude.Include.NON_EMPTY)
class Event extends Metadata<Event> {

    private String result;

    Event() {
    };

    public Event(String result) {
        this.result = result;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

}

@JsonInclude(JsonInclude.Include.NON_EMPTY)
class Command extends Metadata<Command> {

    private String action;

    Command() {
    }

    public Command(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

}

@JsonInclude(JsonInclude.Include.NON_EMPTY)
class Metadata<T extends Metadata<T>> {

    private String id = UUID.randomUUID().toString();

    private Date timestamp = new Date();

    private String parent;

    private Set<String> children;

    private Map<String, Object> data;

    Metadata() {
    }

    public Metadata(String id, Date timestamp, Map<String, Object> data) {
        this.id = id;
        this.timestamp = timestamp;
        this.data = data;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public T parent(String parent) {
        setParent(parent);
        @SuppressWarnings("unchecked")
        T result = (T) this;
        return result;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public T data(Map<String, Object> data) {
        this.data = data;
        @SuppressWarnings("unchecked")
        T result = (T) this;
        return result;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public Set<String> getChildren() {
        return children;
    }

    public void setChildren(Set<String> children) {
        this.children = children;
    }
}
