package no.nav.melosys.domain.dokument.organisasjon;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Objects;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.KonverteringTest;
import no.nav.melosys.domain.dokument.XsltTemplatesFactory;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import no.nav.melosys.domain.dokument.organisasjon.adresse.Gateadresse;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import static org.assertj.core.api.Assertions.assertThat;

// Denne konverteringen testes også i DokumentFactoryTest, uten strukturert adresse
class Ereg4KonverteringTest implements KonverteringTest {
    private static final String EREG_4_0_MOCK = "organisasjon/org_med_strukturert_adresse.xml";
    private static DokumentFactory factory;

    @BeforeAll
    public static void setUp() {
        Jaxb2Marshaller marshaller = JaxbConfig.getJaxb2Marshaller();
        XsltTemplatesFactory xsltTemplatesFactory = new XsltTemplatesFactory();
        factory = new DokumentFactory(marshaller, xsltTemplatesFactory);
    }

    @Test
    void testAdresse() throws Exception {
        Saksopplysning test = getSaksopplysning(EREG_4_0_MOCK);

        // Test semistrukturert adresse...
        OrganisasjonDokument dokument = (OrganisasjonDokument) test.getDokument();
        SemistrukturertAdresse postadresse = (SemistrukturertAdresse) dokument.getOrganisasjonDetaljer().getPostadresse().get(0);
        assertThat(postadresse.getAdresselinje1()).isEqualTo("Skuteviksbodene 1");
        assertThat(postadresse.getPostnr()).isEqualTo("5035");
        assertThat(postadresse.getKommunenr()).isEqualTo("1201");

        // Test strukturert adresse...
        Gateadresse forretningsadresse = (Gateadresse) dokument.getOrganisasjonDetaljer().getForretningsadresser().get(0);
        assertThat(forretningsadresse.getGatenavn()).isEqualTo("Gatenavn");
        assertThat(forretningsadresse.getLandkode()).isEqualTo("NO");

        // Test perioder:
        // Forretningsadresse har ingen perioder satt...
        assertThat(forretningsadresse.getBruksperiode()).isNull();
        assertThat(forretningsadresse.getGyldighetsperiode()).isNull();
        // Postadresse har fom-dato på begge periodene, men ikke tom-dato...
        assertThat(postadresse.getGyldighetsperiode().getFom()).isEqualTo(LocalDate.of(2011, 9, 14));
        assertThat(postadresse.getGyldighetsperiode().getTom()).isNull();
        assertThat(postadresse.getBruksperiode().getFom()).isEqualTo(LocalDate.of(2015, 2, 23));
        assertThat(postadresse.getBruksperiode().getTom()).isNull();
    }

    @Test
    void testJuridiskEnhet() throws IOException {
        Saksopplysning saksopplysning = getSaksopplysning(EREG_4_0_MOCK);
        OrganisasjonDokument dokument = (OrganisasjonDokument) saksopplysning.getDokument();

        assertThat(dokument.getSektorkode()).isNotBlank();
        assertThat(dokument.getOrganisasjonDetaljer().getNaering()).isNotEmpty();
        assertThat(dokument.getOppstartsdato()).isNull();
        assertThat(dokument.getEnhetstype()).isNotBlank();
    }

    @Test
    void testOrgledd() throws IOException {
        final String ressurs = "organisasjon/974652366.xml";
        Saksopplysning saksopplysning = getSaksopplysning(ressurs);
        OrganisasjonDokument dokument = (OrganisasjonDokument) saksopplysning.getDokument();

        assertThat(dokument.getSektorkode()).isNotBlank();
        assertThat(dokument.getOrganisasjonDetaljer().getNaering()).isNotEmpty();
        assertThat(dokument.getOppstartsdato()).isNull();
        assertThat(dokument.getEnhetstype()).isEmpty();
    }

    @Test
    void testVirksomhet() throws IOException {
        final String ressurs = "organisasjon/975270211.xml";
        Saksopplysning saksopplysning = getSaksopplysning(ressurs);
        OrganisasjonDokument dokument = (OrganisasjonDokument) saksopplysning.getDokument();

        assertThat(dokument.getSektorkode()).isEmpty();
        assertThat(dokument.getOrganisasjonDetaljer().getNaering()).isNotEmpty();
        assertThat(dokument.getOppstartsdato()).isNotNull();
        assertThat(dokument.getEnhetstype()).isEmpty();
    }

    @Override
    public Saksopplysning getSaksopplysning(String ressurs) throws IOException {
        final InputStream kilde = getClass().getClassLoader().getResourceAsStream(ressurs);
        Objects.requireNonNull(kilde);
        return konverter(kilde, factory, SaksopplysningType.ORG, "4.0");
    }
}
