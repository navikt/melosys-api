package no.nav.melosys.tjenester.gui.aarsavregning

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.service.avgift.aarsavregning.MedlemskapsperiodeForAvgift
import no.nav.melosys.service.avgift.aarsavregning.Trygdeavgiftsgrunnlag
import no.nav.melosys.service.avgift.aarsavregning.Årsavregning
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDate

@WebMvcTest(controllers = [ÅrsavregningTjeneste::class])
internal class ÅrsavregningTjenesteTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var årsavregningService: ÅrsavregningService

    @Test
    fun `hent avregning basert på ID`() {
        every { årsavregningService.hentÅrsavregning(any()) } returns Årsavregning(
            år = 2023,
            tidligereGrunnlag = Trygdeavgiftsgrunnlag(
                medlemskapsperioder = listOf(
                    MedlemskapsperiodeForAvgift(
                        LocalDate.parse("2023-01-01"),
                        LocalDate.parse("2023-07-31"),
                        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON,
                        FTRL_KAP2_2_8
                    ),
                    MedlemskapsperiodeForAvgift(
                        LocalDate.parse("2023-08-01"),
                        LocalDate.parse("2023-12-31"),
                        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER,
                        FTRL_KAP2_2_8
                    )
                ),
                skatteforholdsperioder = listOf(
                    SkatteforholdTilNorge().apply {
                        fomDato = LocalDate.parse("2023-01-01")
                        tomDato = LocalDate.parse("2023-07-31")
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    },
                    SkatteforholdTilNorge().apply {
                        fomDato = LocalDate.parse("2023-08-01")
                        tomDato = LocalDate.parse("2023-12-31")
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    },
                ),
                innteksperioder = listOf(
                    Inntektsperiode().apply {
                        fomDato = LocalDate.parse("2023-01-01")
                        tomDato = LocalDate.parse("2023-07-31")
                        type = Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE
                        isArbeidsgiversavgiftBetalesTilSkatt = true
                        avgiftspliktigInntektMnd = Penger(40000.0)
                    },
                    Inntektsperiode().apply {
                        fomDato = LocalDate.parse("2023-08-01")
                        tomDato = LocalDate.parse("2023-12-31")
                        type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                        isArbeidsgiversavgiftBetalesTilSkatt = false
                        avgiftspliktigInntektMnd = Penger(15000.0)
                    }
                )
            ),
            tidligereAvgift = listOf(
                Trygdeavgiftsperiode().apply {
                    periodeFra = LocalDate.parse("2023-01-01")
                    periodeTil = LocalDate.parse("2023-07-31")
                    grunnlagInntekstperiode = Inntektsperiode().apply {
                        fomDato = LocalDate.parse("2023-01-01")
                        tomDato = LocalDate.parse("2023-07-31")
                        type = Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE
                        isArbeidsgiversavgiftBetalesTilSkatt = true
                        avgiftspliktigInntektMnd = Penger(40000.0)
                    }
                    trygdesats = BigDecimal(0.0)
                    trygdeavgiftsbeløpMd = Penger(0.0)
                },
                Trygdeavgiftsperiode().apply {
                    periodeFra = LocalDate.parse("2023-08-01")
                    periodeTil = LocalDate.parse("2023-12-31")
                    grunnlagInntekstperiode = Inntektsperiode().apply {
                        fomDato = LocalDate.parse("2023-01-01")
                        tomDato = LocalDate.parse("2023-07-31")
                        type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                        isArbeidsgiversavgiftBetalesTilSkatt = false
                        avgiftspliktigInntektMnd = Penger(15000.0)
                    }
                    trygdesats = BigDecimal(42.2)
                    trygdeavgiftsbeløpMd = Penger(6330.0)
                }
            ),
            nyttGrunnlag = null,
            endeligAvgift = emptyList(),
            tidligereFakturertBeloep = BigDecimal(21170.0),
            nyttTotalbeloep = BigDecimal(24280.0),
            tilFaktureringBeloep = BigDecimal(3110.0)
        )
        every { årsavregningService.beregnTotalbeløpForPeriode(any()) } returns BigDecimal(42)


        val expectedJson = """{
  "aar": 2023,
  "tidligereGrunnlagsopplysninger": {
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
      "inntektskperioder": [
        {
          "fom": "2023-01-01",
          "tom": "2023-07-31",
          "type": "ARBEIDSINNTEKT_FRA_NORGE",
          "arbeidsgiversavgiftBetales": true,
          "inntektPerMd": 40000
        },
        {
          "fom": "2023-08-01",
          "tom": "2023-12-31",
          "type": "INNTEKT_FRA_UTLANDET",
          "arbeidsgiversavgiftBetales": false,
          "inntektPerMd": 15000
        }
      ]
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
      "totalInntekt": 42,
      "totalAvgift": 42
    }
  },
  "avvikFunnet": false,
  "nyttGrunnlag": null,
  "endeligAvgift": null,
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
