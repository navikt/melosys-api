package no.nav.melosys.tjenester.gui;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.finn.unleash.FakeUnleash;
import no.finn.unleash.Unleash;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static no.nav.melosys.tjenester.gui.util.ResponseBodyMatchers.responseBody;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {FeatureToggleTjeneste.class})
class FeatureToggleTjenesteTest {

    @MockBean
    private Unleash unleash;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/featuretoggle";

    @Test
    void hentFeatureToggle_alleEnabled_verifiserAlleErEnablet() throws Exception {
        String featureEn = "melosys.feature.en";
        String featureTo = "melosys.feature.to";

        when(unleash.isEnabled(featureEn)).thenReturn(true);
        when(unleash.isEnabled(featureTo)).thenReturn(false);

        mockMvc.perform(get(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .param("features", "melosys.feature.en, melosys.feature.to")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$['melosys.feature.en']", equalTo(true)))
            .andExpect(jsonPath("$['melosys.feature.to']", equalTo(false)));
    }

}
