package no.nav.melosys.tjenester.gui

import io.getunleash.Unleash
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [FeatureToggleController::class])
class FeatureToggleControllerKtTest {

    @MockBean
    private lateinit var unleash: Unleash

    @Autowired
    private lateinit var mockMvc: MockMvc

    private val BASE_URL = "/api/featuretoggle"

    @Test
    fun hentFeatureToggle() {
        val featureEn = "melosys.feature.en"
        val featureTo = "melosys.feature.to"

        `when`(unleash.isEnabled(featureEn)).thenReturn(true)
        `when`(unleash.isEnabled(featureTo)).thenReturn(false)

        mockMvc.perform(
            get(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .param("features", "melosys.feature.en, melosys.feature.to")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$['melosys.feature.en']", equalTo(true)))
            .andExpect(jsonPath("$['melosys.feature.to']", equalTo(false)))
    }
}
