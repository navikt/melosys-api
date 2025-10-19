package no.nav.melosys.saksflyt

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.avgift.TrygdeavgiftsperiodeTestFactory
import no.nav.melosys.domain.avgift.forTest
import no.nav.melosys.domain.behandling
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.medlemskapsperiode
import no.nav.melosys.domain.trygdeavgiftsperiode
import java.time.LocalDate
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.behandling
import no.nav.melosys.saksflytapi.domain.forTest
import java.math.BigDecimal
import java.util.*
import kotlin.test.Test

class DSLTest {

    @Test
    fun `Fagsak med behandlinger`() {
        val fagsak = Fagsak.forTest {
            tema = Sakstemaer.TRYGDEAVGIFT
            behandling {
                id = 1L
            }
            behandling {
                id = 2L
            }
        }

        fagsak.run {
            behandlinger[0].fagsak shouldBe fagsak
            behandlinger[1].fagsak shouldBe fagsak
            toMap().toJsonString() shouldEqualJson """
            {
              "saksnummer" : "MEL-test",
              "type" : "EU_EOS",
              "tema" : "TRYGDEAVGIFT",
              "status" : "OPPRETTET",
              "aktører" : [ ],
              "behandlinger" : [ {
                "id" : 1,
                "status" : "UNDER_BEHANDLING",
                "type" : "FØRSTEGANG",
                "tema" : "REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING"
              }, {
                "id" : 2,
                "status" : "UNDER_BEHANDLING",
                "type" : "FØRSTEGANG",
                "tema" : "REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING"
              } ]
            }"""
        }
    }

    @Test
    fun `Behandling med fagsak`() {
        val behandling = Behandling.forTest {
            fagsak {
                medBruker()
                medVirksomhet()
            }
        }

        behandling.run {
            fagsak shouldBe behandling.fagsak
            toMap().toJsonString() shouldEqualJson """
            {
              "id" : 0,
              "status" : "UNDER_BEHANDLING",
              "type" : "FØRSTEGANG",
              "tema" : "REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING",
              "fagsak" : {
                "saksnummer" : "MEL-test",
                "type" : "EU_EOS",
                "tema" : "MEDLEMSKAP_LOVVALG",
                "status" : "OPPRETTET",
                "aktører" : [ {
                  "aktørId" : "12345678901",
                  "rolle" : "BRUKER"
                }, {
                  "rolle" : "VIRKSOMHET",
                  "orgnr" : "123456789"
                } ]
              }
            }
            """
        }
    }

    @Test
    fun `Prosessinstans med behandling og data`() {
        val prosessinstans = Prosessinstans.forTest {
            id = UUID.fromString("da6a548b-59a8-4f19-9788-434254728307")
            behandling {
                fagsak {
                    medBruker()
                }
            }
            medData(ProsessDataKey.SAKSBEHANDLER, "Z123456")
        }

        prosessinstans.run {
            toMap().toJsonString() shouldEqualJson """
            {
              "id" : "da6a548b-59a8-4f19-9788-434254728307",
              "status" : "KLAR",
              "behandling" : {
                "id" : 0,
                "status" : "UNDER_BEHANDLING",
                "type" : "FØRSTEGANG",
                "tema" : "REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING",
                "fagsak" : {
                  "saksnummer" : "MEL-test",
                  "type" : "EU_EOS",
                  "tema" : "MEDLEMSKAP_LOVVALG",
                  "status" : "OPPRETTET",
                  "aktører" : [ {
                    "aktørId" : "12345678901",
                    "rolle" : "BRUKER"
                  } ]
                }
              },
              "data" : [ {
                "key" : "saksbehandler",
                "value" : "Z123456"
              } ]
            }"""
        }
    }

    @Test
    fun `Behandlingsresultat med trygdeavgiftsperiode`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            behandling {
                fagsak {
                    type = Sakstyper.FTRL
                }
            }
            medlemskapsperiode {
                fom = LocalDate.of(2023, 1, 1)
                tom = LocalDate.of(2023, 12, 31)
                trygdeavgiftsperiode {
                    grunnlagInntekstperiode {
                        type = Inntektskildetype.ARBEIDSINNTEKT
                        isArbeidsgiversavgiftBetalesTilSkatt = false
                        avgiftspliktigMndInntekt = Penger(15000.0)
                    }
                    grunnlagSkatteforholdTilNorge {
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                    trygdesats = 6.8.toBigDecimal()
                }
            }
        }

