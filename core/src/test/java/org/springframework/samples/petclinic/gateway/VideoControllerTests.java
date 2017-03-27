package org.springframework.samples.petclinic.gateway;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.WebClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.cloud.function.web.gateway.ProxyResponseAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.CoreMatchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test class for {@link OwnerController}
 *
 * @author Colin But
 */
@RunWith(SpringRunner.class)
@WebMvcTest(VideoController.class)
@Import({ ProxyResponseAutoConfiguration.class, WebClientAutoConfiguration.class })
@AutoConfigureWireMock(stubs = "classpath:META-INF/com.example/petflix-videos/0.0.1-SNAPSHOT/", port = 0)
@TestPropertySource(properties = "services.video.uri=http://localhost:${wiremock.server.port}")
public class VideoControllerTests {

    private static final int TEST_PET_ID = 0;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testGetVideo() throws Exception {
        mockMvc.perform(get("/owners/videos/{id}", TEST_PET_ID))
                .andExpect(status().isOk());
    }

    @Test
    public void testPostRating() throws Exception {
        mockMvc.perform(post("/owners/videos/{id}", TEST_PET_ID).content("{\"stars\":0}")
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    public void testGetTemplate() throws Exception {
        mockMvc.perform(get("/owners/videos/templates/pet.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Video of {{pet.name}}")));
    }

    @Test
    public void testGetScript() throws Exception {
        mockMvc.perform(get("/owners/videos/js/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(".module(\"")));
    }

}
