package no.nav.melosys.integrasjon.ereg.organisasjon

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeTypeOf
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.integrasjon.ereg.organisasjon.OrganisasjonResponse.*
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.codec.DecodingException
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@SpringBootTest
@ActiveProfiles("wiremock-test")
@ContextConfiguration(
    classes = [
        CorrelationIdOutgoingFilter::class,
        OrganisasjonRestConsumerConfig::class,
    ]
)
@AutoConfigureWebClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrganisasjonRestConsumerTest(
    @Autowired private val organisasjonRestConsumer: OrganisasjonRestConsumer,
    @Value("\${mockserver.port}") mockServiceUnderTestPort: Int
) {


    private val processUUID = UUID.randomUUID()
    private val serviceUnderTestMockServer: WireMockServer =
        WireMockServer(WireMockConfiguration.wireMockConfig().port(mockServiceUnderTestPort))

    @BeforeAll
    fun beforeAll() {
        serviceUnderTestMockServer.start()
    }

    @AfterAll
    fun afterAll() {
        serviceUnderTestMockServer.stop()
    }

    @BeforeEach
    fun before() {
        ThreadLocalAccessInfo.beforeExecuteProcess(processUUID, "prossesSteg")
        serviceUnderTestMockServer.resetAll()
    }

    @AfterEach
    fun after() {
        ThreadLocalAccessInfo.afterExecuteProcess(processUUID)
    }

    @Test
    fun `hent organisasjon av type Organisasjon`() {
        val orgnummer = "928497705"
        lagStub(orgnummer)

        val organisasjon = organisasjonRestConsumer.hentOrganisasjon(orgnummer)


        organisasjon.shouldBeTypeOf<Organisasjon>().apply {
            organisasjonDetaljer.shouldNotBeNull().navn
                .single().sammensattnavn.shouldBe("BESK KAFFE")
        }
    }

    @Test
    fun `hent organisasjon av type JuridiskEnhet`() {
        val orgnummer = "928497704"
        lagStub(orgnummer)


        val organisasjon = organisasjonRestConsumer.hentOrganisasjon(orgnummer)


        organisasjon.shouldBeTypeOf<JuridiskEnhet>().apply {
            orgnummer.shouldBe(orgnummer)
            navn.shouldNotBeNull().apply {
                sammensattnavn.shouldBe("BESK KAFFE")
                navnelinje1.shouldBe("BESK KAFFE")
                bruksperiode.fom.shouldBe(LocalDateTime.parse("2021-06-02T09:23:59.799"))
                bruksperiode.tom.shouldBeNull()
                gyldighetsperiode.fom.shouldBe(LocalDate.parse("2021-06-02"))
                gyldighetsperiode.tom.shouldBeNull()
            }
            organisasjonDetaljer.shouldNotBeNull().apply {
                registreringsdato.shouldBe(LocalDateTime.parse("2019-10-15T00:00:00"))
                enhetstyper.shouldNotBeNull().shouldHaveSize(1).first().apply {
                    enhetstype.shouldBe("AS")
                    bruksperiode.fom.shouldNotBeNull()
                    gyldighetsperiode.fom.shouldNotBeNull()
                }
                navn.shouldNotBeNull().shouldHaveSize(1).first().apply {
                    sammensattnavn.shouldBe("BESK KAFFE")
                    navnelinje1.shouldBe("BESK KAFFE")
                    bruksperiode.fom.shouldBe(LocalDateTime.parse("2021-06-02T09:23:59.799"))
                    bruksperiode.tom.shouldBeNull()
                    gyldighetsperiode.fom.shouldBe(LocalDate.of(2021, 6, 2))
                    gyldighetsperiode.tom.shouldBeNull()
                }
                forretningsadresser.shouldNotBeNull().shouldHaveSize(1).first().apply {
                    adresselinje1.shouldBe("STENSLANDSFJELL 94")
                    postnummer.shouldBe("2408")
                    poststed.shouldBe("ELVERUM")
                    landkode.shouldBe("NO")
                    kommunenummer.shouldBe("3420")
                    bruksperiode.fom.shouldNotBeNull()
                    gyldighetsperiode.fom.shouldNotBeNull()
                }
                postadresser.shouldNotBeNull().shouldHaveSize(1).first().apply {
                    adresselinje1.shouldBe("STENSLANDSFJELL 94")
                    postnummer.shouldBe("2408")
                    poststed.shouldBe("ELVERUM")
                    landkode.shouldBe("NO")
                    kommunenummer.shouldBe("3420")
                    bruksperiode.fom.shouldNotBeNull()
                    gyldighetsperiode.fom.shouldNotBeNull()
                }
                navSpesifikkInformasjon.shouldNotBeNull().apply {
                    erIA.shouldBe(false)
                    bruksperiode.fom.shouldNotBeNull()
                    gyldighetsperiode.fom.shouldNotBeNull()
                }
                sistEndret.shouldBe(LocalDate.of(2021, 6, 2))
                juridiskEnhetDetaljer
                    .shouldNotBeNull()
                    .enhetstype.shouldBe("AS")
            }
        }
    }

    @Test
    fun `hent organisasjon av type Virksomhet`() {
        val orgnummer = "901851573"
        lagStub(orgnummer)


        val organisasjon = organisasjonRestConsumer.hentOrganisasjon(orgnummer)


        organisasjon.shouldBeTypeOf<Virksomhet>().apply {
            orgnummer.shouldBe(orgnummer)
            navn.shouldNotBeNull().apply {
                sammensattnavn.shouldBe("ELASTISK FYRSTINNE")
                navnelinje1.shouldBe("ELASTISK FYRSTINNE")
                bruksperiode.fom.shouldBe(LocalDateTime.parse("2021-03-02T14:19:37.229"))
                bruksperiode.tom.shouldBeNull()
                gyldighetsperiode.fom.shouldBe(LocalDate.parse("2021-03-02"))
                gyldighetsperiode.tom.shouldBeNull()
            }
            organisasjonDetaljer.shouldNotBeNull().apply {
                registreringsdato.shouldBe(LocalDateTime.parse("2021-03-02T00:00:00"))
                enhetstyper.shouldNotBeNull().shouldHaveSize(1).first().apply {
                    enhetstype.shouldBe("BEDR")
                    bruksperiode.fom.shouldNotBeNull()
                    gyldighetsperiode.fom.shouldNotBeNull()
                }
                navn.shouldNotBeNull().shouldHaveSize(1).first().apply {
                    sammensattnavn.shouldBe("ELASTISK FYRSTINNE")
                    navnelinje1.shouldBe("ELASTISK FYRSTINNE")
                    bruksperiode.fom.shouldBe(LocalDateTime.parse("2021-03-02T14:19:37.229"))
                    bruksperiode.tom.shouldBeNull()
                    gyldighetsperiode.fom.shouldBe(LocalDate.parse("2021-03-02"))
                    gyldighetsperiode.tom.shouldBeNull()
                }
                forretningsadresser.shouldNotBeNull().shouldHaveSize(1).first().apply {
                    adresselinje1.shouldBe("HEIMLY 9")
                    postnummer.shouldBe("4836")
                    poststed.shouldBeNull()
                    landkode.shouldBe("NO")
                    kommunenummer.shouldBe("4203")
                    bruksperiode.fom.shouldNotBeNull()
                    gyldighetsperiode.fom.shouldNotBeNull()
                }
                postadresser.shouldBeNull()
                navSpesifikkInformasjon.shouldNotBeNull().apply {
                    erIA.shouldBe(false)
                    bruksperiode.fom.shouldNotBeNull()
                    gyldighetsperiode.fom.shouldNotBeNull()
                }
                sistEndret.shouldBe(LocalDate.parse("2021-03-02"))
            }
            virksomhetDetaljer.shouldNotBeNull().apply {
                enhetstype.shouldBe("BEDR")
            }
        }
    }

    @Test
    fun `hent organisasjon av type Organisasjonsledd`() {
        val orgnummer = "974774577"
        lagStub(orgnummer)

        val organisasjon = organisasjonRestConsumer.hentOrganisasjon(orgnummer)


        organisasjon.shouldBeTypeOf<Organisasjonsledd>().apply {
            orgnummer.shouldBe(orgnummer)
            navn.shouldNotBeNull().apply {
                sammensattnavn.shouldBe("ASKØY KOMMUNE AVDELING FOR PLEIE OG OMSORG")
                navnelinje1.shouldBe("ASKØY KOMMUNE AVDELING FOR")
                navnelinje2.shouldBe("PLEIE OG OMSORG")
                bruksperiode.fom.shouldBe(LocalDateTime.parse("2015-02-23T08:04:53.2"))
                bruksperiode.tom.shouldBeNull()
                gyldighetsperiode.fom.shouldBe(LocalDate.parse("2012-03-08"))
                gyldighetsperiode.tom.shouldBeNull()
            }
            organisasjonDetaljer.shouldNotBeNull().apply {
                registreringsdato.shouldBe(LocalDateTime.parse("1996-09-01T00:00:00"))
                stiftelsesdato.shouldBe(LocalDate.parse("1988-04-01"))
                enhetstyper.shouldNotBeNull().shouldHaveSize(1).first().apply {
                    enhetstype.shouldBe("ORGL")
                    bruksperiode.fom.shouldNotBeNull()
                    gyldighetsperiode.fom.shouldNotBeNull()
                }
                navn.shouldNotBeNull().shouldHaveSize(1).first().apply {
                    sammensattnavn.shouldBe("ASKØY KOMMUNE AVDELING FOR PLEIE OG OMSORG")
                    navnelinje1.shouldBe("ASKØY KOMMUNE AVDELING FOR")
                    navnelinje2.shouldBe("PLEIE OG OMSORG")
                    bruksperiode.fom.shouldNotBeNull()
                    gyldighetsperiode.fom.shouldNotBeNull()
                }
                naeringer.shouldNotBeNull().shouldHaveSize(1).first().apply {
                    naeringskode.shouldBe("84.120")
                    hjelpeenhet.shouldBe(false)
                    bruksperiode.fom.shouldNotBeNull()
                    gyldighetsperiode.fom.shouldNotBeNull()
                }
                forretningsadresser.shouldNotBeNull().shouldHaveSize(1).first().apply {
                    adresselinje1.shouldBe("Kleppevegen 23A")
                    postnummer.shouldBe("5300")
                    poststed.shouldBeNull()
                    landkode.shouldBe("NO")
                    kommunenummer.shouldBe("1247")
                    bruksperiode.fom.shouldNotBeNull()
                    gyldighetsperiode.fom.shouldNotBeNull()
                }
                epostadresser.shouldNotBeNull().shouldHaveSize(1).first().apply {
                    adresse.shouldBe("postmottak@askoy.kommune.no")
                    bruksperiode.fom.shouldNotBeNull()
                    gyldighetsperiode.fom.shouldNotBeNull()
                }
                telefonnummer.shouldNotBeNull().shouldHaveSize(1).first().apply {
                    nummer.shouldBe("56158000")
                    telefontype.shouldBe("TFON")
                    bruksperiode.fom.shouldNotBeNull()
                    gyldighetsperiode.fom.shouldNotBeNull()
                }
                navSpesifikkInformasjon.shouldNotBeNull().apply {
                    erIA.shouldBe(true)
                    bruksperiode.fom.shouldNotBeNull()
                    gyldighetsperiode.fom.shouldNotBeNull()
                }
                maalform.shouldBe("NB")
                sistEndret.shouldBe(LocalDate.parse("2016-02-16"))
            }
            organisasjonsleddDetaljer.shouldNotBeNull().apply {
                enhetstype.shouldBe("ORGL")
                sektorkode.shouldBe("6500")
            }
        }
    }

    @Test
    fun `kast IkkeFunnetException når ikke funnet`() {
        val orgnummer = "11111111111"
        serviceUnderTestMockServer.stubFor(
            WireMock.get("/ereg/v2/organisasjon/$orgnummer")
                .withHeader(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.CONTENT_TYPE, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(404)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\n" +
                            "    \"melding\": \"Ingen organisasjon med organisasjonsnummer $orgnummer ble funnet\"\n" +
                            "}")
                )
        )

        shouldThrow<IkkeFunnetException> {
            organisasjonRestConsumer.hentOrganisasjon(orgnummer)
        }
    }

    @Test
    fun `organisasjonDetaljer mangler fører til DecodingException`() {
        val orgnummer = "organisasjonDetaljer-mangler"
        lagStub(orgnummer)

        shouldThrow<DecodingException> {
            organisasjonRestConsumer.hentOrganisasjon(orgnummer)
        }.message.shouldContain("value failed for JSON property organisasjonDetaljer due to missing")
    }


    private fun lagStub(orgnummer: String) {
        val fil = "mock/organisasjon/$orgnummer.json"
        val jsonData = OrganisasjonRestConsumerTest::class.java.classLoader.getResource(fil)
            ?.readText(StandardCharsets.UTF_8) ?: throw IkkeFunnetException("Fant ikke $fil")
        serviceUnderTestMockServer.stubFor(
            WireMock.get("/ereg/v2/organisasjon/$orgnummer")
                .withHeader(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.CONTENT_TYPE, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(jsonData)
                )
        )
    }
}
