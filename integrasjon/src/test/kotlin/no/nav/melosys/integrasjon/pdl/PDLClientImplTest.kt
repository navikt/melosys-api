package no.nav.melosys.integrasjon.pdl

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.any
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.melosys.exception.IntegrasjonException
import no.nav.melosys.integrasjon.MetricsTestConfig
import no.nav.melosys.integrasjon.OAuthMockServer
import no.nav.melosys.integrasjon.felles.graphql.GraphQLRequest
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import no.nav.melosys.integrasjon.pdl.dto.Endring
import no.nav.melosys.integrasjon.pdl.dto.Endringstype
import no.nav.melosys.integrasjon.pdl.dto.Metadata
import no.nav.melosys.integrasjon.pdl.dto.identer.Ident
import no.nav.melosys.integrasjon.pdl.dto.identer.IdentGruppe.AKTORID
import no.nav.melosys.integrasjon.pdl.dto.identer.IdentGruppe.FOLKEREGISTERIDENT
import no.nav.melosys.integrasjon.pdl.dto.identer.Query.HENT_IDENTER_QUERY
import no.nav.melosys.integrasjon.pdl.dto.person.Familierelasjonsrolle
import no.nav.melosys.integrasjon.pdl.dto.person.KjoennType
import no.nav.melosys.integrasjon.pdl.dto.person.Sivilstandstype
import no.nav.melosys.integrasjon.pdl.dto.person.Statsborgerskap
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.PostadresseIFrittFormat
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.UtenlandskAdresse
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.UtenlandskAdresseIFrittFormat
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.Vegadresse
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.io.IOException
import java.net.URISyntaxException
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Objects
import java.util.UUID

