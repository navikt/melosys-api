package no.nav.melosys.domain.dokument.inntekt;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.KonverteringTest;
import no.nav.melosys.domain.dokument.XsltTemplatesFactory;
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.Tilleggsinformasjon;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import static org.assertj.core.api.Assertions.assertThat;

class Inntekt3KonverteringTest implements KonverteringTest {
    private static final String INNTEKT_3_2_MOCK_BOLK = "inntekt/99999999992_mock_bolk.xml";
    private static final String INNTEKT_3_2_MOCK_TILLEGGSINFO = "inntekt/99999999992_mock_tilleggsinformasjon.xml";

    private DokumentFactory factory;

    @BeforeEach
    void setUp() {
        Jaxb2Marshaller marshaller = JaxbConfig.getJaxb2Marshaller();
        XsltTemplatesFactory xsltTemplatesFactory = new XsltTemplatesFactory();
        factory = new DokumentFactory(marshaller, xsltTemplatesFactory);
    }

    @Test
    void testKonvertering() throws Exception {
        Saksopplysning saksopplysning = getSaksopplysning(INNTEKT_3_2_MOCK_BOLK);

        assertThat(saksopplysning.getDokument()).isInstanceOf(InntektDokument.class);

        InntektDokument dokument = (InntektDokument) saksopplysning.getDokument();

        assertThat(dokument.getArbeidsInntektMaanedListe()).isNotEmpty();

        for (ArbeidsInntektMaaned arbeidsInntektMaaned : dokument.getArbeidsInntektMaanedListe()) {
            assertThat(arbeidsInntektMaaned.getArbeidsInntektInformasjon()).isNotNull();

            if (!arbeidsInntektMaaned.getArbeidsInntektInformasjon().getArbeidsforholdListe().isEmpty()) {
                for (ArbeidsforholdFrilanser arbeidsforhold : arbeidsInntektMaaned.getArbeidsInntektInformasjon().getArbeidsforholdListe()) {
                    assertThat(arbeidsforhold.yrke).isNotBlank();
                    assertThat(arbeidsforhold.frilansPeriode).isNotNull();
                }
            }
        }
    }

    @Test
    void testKonverteringTilleggsinformasjon() throws Exception {
        Saksopplysning saksopplysning = getSaksopplysning(INNTEKT_3_2_MOCK_TILLEGGSINFO);
        InntektDokument dokument = (InntektDokument) saksopplysning.getDokument();

        assertThat(dokument.getArbeidsInntektMaanedListe()).isNotEmpty();

        for (ArbeidsInntektMaaned arbeidsInntektMaaned : dokument.getArbeidsInntektMaanedListe()) {
            for (Inntekt inntekt : arbeidsInntektMaaned.getArbeidsInntektInformasjon().getInntektListe()) {
                assertThat(inntekt.getTilleggsinformasjon()).isNotNull();
                Tilleggsinformasjon tilleggsinformasjon = inntekt.getTilleggsinformasjon();
                assertThat(tilleggsinformasjon.kategori).isNotEmpty();
            }
        }
    }

    @Override
    public Saksopplysning getSaksopplysning(String ressurs) throws IOException {
        final InputStream kilde = getClass().getClassLoader().getResourceAsStream(ressurs);
        Objects.requireNonNull(kilde);
        return konverter(kilde, factory, SaksopplysningType.INNTK, "3.2");
    }
}
