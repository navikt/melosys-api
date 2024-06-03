package no.nav.melosys.tjenester.gui.aarsavregning

import com.ninjasquad.springmockk.MockkBean
import no.nav.melosys.service.sak.ÅrsavregningService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [ÅrsavregningTjeneste::class])
internal class ÅrsavregningTjenesteTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var årsavregningService: ÅrsavregningService

    @Test
    fun `hent avregning basert på ID`() {
        // TODO every { årsavregningService.hentÅrsavregning(any()) } returns Årsavregning()


        val expectedJson = """{
  "aar": 2023,
  "tidligereOpplysninger": {
    "trygdeavgiftsgrunnlag": {
      "medlemskapsperioder": [
        {
          "fom": "2023-01-01",
          "tom": "2023-07-31",
          "trygdedekning": "FTRL_2_9_FØRSTE_LEDD_B_PENSJON"
        },
        {
          "fom": "2023-08-01",
          "tom": "2023-12-31",
          "trygdedekning": "FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER"
        }
      ],
      "skatteforholdsperioder": [
        {
          "fom": "2023-01-01",
          "tom": "2023-07-31",
          "skatteplikttype": "SKATTEPLIKTIG"
        },
        {
          "fom": "2023-08-01",
          "tom": "2023-12-31",
          "skatteplikttype": "IKKE_SKATTEPLIKTIG"
        }
      ],
      "inntektskperioder": []
    },
    "avgift": {
      "trygdeavgiftsperioder": [
        {
          "fom": "2023-01-01",
          "tom": "2023-07-31",
          "inntektskildetype": "ARBEIDSINNTEKT_FRA_NORGE",
          "arbeidsgiversavgiftBetales": true,
          "inntektPerMd": 40000,
          "avgiftssats": 0.0,
          "avgiftPerMd": 0
        },
        {
          "fom": "2023-08-01",
          "tom": "2023-12-31",
          "inntektskildetype": "INNTEKT_FRA_UTLANDET",
          "arbeidsgiversavgiftBetales": false,
          "inntektPerMd": 15000,
          "avgiftssats": 42.2,
          "avgiftPerMd": 6330
        }
      ],
      "totalInntektPerMd": 690000,
      "totalAvgiftPerMd": 127020
    }
  },
  "avvikFunnet": true,
  "nyttGrunnlag": {
    "medlemskapsperioder": [],
    "skatteforholdsperioder": [
      {
        "fom": "2023-01-01",
        "tom": "2023-07-31",
        "skatteplikttype": "SKATTEPLIKTIG"
      },
      {
        "fom": "2023-08-01",
        "tom": "2023-12-31",
        "skatteplikttype": "IKKE_SKATTEPLIKTIG"
      }
    ],
    "inntektskperioder": [
      {
        "fom": "2023-01-01",
        "tom": "2023-07-31",
        "type": "ARBEIDSINNTEKT_FRA_NORGE",
        "arbeidsgiversavgiftBetales": true,
        "inntektPerMd": 95000
      }
    ]
  },
  "endeligAvgift": {
    "trygdeavgiftsperioder": [
      {
        "fom": "2023-01-01",
        "tom": "2023-07-31",
        "inntektskildetype": "ARBEIDSINNTEKT_FRA_NORGE",
        "arbeidsgiversavgiftBetales": true,
        "inntektPerMd": 40000,
        "avgiftssats": 0.0,
        "avgiftPerMd": 0
      },
      {
        "fom": "2023-08-01",
        "tom": "2023-12-31",
        "inntektskildetype": "INNTEKT_FRA_UTLANDET",
        "arbeidsgiversavgiftBetales": false,
        "inntektPerMd": 15000,
        "avgiftssats": 42.2,
        "avgiftPerMd": 6330
      }
    ],
    "totalInntektPerMd": 690000,
    "totalAvgiftPerMd": 127020
  },
  "avregning": {
    "nyttTotalbeloep": 24280,
    "tidligereFakturertBeloep": 21170,
    "tilFaktureringBeloep": 3110
  }
}"""

        mockMvc.perform(
            MockMvcRequestBuilders.get("$BASE_URL/{avregningID}", 1).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andExpect(content().json(expectedJson, true))
    }

    companion object {
        private const val BASE_URL: String = "/api/aarsavregninger"
    }
}
