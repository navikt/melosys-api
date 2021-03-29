package no.nav.melosys.integrasjon.pdl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
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
    public void setup() {
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
    void hentPerson_medIdent_mottarPersonResponseUtenFeil() throws IkkeFunnetException, IntegrasjonException {
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
            .containsExactly(LocalDate.of(1991, 2, 27));
        assertThat(person.folkeregisteridentifikator())
            .flatExtracting(Folkeregisteridentifikator::identifikasjonsnummer, Folkeregisteridentifikator::type,
                Folkeregisteridentifikator::status)
            .containsExactly("27429104489", "FNR", "I_BRUK");
        assertThat(person.folkeregisterpersonstatus())
            .flatExtracting(Folkeregisterpersonstatus::forenkletStatus)
            .containsExactly("bosattEtterFolkeregisterloven");
        assertThat(person.forelderBarnRelasjon())
            .flatExtracting(ForelderBarnRelasjon::relatertPersonsIdent, ForelderBarnRelasjon::relatertPersonsRolle,
                ForelderBarnRelasjon::minRolleForPerson)
            .containsExactly("22511596061",Familierelasjonsrolle.BARN, Familierelasjonsrolle.MOR);
        assertThat(person.kjoenn())
            .flatExtracting(Kjoenn::kjoenn)
            .containsExactly(KjoennType.KVINNE);
        assertThat(person.navn())
            .flatExtracting(Navn::fornavn, Navn::mellomnavn, Navn::etternavn)
            .containsExactly("MOLEFONKEN", "TIKKENDE", "KNOTT");
        assertThat(person.statsborgerskap())
            .flatExtracting(Statsborgerskap::land)
            .containsExactly("NOR");
        assertThat(person.sivilstand())
            .flatExtracting(Sivilstand::type, Sivilstand::relatertVedSivilstand, Sivilstand::gyldigFraOgMed)
            .containsExactly(Sivilstandstype.REGISTRERT_PARTNER, "11466927750", LocalDate.parse("2021-03-02"));
        assertThat(person.kontaktadresse()).hasSize(2).flatExtracting(Kontaktadresse::type,
            Kontaktadresse::gyldigFraOgMed, Kontaktadresse::gyldigTilOgMed, Kontaktadresse::coAdressenavn)
            .contains(KontaktadresseType.Innland, LocalDateTime.parse("2020-03-29T00:00"),
                LocalDateTime.parse("2021-04-01T00:00"), "C/O RAKRYGGET STAFFELI");
        assertThat(person.kontaktadresse()).extracting(Kontaktadresse::vegadresse).contains(
            new Vegadresse("LANGBERGA", "30", null, null, "6800"));
        assertThat(person.kontaktadresse()).extracting(Kontaktadresse::postadresseIFrittFormat).contains(
            new PostadresseIFrittFormat("POSTLINJE 1", "OG 2", null, "4994"));
        assertThat(person.bostedsadresse()).extracting(Bostedsadresse::angittFlyttedato)
            .contains(LocalDate.parse("2020-03-29"));
        assertThat(person.bostedsadresse()).extracting(Bostedsadresse::vegadresse).contains(
            new Vegadresse("HALÅSVEGEN", "5", null, null, "6713"));
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
