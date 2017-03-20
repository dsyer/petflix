package org.spingframework.cloud.function.web;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.spingframework.cloud.function.web.ProductionConfigurationTests.TestApplication;
import org.spingframework.cloud.function.web.ProductionConfigurationTests.TestApplication.Bar;
import org.spingframework.cloud.function.web.ProductionConfigurationTests.TestApplication.Foo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = TestApplication.class)
public class ProductionConfigurationTests {

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
    public void get() throws Exception {
        assertThat(rest.getForObject("/proxy/0", Foo.class).getName()).isEqualTo("bye");
    }

    @Test
    public void path() throws Exception {
        assertThat(rest.getForObject("/proxy/path/1", Foo.class).getName()).isEqualTo("foo");
    }

    @Test
    public void missing() throws Exception {
        assertThat(rest.getForEntity("/proxy/missing/0", Foo.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    public void uri() throws Exception {
        assertThat(rest.getForObject("/proxy/0", Foo.class).getName()).isEqualTo("bye");
    }

    @Test
    public void post() throws Exception {
        assertThat(rest.postForObject("/proxy/0", Collections.singletonMap("name", "foo"),
                Bar.class).getName()).isEqualTo("foo");
    }

    @Test
    public void list() throws Exception {
        assertThat(rest.exchange(
                RequestEntity
                        .post(rest.getRestTemplate().getUriTemplateHandler().expand(
                                "/proxy"))
                .body(Collections.singletonList(Collections.singletonMap("name", "foo"))),
                new ParameterizedTypeReference<List<Bar>>() {
                }).getBody().iterator().next().getName()).isEqualTo("foo");
    }

    @Test
    public void bodyless() throws Exception {
        assertThat(rest.postForObject("/proxy/0", Collections.singletonMap("name", "foo"),
                Bar.class).getName()).isEqualTo("foo");
    }

    @Test
    public void entity() throws Exception {
        assertThat(rest.exchange(
                RequestEntity
                        .post(rest.getRestTemplate().getUriTemplateHandler()
                                .expand("/proxy/entity"))
                        .body(Collections.singletonMap("name", "foo")),
                new ParameterizedTypeReference<List<Bar>>() {
                }).getBody().iterator().next().getName()).isEqualTo("foo");
    }

    @Test
    public void single() throws Exception {
        assertThat(rest.postForObject("/proxy/single",
                Collections.singletonMap("name", "foobar"), Bar.class).getName())
                        .isEqualTo("foobar");
    }

    @SpringBootApplication
    @Controller
    static class TestApplication {

        @RestController
        static class ProxyController {

            private URI home;

            public void setHome(URI home) {
                this.home = home;
            }

            @GetMapping("/proxy/{id}")
            public ResponseEntity<?> proxyFoos(@PathVariable Integer id,
                    ProxyExchange proxy) throws Exception {
                return proxy.uri(home.toString() + "/foos/" + id).get();
            }

            @GetMapping("/proxy/path/**")
            public ResponseEntity<?> proxyPath(ProxyExchange proxy, UriComponentsBuilder uri) throws Exception {
                String path = proxy.path("/proxy/path/");
                return proxy.uri(home.toString() + "/foos/" + path).get();
            }

            @GetMapping("/proxy/missing/{id}")
            public ResponseEntity<?> proxyMissing(@PathVariable Integer id,
                    ProxyExchange proxy) throws Exception {
                return proxy.uri(home.toString() + "/missing/" + id).get();
            }

            @GetMapping("/proxy")
            public ResponseEntity<?> proxyUri(ProxyExchange proxy) throws Exception {
                return proxy.uri(home.toString() + "/foos").get();
            }

            @PostMapping("/proxy/{id}")
            public ResponseEntity<?> proxyBars(@PathVariable Integer id,
                    @RequestBody Map<String, Object> body, ProxyExchange proxy)
                            throws Exception {
                body.put("id", id);
                return proxy.uri(home.toString() + "/bars").body(Arrays.asList(body))
                        .postFirst();
            }

            @PostMapping("/proxy")
            public ResponseEntity<?> barsWithNoBody(ProxyExchange proxy)
                    throws Exception {
                return proxy.uri(home.toString() + "/bars").post();
            }

            @PostMapping("/proxy/entity")
            // TODO: support for ResponseEntity<List<Bar>>
            public ResponseEntity<?> explicitEntity(@RequestBody Foo foo,
                    ProxyExchange proxy) throws Exception {
                return proxy.uri(home.toString() + "/bars").body(Arrays.asList(foo))
                        .post();
            }

            @PostMapping("/proxy/single")
            public ResponseEntity<?> implicitEntity(@RequestBody Foo foo,
                    ProxyExchange proxy) throws Exception {
                return proxy.uri(home.toString() + "/bars").body(Arrays.asList(foo))
                        .postFirst();
            }

        }

        @Autowired
        private ProxyController controller;

        public void setHome(URI home) {
            controller.setHome(home);
        }

        @RestController
        static class TestController {

            @GetMapping("/foos")
            public List<Foo> foos() {
                return Arrays.asList(new Foo("hello"));
            }

            @GetMapping("/foos/{id}")
            public Foo foo(@PathVariable Integer id) {
                return new Foo(id==1 ? "foo" : "bye");
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