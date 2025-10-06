package no.nav.melosys.tjenester.gui.aarsavregning

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
import no.nav.melosys.service.avgift.aarsavregning.*
import no.nav.melosys.service.behandling.BehandlingService
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

    @MockkBean
    private lateinit var behandlingService: BehandlingService

    @MockBean
    private lateinit var aksesskontroll: Aksesskontroll

    @Test
    fun `hent avregning basert på ID`() {
        every { årsavregningService.finnÅrsavregningForBehandling(any()) } returns ÅrsavregningModel(
            årsavregningID = 112,
            år = 2023,
            tidligereTrygdeavgiftsGrunnlag = Trygdeavgiftsgrunnlag(
                medlemskapsperioder = listOf(
                    MedlemskapsperiodeForAvgift(
                        LocalDate.parse("2023-01-01"),
                        LocalDate.parse("2023-07-31"),
                        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON,
                        FTRL_KAP2_2_8,
                        Medlemskapstyper.PLIKTIG,
                        InnvilgelsesResultat.INNVILGET
                    ),
                    MedlemskapsperiodeForAvgift(
                        LocalDate.parse("2023-08-01"),
                        LocalDate.parse("2023-12-31"),
                        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER,
                        FTRL_KAP2_2_8,
                        Medlemskapstyper.PLIKTIG,
                        InnvilgelsesResultat.INNVILGET
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
            sisteGjeldendeMedlemskapsperioder = listOf(
                MedlemskapsperiodeForAvgift(
                    LocalDate.parse("2023-01-01"),
                    LocalDate.parse("2023-07-31"),
                    Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON,
                    FTRL_KAP2_2_8,
                    Medlemskapstyper.PLIKTIG,
                    InnvilgelsesResultat.INNVILGET
                ),
                MedlemskapsperiodeForAvgift(
                    LocalDate.parse("2023-08-01"),
                    LocalDate.parse("2023-12-31"),
                    Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER,
                    FTRL_KAP2_2_8,
                    Medlemskapstyper.PLIKTIG,
                    InnvilgelsesResultat.INNVILGET
                )
            ),
            tidligereAvgift = listOf(
                Trygdeavgiftsperiode(
                    periodeFra = LocalDate.parse("2023-01-01"),
                    periodeTil = LocalDate.parse("2023-07-31"),
                    grunnlagInntekstperiode = Inntektsperiode().apply {
                        fomDato = LocalDate.parse("2023-01-01")
                        tomDato = LocalDate.parse("2023-07-31")
                        type = Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE
                        isArbeidsgiversavgiftBetalesTilSkatt = true
                        avgiftspliktigMndInntekt = Penger(40000.0)
                    },
                    trygdesats = BigDecimal(0.0),
                    trygdeavgiftsbeløpMd = Penger(0.0)
                ),
                Trygdeavgiftsperiode(
                    periodeFra = LocalDate.parse("2023-08-01"),
                    periodeTil = LocalDate.parse("2023-12-31"),
                    grunnlagInntekstperiode = Inntektsperiode().apply {
                        fomDato = LocalDate.parse("2023-01-01")
                        tomDato = LocalDate.parse("2023-07-31")
                        type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                        isArbeidsgiversavgiftBetalesTilSkatt = false
                        avgiftspliktigMndInntekt = Penger(15000.0)
                    },
                    trygdesats = BigDecimal(42.2),
                    trygdeavgiftsbeløpMd = Penger(6330.0)
                )
            ),
            nyttTrygdeavgiftsGrunnlag = null,
            endeligAvgift = emptyList(),
            tidligereFakturertBeloep = BigDecimal(21170.0),
            beregnetAvgiftBelop = BigDecimal(24280.0),
            tilFaktureringBeloep = BigDecimal(3110.0),
            harTrygdeavgiftFraAvgiftssystemet = false,
            trygdeavgiftFraAvgiftssystemet = null,
            endeligAvgiftValg = null,
            manueltAvgiftBeloep = null,
            tidligereTrygdeavgiftFraAvgiftssystemet = null,
            tidligereÅrsavregningmanueltAvgiftBeloep = null,
            harSkjoennsfastsattInntektsgrunnlag = false
        )


        val expectedJson = """{
  "aarsavregningID": 112,
  "aar": 2023,
  "tidligereTrygdeavgiftsGrunnlagsopplysninger": {
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
    },
    "tidligereTrygdeavgiftFraAvgiftssystemet": null,
    "tidligereÅrsavregningManueltAvgiftBeloep": null
  },
  "gjeldendeMedlemskapsperioder": [
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
  "nyttGrunnlag": null,
  "endeligAvgift": null,
  "avregning": {
    "beregnetAvgiftBelop": 24280,
    "tidligereFakturertBeloep": 21170,
    "tilFaktureringBeloep": 3110,
    "trygdeavgiftFraAvgiftssystemet": null,
    "manueltAvgiftBeloep": null
  },
  "harTrygdeavgiftFraAvgiftssystemet": false,
  "endeligAvgiftValg": null,
  "harSkjoennsfastsattInntekt": false
}"""

        mockMvc.perform(
            MockMvcRequestBuilders.get("$BASE_URL/{behandlingID}/aarsavregninger", 1).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andExpect(content().json(expectedJson, true))
    }

    @Test
    fun `hent avregning basert på ID med total beløp kalkulerer riktig`() {
        every { årsavregningService.finnÅrsavregningForBehandling(any()) } returns ÅrsavregningModel(
            årsavregningID = 112,
            år = 2023,
            tidligereTrygdeavgiftsGrunnlag = Trygdeavgiftsgrunnlag(
                medlemskapsperioder = listOf(
                    MedlemskapsperiodeForAvgift(
                        LocalDate.parse("2023-01-01"),
                        LocalDate.parse("2023-12-31"),
                        Trygdedekninger.FULL_DEKNING_FTRL,
                        FTRL_KAP2_2_1,
                        Medlemskapstyper.PLIKTIG,
                        InnvilgelsesResultat.INNVILGET
                    )
                ),
                skatteforholdsperioder = listOf(
                    SkatteforholdTilNorgeForAvgift(
                        fom = LocalDate.parse("2023-01-01"),
                        tom = LocalDate.parse("2023-12-31"),
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    )
                ),
                innteksperioder = listOf(
                    InntektsperioderForAvgift(
                        fom = LocalDate.parse("2023-01-01"),
                        tom = LocalDate.parse("2023-12-31"),
                        type = Inntektskildetype.ARBEIDSINNTEKT,
                        avgiftspliktigInntekt = Penger(85000.0),
                        avgiftspliktigTotalInntekt = null,
                        isArbeidsgiversavgiftBetalesTilSkatt = false,
                        erMaanedsbelop = true
                    )
                )
            ),
            sisteGjeldendeMedlemskapsperioder = emptyList(),
            tidligereAvgift = listOf(
                Trygdeavgiftsperiode(
                    periodeFra = LocalDate.parse("2023-01-01"),
                    periodeTil = LocalDate.parse("2023-12-31"),
                    grunnlagInntekstperiode = Inntektsperiode().apply {
                        fomDato = LocalDate.parse("2023-01-01")
                        tomDato = LocalDate.parse("2023-12-31")
                        type = Inntektskildetype.ARBEIDSINNTEKT
                        isArbeidsgiversavgiftBetalesTilSkatt = false
                        avgiftspliktigMndInntekt = Penger(85000.0)
                    },
                    trygdesats = BigDecimal(7.9),
                    trygdeavgiftsbeløpMd = Penger(6715.0)
                )
            ),
            nyttTrygdeavgiftsGrunnlag = Trygdeavgiftsgrunnlag(
                medlemskapsperioder = listOf(
                    MedlemskapsperiodeForAvgift(
                        fom = LocalDate.of(2023, 1, 1),
                        tom = LocalDate.of(2023, 12, 31),
                        dekning = Trygdedekninger.FULL_DEKNING_FTRL,
                        bestemmelse = FTRL_KAP2_2_1,
                        medlemskapstyper = Medlemskapstyper.PLIKTIG,
                        InnvilgelsesResultat.INNVILGET
                    )
                ),
                skatteforholdsperioder = listOf(
                    SkatteforholdTilNorgeForAvgift(
                        fom = LocalDate.of(2023, 1, 1),
                        tom = LocalDate.of(2023, 12, 31),
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    )
                ),
                innteksperioder = listOf(
                    InntektsperioderForAvgift(
                        fom = LocalDate.of(2023, 1, 1),
                        tom = LocalDate.of(2023, 12, 31),
                        type = Inntektskildetype.ARBEIDSINNTEKT,
                        avgiftspliktigInntekt = null,
                        avgiftspliktigTotalInntekt = Penger(85000.0),
                        isArbeidsgiversavgiftBetalesTilSkatt = false,
                        erMaanedsbelop = false
                    )
                )

            ),
            endeligAvgift = listOf(
                Trygdeavgiftsperiode(
                    id = 14,
                    periodeFra = LocalDate.of(2023, 1, 1),
                    periodeTil = LocalDate.of(2023, 12, 31),
                    trygdeavgiftsbeløpMd = Penger(559.0),
                    trygdesats = 7.9.toBigDecimal(),
                    grunnlagInntekstperiode = Inntektsperiode().apply {
                        id = 14
                        fomDato = LocalDate.of(2023, 1, 1)
                        tomDato = LocalDate.of(2023, 12, 31)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                        avgiftspliktigTotalinntekt = Penger(85000.0)
                        isArbeidsgiversavgiftBetalesTilSkatt = false
                    },
                    grunnlagSkatteforholdTilNorge = SkatteforholdTilNorge().apply {
                        id = 14
                        fomDato = LocalDate.of(2023, 1, 1)
                        tomDato = LocalDate.of(2023, 12, 31)
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    })
            ),
            tidligereFakturertBeloep = BigDecimal(80580.0),
            beregnetAvgiftBelop = BigDecimal(6708.0),
            tilFaktureringBeloep = BigDecimal(-73872.0),
            harTrygdeavgiftFraAvgiftssystemet = false,
            trygdeavgiftFraAvgiftssystemet = null,
            endeligAvgiftValg = EndeligAvgiftValg.OPPLYSNINGER_ENDRET,
            manueltAvgiftBeloep = null,
            tidligereTrygdeavgiftFraAvgiftssystemet = null,
            tidligereÅrsavregningmanueltAvgiftBeloep = null,
            harSkjoennsfastsattInntektsgrunnlag = false
        )


        val expectedJson = """{
    "aarsavregningID": 112,
    "aar": 2023,
    "tidligereTrygdeavgiftsGrunnlagsopplysninger": {
        "trygdeavgiftsgrunnlag": {
            "medlemskapsperioder": [
                {
                    "id": 0,
                    "fomDato": "2023-01-01",
                    "tomDato": "2023-12-31",
                    "bestemmelse": "FTRL_KAP2_2_1",
                    "innvilgelsesResultat": "INNVILGET",
                    "trygdedekning": "FULL_DEKNING_FTRL",
                    "medlemskapstype": "PLIKTIG"
                }
            ],
            "skatteforholdsperioder": [
                {
                    "fomDato": "2023-01-01",
                    "tomDato": "2023-12-31",
                    "skatteplikttype": "IKKE_SKATTEPLIKTIG"
                }
            ],
            "inntektskperioder": [
                {
                    "type": "ARBEIDSINNTEKT",
                    "arbeidsgiversavgiftBetales": false,
                    "avgiftspliktigInntekt": 85000,
                    "fomDato": "2023-01-01",
                    "tomDato": "2023-12-31",
                    "erMaanedsbelop": true
                }
            ]
        },
        "avgift": {
            "trygdeavgiftsperioder": [
                {
                    "fom": "2023-01-01",
                    "tom": "2023-12-31",
                    "inntektskildetype": "ARBEIDSINNTEKT",
                    "arbeidsgiversavgiftBetales": false,
                    "inntektPerMd": 85000,
                    "avgiftssats": 7.9,
                    "avgiftPerMd": 6715
                }
            ],
            "totalInntekt": 1020000.00,
            "totalAvgift": 80580.00
        },
        "tidligereTrygdeavgiftFraAvgiftssystemet": null,
        "tidligereÅrsavregningManueltAvgiftBeloep": null
    },
    "gjeldendeMedlemskapsperioder": [],
    "nyttGrunnlag": {
        "trygdeavgiftsgrunnlag": {
            "medlemskapsperioder": [
                {
                    "id": 0,
                    "fomDato": "2023-01-01",
                    "tomDato": "2023-12-31",
                    "bestemmelse": "FTRL_KAP2_2_1",
                    "innvilgelsesResultat": "INNVILGET",
                    "trygdedekning": "FULL_DEKNING_FTRL",
                    "medlemskapstype": "PLIKTIG"
                }
            ],
            "skatteforholdsperioder": [
                {
                    "fomDato": "2023-01-01",
                    "tomDato": "2023-12-31",
                    "skatteplikttype": "IKKE_SKATTEPLIKTIG"
                }
            ],
            "inntektskperioder": [
                {
                    "type": "ARBEIDSINNTEKT",
                    "arbeidsgiversavgiftBetales": false,
                    "avgiftspliktigInntekt": 85000,
                    "fomDato": "2023-01-01",
                    "tomDato": "2023-12-31",
                    "erMaanedsbelop": false
                }
            ]
        },
        "avgift": {
            "trygdeavgiftsperioder": [
                {
                    "fom": "2023-01-01",
                    "tom": "2023-12-31",
                    "inntektskildetype": "ARBEIDSINNTEKT",
                    "arbeidsgiversavgiftBetales": false,
                    "inntektPerMd": 7083,
                    "avgiftssats": 7.9,
                    "avgiftPerMd": 559
                }
            ],
            "totalInntekt": 1020000.00,
            "totalAvgift": 6708.00
        }
    },
    "endeligAvgift": null,
    "avregning": {
        "beregnetAvgiftBelop": 6708,
        "tidligereFakturertBeloep": 80580,
        "tilFaktureringBeloep": -73872,
        "trygdeavgiftFraAvgiftssystemet": null,
        "manueltAvgiftBeloep": null
    },
    "harTrygdeavgiftFraAvgiftssystemet": false,
    "endeligAvgiftValg": "OPPLYSNINGER_ENDRET",
    "harSkjoennsfastsattInntekt": false
}"""

        mockMvc.perform(
            MockMvcRequestBuilders.get("$BASE_URL/{behandlingID}/aarsavregninger", 1).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andExpect(content().json(expectedJson, true))
    }

    companion object {
        private const val BASE_URL: String = "/api/behandlinger"
    }
}
