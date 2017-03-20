package org.springframework.cloud.function.web;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.function.web.ProductionConfigurationTests.TestApplication.Bar;
import org.springframework.cloud.function.web.SimpleHandlerTests.TestApplication;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = TestApplication.class)
public class SimpleHandlerTests {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private TestApplication application;

    @LocalServerPort
    private int port;

    @Before
    public void init() throws Exception {
        application.setHome(new URI("http://localhost:" + port));
    }

    @Test
    public void post() throws Exception {
        assertThat(rest.exchange(
                RequestEntity
                        .post(rest.getRestTemplate().getUriTemplateHandler()
                                .expand("/proxy/single"))
                        .body(Collections.singletonMap("name", "foobar")),
                new ParameterizedTypeReference<List<Bar>>() {
                }).getBody().iterator().next().getName()).isEqualTo("foobar");
    }

    @SpringBootApplication
    @RestController
    static class TestApplication {

        private URI home;
        private RestTemplate rest;

        TestApplication(RestTemplateBuilder builder) {
            this.rest = builder.build();
        }

        public void setHome(URI home) {
            this.home = home;
        }

        @PostMapping("/proxy/single")
        public ResponseEntity<List<Bar>> implicitEntity(@RequestBody Foo foo)
                throws Exception {
            URI uri = new URI(home.toString() + "/bars");
            return rest.exchange(RequestEntity.post(uri).body(Arrays.asList(foo)),
                    new ParameterizedTypeReference<List<Bar>>() {
                    });
        }

        @RestController
        static class TestController {

            @GetMapping("/foos")
            public List<Foo> foos() {
                return Arrays.asList(new Foo("hello"));
            }

            @PostMapping("/bars")
            public List<Bar> bars(@RequestBody List<Foo> foos) {
                return Arrays.asList(new Bar(foos.iterator().next().getName()));
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        static class Foo {
            private String name;

            public Foo() {
            }

            public Foo(String name) {
                this.name = name;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        static class Bar {
            private String name;

            public Bar() {
            }

            public Bar(String name) {
                this.name = name;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }
        }

    }

}