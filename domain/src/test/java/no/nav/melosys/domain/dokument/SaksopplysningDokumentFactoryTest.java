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
import no.nav.melosys.domain.dokument.person.PersonopplysningDokument;

public class SaksopplysningDokumentFactoryTest {

    SaksopplysningDokumentFactory factory;

    @Before
    public void setUp() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setClassesToBeBound(ArbeidsforholdDokument.class, PersonopplysningDokument.class);
        XsltTemplatesFactory xsltTemplatesFactory = new XsltTemplatesFactory();
        factory = new SaksopplysningDokumentFactory(marshaller, xsltTemplatesFactory);
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

}