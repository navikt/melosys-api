package no.nav.melosys.tjenester.gui

import com.ninjasquad.springmockk.MockkBean
import io.getunleash.Unleash
import io.mockk.every
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [FeatureToggleController::class])
class FeatureToggleControllerKtTest {

    @MockkBean
    private lateinit var unleash: Unleash

    @Autowired
    private lateinit var mockMvc: MockMvc

    companion object {
        private const val BASE_URL = "/api/featuretoggle"
    }

    @Test
    fun hentFeatureToggle() {
        val featureEn = "melosys.feature.en"
        val featureTo = "melosys.feature.to"
        every { unleash.isEnabled(featureEn) } returns true
        every { unleash.isEnabled(featureTo) } returns false


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
