package no.nav.melosys.domain.dokument.organisasjon;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.Test;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.XsltTemplatesFactory;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import no.nav.melosys.domain.dokument.organisasjon.adresse.Gateadresse;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;

// Denne konverteringen testes også i DokumentFactoryTest, uten strukturert adresse
public class Ereg4KonverteringTest {

    public static final String EREG_4_0_MOCK = "organisasjon/org_med_strukturert_adresse.xml";

    DokumentFactory factory;

    @Before
    public void setUp() {
        Jaxb2Marshaller marshaller = new JaxbConfig().jaxb2Marshaller();
        XsltTemplatesFactory xsltTemplatesFactory = new XsltTemplatesFactory();
        factory = new DokumentFactory(marshaller, xsltTemplatesFactory);
    }

    @Test
    public void testAdresse() throws Exception {
        Saksopplysning test = new Saksopplysning();

        InputStream kilde = getClass().getClassLoader().getResourceAsStream(EREG_4_0_MOCK);
        StringBuilder stringBuilder = new StringBuilder();
        try (Reader reader = new BufferedReader(new InputStreamReader
                (kilde, Charset.forName(StandardCharsets.UTF_8.name())))) {
            int c = 0;
            while ((c = reader.read()) != -1) {
                stringBuilder.append((char) c);
            }
        }
        test.setDokumentXml(stringBuilder.toString());
        test.setType(SaksopplysningType.ORGANISASJON);
        test.setVersjon("4.0");

        factory.lagDokument(test);

        // Test semistrukturert adresse...
        OrganisasjonDokument dokument = (OrganisasjonDokument) test.getDokument();
        SemistrukturertAdresse postadresse = (SemistrukturertAdresse) dokument.getOrganisasjonDetaljer().getPostadresse().get(0);
        assertEquals("Skuteviksbodene 1", postadresse.getAdresselinje1());
        assertEquals("5035", postadresse.getPostnr());
        assertEquals("1201", postadresse.getKommunenr());

        // Test strukturert adresse...
        Gateadresse adresse = (Gateadresse) dokument.getOrganisasjonDetaljer().getForretningsadresse().get(0);
        assertEquals("Gatenavn", adresse.getGatenavn());
        assertEquals("NO", adresse.getLandkode());

    }

}
