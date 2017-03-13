package org.spingframework.cloud.function.web;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
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
        assertThat(rest.getForObject("/proxy/0", Foo.class).getName()).isEqualTo("hello");
    }

    @Test
    public void uri() throws Exception {
        assertThat(rest.getForObject("/proxy", Foo.class).getName()).isEqualTo("hello");
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
        assertThat(rest.postForObject("/proxy", Collections.singletonMap("name", "foo"),
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
}

@SpringBootApplication
@Controller
class TestApplication {

    private URI home;

    public void setHome(URI home) {
        this.home = home;
    }

    @GetMapping("/proxy/{id}")
    public Supplier<String> proxyFoos(@PathVariable Integer id) throws Exception {
        return () -> home.toString() + "/foos";
    }

    @GetMapping("/proxy")
    public Supplier<URI> proxyUri() throws Exception {
        URI uri = new URI(home.toString() + "/foos");
        return () -> uri;
    }

    @PostMapping("/proxy/{id}")
    public Supplier<String> proxyBars(@PathVariable Integer id,
            @RequestBody Map<String, Object> body) throws Exception {
        body.put("id", id);
        return () -> home.toString() + "/bars";
    }

    @PostMapping("/proxy")
    public Supplier<String> barsWithNoBody() throws Exception {
        return () -> home.toString() + "/bars";
    }

    @PostMapping("/proxy/entity")
    public Supplier<RequestEntity<List<Foo>>> explicitEntity(@RequestBody Foo foo)
            throws Exception {
        URI uri = new URI(home.toString() + "/bars");
        return () -> RequestEntity.post(uri).body(Arrays.asList(foo));
    }

    @PostMapping("/proxy/single")
    public Supplier<RequestEntity<Foo>> implicitEntity(@RequestBody Foo foo)
            throws Exception {
        URI uri = new URI(home.toString() + "/bars");
        return () -> RequestEntity.post(uri).body(foo);
    }
}

@RestController
class TestController {

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
class Foo {
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
class Bar {
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