package de.ichsagnurweb.coopmap;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles(value = "test")
@Disabled
public class SampleDataRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MarkerRepository markerRepository;

    @Test
    public void testUserController() throws Exception {
        String mapId = "test";

        // Define the expected result or behavior
        String expectedResponse = "";


        assertThat(markerRepository.findAllByMapId(mapId)).isEmpty();

        // Perform POST request and verify the response
        mockMvc.perform(MockMvcRequestBuilders.post("/api/map/"+mapId+"/sampledata")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
               .andExpect(MockMvcResultMatchers.content().string(expectedResponse));

        assertThat(markerRepository.findAllByMapId(mapId)).hasSize(1);
    }

}
