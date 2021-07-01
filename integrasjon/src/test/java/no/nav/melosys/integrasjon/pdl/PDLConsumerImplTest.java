package no.nav.melosys.integrasjon.pdl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.pdl.dto.Endring;
import no.nav.melosys.integrasjon.pdl.dto.Metadata;
import no.nav.melosys.integrasjon.pdl.dto.identer.Ident;
import no.nav.melosys.integrasjon.pdl.dto.person.*;
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.*;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import static no.nav.melosys.integrasjon.pdl.dto.identer.IdentGruppe.AKTORID;
import static no.nav.melosys.integrasjon.pdl.dto.identer.IdentGruppe.FOLKEREGISTERIDENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class PDLConsumerImplTest {
    private static MockWebServer mockServer;

    private PDLConsumer pdlConsumer;

    @BeforeAll
    static void setupServer() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();
    }

    @BeforeEach
    void setup() {
        pdlConsumer = new PDLConsumerImpl(
            WebClient.builder().baseUrl(String.format("http://localhost:%s", mockServer.getPort()))
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).build());
    }

    @Test
    void hentIdenter_medIdent_mottarOgMapperResponseUtenFeil() throws IkkeFunnetException, IntegrasjonException {
        mockServer.enqueue(
            new MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(lastFil("mock/pdl/hentIdenter.json"))
        );

        assertThat(pdlConsumer.hentIdenter("123").identer()).containsExactly(
            new Ident("99026522600", FOLKEREGISTERIDENT), new Ident("9834873315250", AKTORID));
    }

    @Test
    void hentIdenter_feilFraPDL_kasterFeil() {
        mockServer.enqueue(
            new MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(lastFil("mock/pdl/feil.json"))
        );

        assertThatExceptionOfType(IntegrasjonException.class).isThrownBy(
            () -> pdlConsumer.hentIdenter("123"))
            .withMessageContaining("My error message");
    }

    @Test
    void hentPerson_medIdent_mottarPersonResponseUtenFeil() {
        mockServer.enqueue(
            new MockResponse()
                .setBody(lastFil("mock/pdl/hentPerson.json"))
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        );

        var person = pdlConsumer.hentPerson("123123123");
        assertThat(person.adressebeskyttelse())
            .flatExtracting(Adressebeskyttelse::gradering)
            .isEmpty();
        assertThat(person.doedsfall())
            .flatExtracting(Doedsfall::doedsdato)
            .isEmpty();
        assertThat(person.foedsel())
            .flatExtracting(Foedsel::foedselsdato)
            .containsExactly(LocalDate.of(1979, 11, 18));
        assertThat(person.folkeregisteridentifikator())
            .flatExtracting(Folkeregisteridentifikator::identifikasjonsnummer)
            .containsExactly("58517918383");
        assertThat(person.forelderBarnRelasjon())
            .flatExtracting(ForelderBarnRelasjon::relatertPersonsIdent, ForelderBarnRelasjon::relatertPersonsRolle,
                ForelderBarnRelasjon::minRolleForPerson)
            .containsExactly("22511596061",Familierelasjonsrolle.BARN, Familierelasjonsrolle.FAR);
        assertThat(person.kjoenn())
            .flatExtracting(Kjoenn::kjoenn)
            .containsExactly(KjoennType.MANN);
        assertThat(person.navn())
            .flatExtracting(Navn::fornavn, Navn::mellomnavn, Navn::etternavn)
            .containsExactly("ÅPENHJERTIG", null, "BLYANT");
        assertThat(person.statsborgerskap())
            .flatExtracting(Statsborgerskap::land)
            .containsExactly("ALB", "AIA");
    }

    @Test
    void hentPerson_medIdent_mottarAdresserUtenFeil() {
        mockServer.enqueue(
            new MockResponse()
                .setBody(lastFil("mock/pdl/hentPerson.json"))
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        );

        var person = pdlConsumer.hentPerson("123123123");

        assertThat(person.bostedsadresse()).extracting(Bostedsadresse::vegadresse).contains(
            new Vegadresse("HALÅSVEGEN", "5", null, null, "6713"));

        assertThat(person.kontaktadresse()).hasSize(2).flatExtracting(Kontaktadresse::gyldigFraOgMed,
            Kontaktadresse::gyldigTilOgMed, Kontaktadresse::coAdressenavn).contains(
            LocalDateTime.parse("2020-03-30T00:00"), LocalDateTime.parse("2021-04-01T00:00"), "C/O RAKRYGGET STAFFELI");
        assertThat(person.kontaktadresse()).extracting(Kontaktadresse::postadresseIFrittFormat).contains(
            new PostadresseIFrittFormat("POSTLINJE 1", "OG 2", null, "4994"));
        assertThat(person.kontaktadresse()).extracting(Kontaktadresse::utenlandskAdresseIFrittFormat).contains(
            new UtenlandskAdresseIFrittFormat("1KOLEJOWA 6/5", "18-500 KOLNO", "CAPITAL WEST 3000", null, null,
                "ARG"));

        assertThat(person.oppholdsadresse()).extracting(Oppholdsadresse::coAdressenavn).contains("Estate of");
        assertThat(person.oppholdsadresse()).extracting(Oppholdsadresse::utenlandskAdresse).contains(
            new UtenlandskAdresse("Adresse er påkrevd", "Bygning", null, "Postkode", "By", "Region", "ABW"));
    }

    @Test
    void hentStatsborgerskap_medIdent_mottarResponseUtenFeil() {
        mockServer.enqueue(
            new MockResponse().setBody(lastFil("mock/pdl/hentStatsborgerskap.json")).addHeader(HttpHeaders.CONTENT_TYPE,
                MediaType.APPLICATION_JSON_VALUE));

        assertThat(pdlConsumer.hentStatsborgerskap("123")).containsExactlyInAnyOrder(
            new Statsborgerskap("ALB", null, LocalDate.parse("1961-02-01"), LocalDate.parse("1981-09-07"),
                new Metadata("FREG", true,
                    List.of(new Endring("OPPRETT", LocalDateTime.parse("2021-05-07T10:04:52"), "Dolly")))),
            new Statsborgerskap("AIA", LocalDate.parse("2021-05-08"), LocalDate.parse("1979-11-18"), null,
                new Metadata("PDL", false,
                    List.of(new Endring("OPPRETT", LocalDateTime.parse("2021-05-07T10:04:52"), "PDL")))));
    }

    @Test
    void hentPersonMedHistorikk_mottarPersonResponseUtenFeil() {
        mockServer.enqueue(
            new MockResponse()
                .setBody(lastFil("mock/pdl/hentPersonHistorikk.json"))
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        );

        var person = pdlConsumer.hentPerson("23487505536");
        assertThat(person.doedsfall())
            .flatExtracting(Doedsfall::doedsdato)
            .containsExactly(LocalDate.parse("2021-07-06"));
        assertThat(person.foedsel())
            .flatExtracting(Foedsel::foedselsdato)
            .contains(LocalDate.of(1975, 8, 23));
        assertThat(person.folkeregisteridentifikator())
            .flatExtracting(Folkeregisteridentifikator::identifikasjonsnummer)
            .containsExactly("23487505536");
        assertThat(person.folkeregisterpersonstatus())
            .flatExtracting(Folkeregisterpersonstatus::status)
            .containsExactly("doed");
       assertThat(person.forelderBarnRelasjon())
            .flatExtracting(ForelderBarnRelasjon::relatertPersonsIdent, ForelderBarnRelasjon::relatertPersonsRolle,
                ForelderBarnRelasjon::minRolleForPerson)
            .containsExactly("01421474318",Familierelasjonsrolle.BARN, Familierelasjonsrolle.FAR);
        assertThat(person.kjoenn())
            .flatExtracting(Kjoenn::kjoenn)
            .containsExactly(KjoennType.MANN);
        assertThat(person.navn())
            .flatExtracting(Navn::fornavn, Navn::mellomnavn, Navn::etternavn)
            .containsExactly("ABSURD", null, "HEST");
        assertThat(person.statsborgerskap())
            .flatExtracting(Statsborgerskap::land)
            .containsExactly("EST");
        assertThat(person.sivilstand())
            .flatExtracting(Sivilstand::type, Sivilstand::relatertVedSivilstand, Sivilstand::gyldigFraOgMed)
            .containsExactly(Sivilstandstype.UOPPGITT, null, null,
                Sivilstandstype.GIFT, "04507445824", LocalDate.parse("2021-07-06"));
    }

    @Test
    void hentPersonMedHistorikk_mottarAdresserUtenFeil() {
        mockServer.enqueue(
            new MockResponse()
                .setBody(lastFil("mock/pdl/hentPersonHistorikk.json"))
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        );

        var person = pdlConsumer.hentPerson("23487505536");

        assertThat(person.bostedsadresse()).extracting(Bostedsadresse::vegadresse).contains(
            new Vegadresse("Akkarfjordneset", "153", null, null, "9190"));

        assertThat(person.kontaktadresse()).hasSize(2).flatExtracting(Kontaktadresse::gyldigFraOgMed,
            Kontaktadresse::gyldigTilOgMed).contains(
            LocalDateTime.parse("2020-07-06T00:00"), LocalDateTime.parse("2031-07-06T23:59:59"));
        assertThat(person.kontaktadresse()).extracting(Kontaktadresse::postadresseIFrittFormat).contains(
            new PostadresseIFrittFormat("POSTLINJE 1", "POSTLINJE 2", null, "9650"));
        assertThat(person.kontaktadresse()).extracting(Kontaktadresse::utenlandskAdresseIFrittFormat).contains(
            new UtenlandskAdresseIFrittFormat("POSTLINJE 1", "POSTLINJE 2", "POSTLINJE 3", null, null,
                "BMU"));

        assertThat(person.oppholdsadresse()).extracting(Oppholdsadresse::utenlandskAdresse).contains(
            new UtenlandskAdresse("1KOLEJOWA 6/5, 18-500 KOLNO, CAPITAL WEST 3000", "", null, null, null, "", "ARG"));
    }

    private String lastFil(String filnavn) {
        try {
            return Files.readString(Paths.get(
                Objects.requireNonNull(getClass().getClassLoader().getResource(filnavn)).toURI()
            ));
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