@SpringBootTest
@ActiveProfiles("wiremock-test")
@ContextConfiguration(
    classes = [
        OAuthMockServer::class,
        CorrelationIdOutgoingFilter::class,
        PDLClientProducer::class,
        PDLAuthFilterAzure::class,
        MetricsTestConfig::class,
    ]
)
@AutoConfigureWebClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PDLClientImplTest(
    @Autowired private val pdlClient: PDLClient,
    @Autowired private val oAuthMockServer: OAuthMockServer,
    @Value("\${mockserver.port}") mockServerPort: Int,
) {
    private val processUUID = UUID.randomUUID()
    private val mockServer = WireMockServer(WireMockConfiguration.wireMockConfig().port(mockServerPort))
    private val objectMapper = ObjectMapper()

    @BeforeAll
    fun beforeAll() {
        ThreadLocalAccessInfo.beforeExecuteProcess(processUUID, "prosessSteg")
        mockServer.start()
        oAuthMockServer.start()
    }

    @AfterAll
    fun afterAll() {
        mockServer.stop()
        oAuthMockServer.stop()
        ThreadLocalAccessInfo.afterExecuteProcess(processUUID)
    }

    @BeforeEach
    fun beforeEach() {
        mockServer.resetAll()
        oAuthMockServer.reset()
    }

    @Test
    fun `hentIdenter serialiserer GraphQL request body korrekt`() {
        mockServer.stubFor(
            any(anyUrl()).willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(lastFil("mock/pdl/hentIdenter.json"))
            )
        )

        pdlClient.hentIdenter("12345678901")

        val expectedRequest = GraphQLRequest(HENT_IDENTER_QUERY, mapOf("ident" to "12345678901"))
        val expectedJson = objectMapper.writeValueAsString(expectedRequest)

        mockServer.verify(
            postRequestedFor(urlEqualTo("/graphql"))
                .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.ACCEPT, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.AUTHORIZATION, matching("Bearer .+"))
                .withRequestBody(equalToJson(expectedJson, true, false))
        )
    }

    @Test
    fun `hentIdenter med ident mottar og mapper response uten feil`() {
        mockServer.stubFor(
            any(anyUrl()).willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(lastFil("mock/pdl/hentIdenter.json"))
            )
        )

        pdlClient.hentIdenter("123").identer() shouldContainExactly listOf(
            Ident("99026522600", FOLKEREGISTERIDENT),
            Ident("9834873315250", AKTORID)
        )
    }

    @Test
    fun `hentIdenter feil fra PDL kaster feil`() {
        mockServer.stubFor(
            any(anyUrl()).willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(lastFil("mock/pdl/feil.json"))
            )
        )

        val exception = shouldThrow<IntegrasjonException> {
            pdlClient.hentIdenter("123")
        }
        exception.message shouldContain "My error message"
    }

    @Test
    fun `hent familierelasjoner`() {
        mockServer.stubFor(
            any(anyUrl()).willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(lastFil("mock/pdl/hentFamilierelasjoner.json"))
            )
        )

        val person = pdlClient.hentFamilierelasjoner("ident")

        person.run {
            folkeregisteridentifikator()
                .shouldHaveSize(1)
                .single().identifikasjonsnummer shouldBe "5340907334"

            forelderBarnRelasjon().run {
                shouldHaveSize(1)
                single().run {
                    relatertPersonsIdent shouldBe "01421474318"
                    relatertPersonsRolle shouldBe Familierelasjonsrolle.BARN
                    minRolleForPerson shouldBe Familierelasjonsrolle.MOR
                }
            }

            sivilstand().run {
                shouldHaveSize(2)
                elementAt(0).run {
                    type shouldBe Sivilstandstype.UGIFT
                    relatertVedSivilstand shouldBe null
                    metadata.historisk shouldBe false
                }
                elementAt(1).run {
                    type shouldBe Sivilstandstype.GIFT
                    relatertVedSivilstand shouldBe "04507445824"
                    metadata.historisk shouldBe true
                }
            }
        }
    }

    @Test
    fun `hentPerson med ident mottar person response uten feil`() {
        mockServer.stubFor(
            any(anyUrl()).willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(lastFil("mock/pdl/hentPerson.json"))
            )
        )

        val person = pdlClient.hentPerson("123123123")

        person.run {
            adressebeskyttelse().shouldBeEmpty()
            doedsfall().shouldBeEmpty()
            foedselsdato()
                .shouldHaveSize(1)
                .single().foedselsdato shouldBe LocalDate.of(1979, 11, 18)
            folkeregisteridentifikator()
                .shouldHaveSize(1)
                .single().identifikasjonsnummer shouldBe "58517918383"

            forelderBarnRelasjon().run {
                shouldHaveSize(1)
                single().run {
                    relatertPersonsIdent shouldBe "22511596061"
                    relatertPersonsRolle shouldBe Familierelasjonsrolle.BARN
                    minRolleForPerson shouldBe Familierelasjonsrolle.FAR
                }
            }

            kjoenn()
                .shouldHaveSize(1)
                .single().kjoenn shouldBe KjoennType.MANN

            navn().run {
                shouldHaveSize(1)
                single().run {
                    fornavn shouldBe "ÅPENHJERTIG"
                    mellomnavn shouldBe null
                    etternavn shouldBe "BLYANT"
                }
            }

            sivilstand().run {
                shouldHaveSize(1)
                single().run {
                    type shouldBe Sivilstandstype.REGISTRERT_PARTNER
                    relatertVedSivilstand shouldBe "11466927750"
                    gyldigFraOgMed shouldBe LocalDate.parse("2021-03-02")
                    bekreftelsesdato shouldBe null
                }
            }

            statsborgerskap().map { it.land } shouldContainExactly listOf("ALB", "AIA")
        }
    }

    @Test
    fun `hentPerson med ident mottar adresser uten feil`() {
        mockServer.stubFor(
            any(anyUrl()).willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(lastFil("mock/pdl/hentPerson.json"))
            )
        )

        val person = pdlClient.hentPerson("123123123")

        person.run {
            bostedsadresse()
                .shouldHaveSize(1)
                .single().vegadresse shouldBe Vegadresse("HALÅSVEGEN", "5", null, null, "6713")

            kontaktadresse().run {
                shouldHaveSize(2)
                elementAt(0).run {
                    gyldigFraOgMed shouldBe LocalDateTime.parse("2020-03-30T00:00")
                    gyldigTilOgMed shouldBe LocalDateTime.parse("2021-04-01T00:00")
                    coAdressenavn shouldBe "C/O RAKRYGGET STAFFELI"
                    postadresseIFrittFormat shouldBe PostadresseIFrittFormat("POSTLINJE 1", "OG 2", null, "4994")
                    utenlandskAdresseIFrittFormat shouldBe null
                }
                elementAt(1).run {
                    gyldigFraOgMed shouldBe LocalDateTime.parse("2021-05-07T10:04:52")
                    gyldigTilOgMed shouldBe null
                    coAdressenavn shouldBe null
                    postadresseIFrittFormat shouldBe null
                    utenlandskAdresseIFrittFormat shouldBe UtenlandskAdresseIFrittFormat(
                        "1KOLEJOWA 6/5",
                        "18-500 KOLNO",
                        "CAPITAL WEST 3000",
                        null,
                        null,
                        "ARG"
                    )
                }
            }

            oppholdsadresse().run {
                shouldHaveSize(1)
                single().run {
                    coAdressenavn shouldBe "Estate of"
                    utenlandskAdresse shouldBe UtenlandskAdresse("Adresse er påkrevd", "Bygning", null, "Postkode", "By", "Region", "ABW")
                }
            }
        }
    }

    @Test
    fun `hentStatsborgerskap med ident mottar response uten feil`() {
        mockServer.stubFor(
            any(anyUrl()).willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(lastFil("mock/pdl/hentStatsborgerskap.json"))
            )
        )

        pdlClient.hentStatsborgerskap("123") shouldContainExactlyInAnyOrder listOf(
            Statsborgerskap(
                "ALB", null, LocalDate.parse("1961-02-01"), LocalDate.parse("1981-09-07"),
                Metadata(
                    "FREG", true,
                    listOf(Endring(Endringstype.OPPRETT, LocalDateTime.parse("2021-05-07T10:04:52"), "Dolly"))
                )
            ),
            Statsborgerskap(
                "AIA", LocalDate.parse("2021-05-08"), LocalDate.parse("1979-11-18"), null,
                Metadata(
                    "PDL", false,
                    listOf(Endring(Endringstype.OPPRETT, LocalDateTime.parse("2021-05-07T10:04:52"), "PDL"))
                )
            )
        )
    }

    @Test
    fun `hentPersonMedHistorikk mottar person response uten feil`() {
        mockServer.stubFor(
            any(anyUrl()).willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(lastFil("mock/pdl/hentPersonHistorikk.json"))
            )
        )

        val person = pdlClient.hentPerson("23487505536")

        person.run {
            doedsfall()
                .shouldHaveSize(1)
                .single().doedsdato shouldBe LocalDate.parse("2021-07-06")
            foedselsdato()
                .shouldHaveSize(1)
                .single().foedselsdato shouldBe LocalDate.of(1975, 8, 23)
            folkeregisteridentifikator()
                .shouldHaveSize(1)
                .single().identifikasjonsnummer shouldBe "23487505536"
            folkeregisterpersonstatus()
                .shouldHaveSize(1)
                .single().status shouldBe "doed"
            folkeregisterpersonstatus()
                .shouldHaveSize(1)
                .single().metadata.endringer
                .shouldHaveSize(1)
                .single().kilde shouldBe "Dolly"

            forelderBarnRelasjon().run {
                shouldHaveSize(1)
                single().run {
                    relatertPersonsIdent shouldBe "01421474318"
                    relatertPersonsRolle shouldBe Familierelasjonsrolle.BARN
                    minRolleForPerson shouldBe Familierelasjonsrolle.FAR
                }
            }

            kjoenn()
                .shouldHaveSize(1)
                .single().kjoenn shouldBe KjoennType.MANN

            navn().run {
                shouldHaveSize(1)
                single().run {
                    fornavn shouldBe "ABSURD"
                    mellomnavn shouldBe null
                    etternavn shouldBe "HEST"
                }
            }

            statsborgerskap()
                .shouldHaveSize(1)
                .single().land shouldBe "EST"

            sivilstand().run {
                shouldHaveSize(2)
                elementAt(0).run {
                    type shouldBe Sivilstandstype.UOPPGITT
                    relatertVedSivilstand shouldBe null
                    gyldigFraOgMed shouldBe null
                    bekreftelsesdato shouldBe LocalDate.parse("2019-05-07")
                }
                elementAt(1).run {
                    type shouldBe Sivilstandstype.GIFT
                    relatertVedSivilstand shouldBe "04507445824"
                    gyldigFraOgMed shouldBe LocalDate.parse("2021-07-06")
                    bekreftelsesdato shouldBe null
                }
            }
        }
    }

    @Test
    fun `hentPersonMedHistorikk mottar adresser uten feil`() {
        mockServer.stubFor(
            any(anyUrl()).willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(lastFil("mock/pdl/hentPersonHistorikk.json"))
            )
        )

        val person = pdlClient.hentPerson("23487505536")

        person.run {
            bostedsadresse()
                .shouldHaveSize(1)
                .single().vegadresse shouldBe Vegadresse("Akkarfjordneset", "153", null, null, "9190")

            kontaktadresse().run {
                shouldHaveSize(2)
                elementAt(0).run {
                    gyldigFraOgMed shouldBe LocalDateTime.parse("2020-07-06T00:00")
                    gyldigTilOgMed shouldBe LocalDateTime.parse("2031-07-06T23:59:59")
                    postadresseIFrittFormat shouldBe PostadresseIFrittFormat("POSTLINJE 1", "POSTLINJE 2", null, "9650")
                    utenlandskAdresseIFrittFormat shouldBe null
                }
                elementAt(1).run {
                    gyldigFraOgMed shouldBe LocalDateTime.parse("2021-07-06T00:00")
                    gyldigTilOgMed shouldBe LocalDateTime.parse("2022-07-06T00:00")
                    postadresseIFrittFormat shouldBe null
                    utenlandskAdresseIFrittFormat shouldBe UtenlandskAdresseIFrittFormat(
                        "POSTLINJE 1",
                        "POSTLINJE 2",
                        "POSTLINJE 3",
                        null,
                        null,
                        "BMU"
                    )
                }
            }

            oppholdsadresse()
                .shouldHaveSize(1)
                .single().utenlandskAdresse shouldBe UtenlandskAdresse(
                "1KOLEJOWA 6/5, 18-500 KOLNO, CAPITAL WEST 3000",
                "",
                null,
                null,
                null,
                "",
                "ARG"
            )
        }
    }

    private fun lastFil(filnavn: String): String {
        return try {
            Files.readString(
                Paths.get(
                    Objects.requireNonNull(javaClass.classLoader.getResource(filnavn)).toURI()
                )
            )
        } catch (e: IOException) {
            throw IllegalStateException(e)
        } catch (e: URISyntaxException) {
            throw IllegalStateException(e)
        }
    }
}

