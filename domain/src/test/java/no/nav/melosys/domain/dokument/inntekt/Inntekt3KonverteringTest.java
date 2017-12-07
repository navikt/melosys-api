package no.nav.melosys.domain.dokument.inntekt;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import no.nav.melosys.domain.dokument.arbeidsforhold.Arbeidsforhold;
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.Tilleggsinformasjon;
import org.junit.Before;
import org.junit.Test;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.XsltTemplatesFactory;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;

public class Inntekt3KonverteringTest {

    public static final String INNTEKT_3_2_MOCK = "inntekt/99999999992.xml";
    private static final String INNTEKT_3_2_MOCK_FRILANS = "inntekt/99999999992_mock_frilans.xml";
    private static final String INNTEKT_3_2_MOCK_TILLEGGSINFO = "inntekt/99999999992_mock_tilleggsinformasjon.xml";

    DokumentFactory factory;

    @Before
    public void setUp() {
        Jaxb2Marshaller marshaller = new JaxbConfig().jaxb2Marshaller();
        XsltTemplatesFactory xsltTemplatesFactory = new XsltTemplatesFactory();
        factory = new DokumentFactory(marshaller, xsltTemplatesFactory);
    }

    @Test
    public void testKonvertering() throws Exception {
        Saksopplysning test = new Saksopplysning();

        InputStream kilde = getClass().getClassLoader().getResourceAsStream(INNTEKT_3_2_MOCK);
        StringBuilder stringBuilder = new StringBuilder();
        try (Reader reader = new BufferedReader(new InputStreamReader
                (kilde, Charset.forName(StandardCharsets.UTF_8.name())))) {
            int c = 0;
            while ((c = reader.read()) != -1) {
                stringBuilder.append((char) c);
            }
        }
        test.setDokumentXml(stringBuilder.toString());
        test.setType(SaksopplysningType.INNTEKT);
        test.setVersjon("3.2");

        factory.lagDokument(test);

        InntektDokument dokument = (InntektDokument) test.getDokument();
        assertThat(dokument).isNotNull();
    }

    @Test
    public void testKonverteringFrilans() throws Exception {
        final InputStream kilde = getClass().getClassLoader().getResourceAsStream(INNTEKT_3_2_MOCK_FRILANS);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(kilde, Charset.forName("UTF-8")))) {
            Saksopplysning saksopplysning = new Saksopplysning();

            String xmlStr = reader.lines().collect(Collectors.joining(System.lineSeparator()));

            saksopplysning.setDokumentXml(xmlStr);
            saksopplysning.setType(SaksopplysningType.INNTEKT);
            saksopplysning.setVersjon("3.2");

            factory.lagDokument(saksopplysning);

            assertThat(saksopplysning.getDokument()).isInstanceOf(InntektDokument.class);

            InntektDokument dokument = (InntektDokument) saksopplysning.getDokument();

            assertThat(dokument.getArbeidsInntektMaanedListe()).isNotEmpty();

            for (ArbeidsInntektMaaned arbeidsInntektMaaned : dokument.getArbeidsInntektMaanedListe()) {

                assertThat(arbeidsInntektMaaned.getArbeidsInntektInformasjon()).isNotNull();
                assertThat(arbeidsInntektMaaned.getArbeidsInntektInformasjon().getArbeidsforholdListe()).isNotEmpty();

                for (ArbeidsforholdFrilanser arbeidsforhold : arbeidsInntektMaaned.getArbeidsInntektInformasjon().getArbeidsforholdListe()) {
                    assertThat(arbeidsforhold.yrke).isNotBlank();
                    assertThat(arbeidsforhold.frilansPeriode).isNotNull();
                }
            }
        }
    }

    @Test
    public void testKonverteringTilleggsinformasjon() throws Exception {
        final InputStream kilde = getClass().getClassLoader().getResourceAsStream(INNTEKT_3_2_MOCK_TILLEGGSINFO);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(kilde, Charset.forName("UTF-8")))) {
            Saksopplysning saksopplysning = new Saksopplysning();

            String xmlStr = reader.lines().collect(Collectors.joining(System.lineSeparator()));

            saksopplysning.setDokumentXml(xmlStr);
            saksopplysning.setType(SaksopplysningType.INNTEKT);
            saksopplysning.setVersjon("3.2");

            factory.lagDokument(saksopplysning);

            InntektDokument dokument = (InntektDokument) saksopplysning.getDokument();

            assertThat(dokument.getArbeidsInntektMaanedListe()).isNotEmpty();

            for (ArbeidsInntektMaaned arbeidsInntektMaaned : dokument.getArbeidsInntektMaanedListe()) {

                for (Inntekt inntekt : arbeidsInntektMaaned.getArbeidsInntektInformasjon().getInntektListe()) {
                    assertThat(inntekt.getTilleggsinformasjon()).isNotNull();
                    Tilleggsinformasjon tilleggsinformasjon = inntekt.getTilleggsinformasjon();
                    assertThat(tilleggsinformasjon.kategori).isNotBlank();
                    assertThat(tilleggsinformasjon.tilleggsinformasjonDetaljer).isNotNull();
                }
            }
        }
    }
}