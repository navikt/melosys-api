package no.nav.melosys.saksflyt

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.behandling
import no.nav.melosys.saksflytapi.domain.forTest
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

    private fun Any.toJsonNode(): JsonNode = objectMapper.valueToTree(this)

    private fun Any.toJsonString(): String = this.toJsonNode().toPrettyString()

    private fun Any.printJson() {
        println(this.toJsonString())
    }

    companion object {
        private val objectMapper = jacksonObjectMapper()
    }

}

