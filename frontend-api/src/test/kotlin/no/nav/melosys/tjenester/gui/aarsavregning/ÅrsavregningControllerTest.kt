package no.nav.melosys.tjenester.gui.aarsavregning

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.service.avgift.aarsavregning.*
import no.nav.melosys.service.tilgang.Aksesskontroll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDate

@WebMvcTest(controllers = [ÅrsavregningController::class])
internal class ÅrsavregningControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var årsavregningService: ÅrsavregningService

    @MockBean
    private lateinit var aksesskontroll: Aksesskontroll

    @Test
    fun `hent avregning basert på ID`() {
        every { årsavregningService.finnÅrsavregningForBehandling(any()) } returns ÅrsavregningModel(
            år = 2023,
            tidligereGrunnlag = Trygdeavgiftsgrunnlag(
                medlemskapsperioder = listOf(
                    MedlemskapsperiodeForAvgift(
                        LocalDate.parse("2023-01-01"),
                        LocalDate.parse("2023-07-31"),
                        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON,
                        FTRL_KAP2_2_8,
                        Medlemskapstyper.PLIKTIG
                    ),
                    MedlemskapsperiodeForAvgift(
                        LocalDate.parse("2023-08-01"),
                        LocalDate.parse("2023-12-31"),
                        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER,
                        FTRL_KAP2_2_8,
                        Medlemskapstyper.PLIKTIG
                    )
                ),
                skatteforholdsperioder = listOf(
                    SkatteforholdTilNorgeForAvgift(
                        fom = LocalDate.parse("2023-01-01"),
                        tom = LocalDate.parse("2023-07-31"),
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    ),

                    SkatteforholdTilNorgeForAvgift(
                        fom = LocalDate.parse("2023-08-01"),
                        tom = LocalDate.parse("2023-12-31"),
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    )
                ),
                innteksperioder = listOf(
                    InntektsperioderForAvgift(
                        fom = LocalDate.parse("2023-01-01"),
                        tom = LocalDate.parse("2023-07-31"),
                        type = Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE,
                        avgiftspliktigInntekt = Penger(40000.0),
                        avgiftspliktigTotalInntekt = Penger(280000.0),
                        isArbeidsgiversavgiftBetalesTilSkatt = true,
                        erMaanedsbelop = true
                    ),
                    InntektsperioderForAvgift(
                        fom = LocalDate.parse("2023-08-01"),
                        tom = LocalDate.parse("2023-12-31"),
                        type = Inntektskildetype.INNTEKT_FRA_UTLANDET,
                        avgiftspliktigInntekt = Penger(15000.0),
                        avgiftspliktigTotalInntekt = Penger(75000.0),
                        isArbeidsgiversavgiftBetalesTilSkatt = false,
                        erMaanedsbelop = true
                    )
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
                        avgiftspliktigMndInntekt = Penger(40000.0)
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
                        avgiftspliktigMndInntekt = Penger(15000.0)
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


        val expectedJson = """{
  "aar": 2023,
  "tidligereGrunnlagsopplysninger": {
    "trygdeavgiftsgrunnlag": {
      "medlemskapsperioder": [
        {
          "id": 0,
          "fomDato": "2023-01-01",
          "tomDato": "2023-07-31",
          "bestemmelse": "FTRL_KAP2_2_8",
          "innvilgelsesResultat": "INNVILGET",
          "trygdedekning": "FTRL_2_9_FØRSTE_LEDD_B_PENSJON",
          "medlemskapstype": "PLIKTIG"
        },
        {
          "id": 0,
          "fomDato": "2023-08-01",
          "tomDato": "2023-12-31",
          "bestemmelse": "FTRL_KAP2_2_8",
          "innvilgelsesResultat": "INNVILGET",
          "trygdedekning": "FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER",
          "medlemskapstype": "PLIKTIG"
        }
      ],
      "skatteforholdsperioder": [
        {
          "fomDato": "2023-01-01",
          "tomDato": "2023-07-31",
          "skatteplikttype": "SKATTEPLIKTIG"
        },
        {
          "fomDato": "2023-08-01",
          "tomDato": "2023-12-31",
          "skatteplikttype": "IKKE_SKATTEPLIKTIG"
        }
      ],
      "inntektskperioder": [
        {
          "fomDato": "2023-01-01",
          "tomDato": "2023-07-31",
          "type": "ARBEIDSINNTEKT_FRA_NORGE",
          "arbeidsgiversavgiftBetales": true,
          "avgiftspliktigInntekt": 40000,
          "erMaanedsbelop": true
        },
        {
          "fomDato": "2023-08-01",
          "tomDato": "2023-12-31",
          "type": "INNTEKT_FRA_UTLANDET",
          "arbeidsgiversavgiftBetales": false,
          "avgiftspliktigInntekt": 15000,
          "erMaanedsbelop": true
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
      "totalInntekt": 355000.0,
      "totalAvgift": 31650.0
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
            MockMvcRequestBuilders.get("$BASE_URL/{behandlingID}/aarsavregninger/{aarsavregningID}", 1, 1).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andExpect(content().json(expectedJson, true))
    }

    companion object {
        private const val BASE_URL: String = "/api/behandlinger"
    }
}