        behandlingsresultat.run {
            medlemskapsperioder.shouldHaveSize(1).single().run {
                fom shouldBe LocalDate.of(2023, 1, 1)
                tom shouldBe LocalDate.of(2023, 12, 31)
                trygdeavgiftsperioder.shouldHaveSize(1).single().run {
                    periodeFra shouldBe LocalDate.of(2023, 1, 1)
                    periodeTil shouldBe LocalDate.of(2023, 12, 31)
                    trygdesats shouldBe 6.8.toBigDecimal()
                    grunnlagInntekstperiode.shouldNotBeNull().run {
                        type shouldBe Inntektskildetype.ARBEIDSINNTEKT
                        isArbeidsgiversavgiftBetalesTilSkatt shouldBe false
                        avgiftspliktigMndInntekt.verdi shouldBe 15000.0.toBigDecimal()
                    }
                    grunnlagSkatteforholdTilNorge.shouldNotBeNull().run {
                        skatteplikttype shouldBe Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                }
            }
        }
    }

    @Test
    fun `trygdeavgiftsperiode med defaults`() {
        val trygdeavgiftsperiode = Trygdeavgiftsperiode.forTest {
            grunnlagInntekstperiode {
            }
            grunnlagSkatteforholdTilNorge {
            }
        }

        trygdeavgiftsperiode.run {
            periodeFra shouldBe TrygdeavgiftsperiodeTestFactory.PERIODE_FRA
            periodeTil shouldBe TrygdeavgiftsperiodeTestFactory.PERIODE_TIL
            trygdesats shouldBe TrygdeavgiftsperiodeTestFactory.TRYGDESATS
            grunnlagInntekstperiode.shouldNotBeNull().run {
                fom shouldBe TrygdeavgiftsperiodeTestFactory.PERIODE_FRA
                tom shouldBe TrygdeavgiftsperiodeTestFactory.PERIODE_TIL
                type shouldBe TrygdeavgiftsperiodeTestFactory.INNTEKTSKILDETYPE
                isArbeidsgiversavgiftBetalesTilSkatt shouldBe TrygdeavgiftsperiodeTestFactory.ARBEIDSGIVERSAVGIFT_BETALES_TIL_SKATT
                avgiftspliktigMndInntekt.verdi shouldBe 15000.0.toBigDecimal()
            }
            grunnlagSkatteforholdTilNorge.shouldNotBeNull().run {
                fom shouldBe TrygdeavgiftsperiodeTestFactory.PERIODE_FRA
                tom shouldBe TrygdeavgiftsperiodeTestFactory.PERIODE_TIL
                skatteplikttype shouldBe Skatteplikttype.IKKE_SKATTEPLIKTIG
            }
        }
    }

    private fun Behandling.toMap(inkluderFagsak: Boolean = true): Map<String, Any?> = mapOf(
        "id" to id,
        "status" to status.name,
        "type" to type.name,
        "tema" to tema.name,
        "fagsak" to if (inkluderFagsak) fagsak.toMap(inkluderBehandlinger = false) else null
    ).filterValues { it != null }

    private fun Fagsak.toMap(inkluderBehandlinger: Boolean = true) = mapOf(
        "saksnummer" to saksnummer,
        "gsakSaksnummer" to gsakSaksnummer,
        "type" to type.name,
        "tema" to tema.name,
        "status" to status.name,
        "betalingsvalg" to betalingsvalg?.name,
        "aktører" to this.aktører.map { aktør ->
            mapOf(
                "aktørId" to aktør.aktørId,
                "rolle" to aktør.rolle?.name,
                "orgnr" to aktør.orgnr
            ).filterValues { it != null }
        },
        "behandlinger" to if (inkluderBehandlinger) this.behandlinger.map { behandling -> behandling.toMap(false) } else null
    ).filterValues { it != null }

    private fun Prosessinstans.toMap() = mapOf(
        "id" to id,
        "status" to status.name,
        "behandling" to hentBehandling.toMap() + mapOf(
            "fagsak" to hentBehandling.fagsak.toMap(inkluderBehandlinger = false)
        ),
        "data" to this.getData().map { (key, value) ->
            mapOf(
                "key" to key,
                "value" to value
            )
        }
    )

    private fun Any.toJsonString() = objectMapper
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .registerModule(JavaTimeModule())
        .writerWithDefaultPrettyPrinter()
        .writeValueAsString(this)

    private fun Any.printJson() {
        println(this.toJsonString())
    }

    companion object {
        private val objectMapper = jacksonObjectMapper()
    }

}

