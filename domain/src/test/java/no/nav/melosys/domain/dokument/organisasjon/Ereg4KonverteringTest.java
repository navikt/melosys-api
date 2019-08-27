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
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import static org.junit.Assert.*;

// Denne konverteringen testes også i DokumentFactoryTest, uten strukturert adresse
public class Ereg4KonverteringTest implements KonverteringTest {
    private static final String EREG_4_0_MOCK = "organisasjon/org_med_strukturert_adresse.xml";
    private static DokumentFactory factory;

    @BeforeClass
    public static void setUp() {
        Jaxb2Marshaller marshaller = new JaxbConfig().jaxb2Marshaller();
        XsltTemplatesFactory xsltTemplatesFactory = new XsltTemplatesFactory();
        factory = new DokumentFactory(marshaller, xsltTemplatesFactory);
    }

    @Test
    public void testAdresse() throws Exception {
        Saksopplysning test = getSaksopplysning(EREG_4_0_MOCK);

        factory.lagDokument(test);

        // Test semistrukturert adresse...
        OrganisasjonDokument dokument = (OrganisasjonDokument) test.getDokument();
        SemistrukturertAdresse postadresse = (SemistrukturertAdresse) dokument.getOrganisasjonDetaljer().getPostadresse().get(0);
        assertEquals("Skuteviksbodene 1", postadresse.getAdresselinje1());
        assertEquals("5035", postadresse.getPostnr());
        assertEquals("1201", postadresse.getKommunenr());

        // Test strukturert adresse...
        Gateadresse forretningsadresse = (Gateadresse) dokument.getOrganisasjonDetaljer().getForretningsadresser().get(0);
        assertEquals("Gatenavn", forretningsadresse.getGatenavn());
        assertEquals("NO", forretningsadresse.getLandkode());
        
        // Test perioder: 
        // Forretningsadresse har ingen perioder satt...
        assertNull(forretningsadresse.getBruksperiode());
        assertNull(forretningsadresse.getGyldighetsperiode());
        // Postadresse har fom-dato på begge periodene, men ikke tom-dato...
        assertEquals(LocalDate.of(2011, 9, 14), postadresse.getGyldighetsperiode().getFom());
        assertNull(postadresse.getGyldighetsperiode().getTom());
        assertEquals(LocalDate.of(2015, 2, 23), postadresse.getBruksperiode().getFom());
        assertNull(postadresse.getBruksperiode().getTom());
    }

    @Test
    public void testJuridiskEnhet() throws IOException {
        Saksopplysning saksopplysning = getSaksopplysning(EREG_4_0_MOCK);
        OrganisasjonDokument dokument = (OrganisasjonDokument) saksopplysning.getDokument();

        assertNotEquals("", dokument.getSektorkode());
        assertFalse(dokument.getOrganisasjonDetaljer().getNaering().isEmpty());
        assertNull(dokument.getOppstartsdato());
        assertNotEquals("", dokument.getEnhetstype());
    }

    @Test
    public void testOrgledd() throws IOException {
        final String ressurs = "organisasjon/974652366.xml";
        Saksopplysning saksopplysning = getSaksopplysning(ressurs);
        OrganisasjonDokument dokument = (OrganisasjonDokument) saksopplysning.getDokument();

        assertNotEquals("", dokument.getSektorkode());
        assertFalse(dokument.getOrganisasjonDetaljer().getNaering().isEmpty());
        assertNull(dokument.getOppstartsdato());
        assertEquals("", dokument.getEnhetstype());
    }

    @Test
    public void testVirksomhet() throws IOException {
        final String ressurs = "organisasjon/975270211.xml";
        Saksopplysning saksopplysning = getSaksopplysning(ressurs);
        OrganisasjonDokument dokument = (OrganisasjonDokument) saksopplysning.getDokument();

        assertEquals("", dokument.getSektorkode());
        assertFalse(dokument.getOrganisasjonDetaljer().getNaering().isEmpty());
        assertNotNull(dokument.getOppstartsdato());
        assertEquals("", dokument.getEnhetstype());
    }

    @Override
    public Saksopplysning getSaksopplysning(String ressurs) throws IOException {
        final InputStream kilde = getClass().getClassLoader().getResourceAsStream(ressurs);
        Objects.requireNonNull(kilde);
        return konverter(kilde, factory, SaksopplysningType.ORG, "4.0");
    }
}
