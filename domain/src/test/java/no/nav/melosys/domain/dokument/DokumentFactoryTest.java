package no.nav.melosys.domain.dokument;

import static org.assertj.core.api.Assertions.assertThat;

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
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.adresse.GeografiskAdresse;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;

public class DokumentFactoryTest {

    DokumentFactory factory;

    @Before
    public void setUp() {
        Jaxb2Marshaller marshaller = new JaxbConfig().jaxb2Marshaller();

        XsltTemplatesFactory xsltTemplatesFactory = new XsltTemplatesFactory();
        factory = new DokumentFactory(marshaller, xsltTemplatesFactory);
    }

    @Test
    public void lagDokument() throws Exception {
        Saksopplysning test = new Saksopplysning();

        InputStream kilde = getClass().getClassLoader().getResourceAsStream("arbeidsforhold/99999999995.xml");
        StringBuilder stringBuilder = new StringBuilder();
        try (Reader reader = new BufferedReader(new InputStreamReader
                (kilde, Charset.forName(StandardCharsets.UTF_8.name())))) {
            int c = 0;
            while ((c = reader.read()) != -1) {
                stringBuilder.append((char) c);
            }
        }
        test.setDokumentXml(stringBuilder.toString());

        test.setType(SaksopplysningType.ARBEIDSFORHOLD);
        test.setVersjon("3.0");

        factory.lagDokument(test);

        SaksopplysningDokument dokument = test.getDokument();
        assertThat(dokument).isInstanceOf(ArbeidsforholdDokument.class);
        assertThat(((ArbeidsforholdDokument) dokument).getArbeidsforhold().get(25).getUtenlandsopphold()).isNotEmpty();

    }

    @Test
    public void lagOrganisasjonDokument() throws Exception {
        Saksopplysning test = new Saksopplysning();

        InputStream kilde = getClass().getClassLoader().getResourceAsStream("organisasjon/920643043.xml");
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

        SaksopplysningDokument dokument = test.getDokument();
        assertThat(dokument).isInstanceOf(OrganisasjonDokument.class);
        GeografiskAdresse geografiskAdresse = ((OrganisasjonDokument) dokument).getOrganisasjonDetaljer().getPostadresse().get(0);
        assertThat(geografiskAdresse).isInstanceOf(SemistrukturertAdresse.class);
        assertThat(((SemistrukturertAdresse) geografiskAdresse).getAdresselinje1()).isEqualTo("Skuteviksbodene 1");

    }

}