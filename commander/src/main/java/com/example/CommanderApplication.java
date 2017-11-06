package com.example;

import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.function.context.catalog.FunctionInspector;
import org.springframework.cloud.function.core.FunctionCatalog;
import org.springframework.cloud.function.stream.StreamAutoConfiguration;
import org.springframework.cloud.function.stream.StreamConfigurationProperties;
import org.springframework.cloud.function.stream.StreamListeningFunctionInvoker;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.converter.CompositeMessageConverterFactory;
import org.springframework.cloud.stream.reactive.FluxSender;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
