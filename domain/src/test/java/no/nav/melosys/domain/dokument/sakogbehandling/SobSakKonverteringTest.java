package no.nav.melosys.domain.dokument.sakogbehandling;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.XsltTemplatesFactory;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import org.junit.Before;
import org.junit.Test;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import static org.assertj.core.api.Assertions.assertThat;

public class SobSakKonverteringTest {

    private static final String EØS_BARNETRYGD = "sakogbehandling/eos_barnetrygd.xml";
    private static final String INGEN_SAKER = "sakogbehandling/ingen_saker.xml";

    DokumentFactory factory;

    @Before
    public void setUp() {
        Jaxb2Marshaller marshaller = JaxbConfig.jaxb2Marshaller();
        XsltTemplatesFactory xsltTemplatesFactory = new XsltTemplatesFactory();
        factory = new DokumentFactory(marshaller, xsltTemplatesFactory);
    }

    @Test
    public void testKonverteringBarnetrygd() throws Exception {
        final InputStream kilde = getClass().getClassLoader().getResourceAsStream(EØS_BARNETRYGD);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(kilde, StandardCharsets.UTF_8))) {
            Saksopplysning saksopplysning = new Saksopplysning();

            String xmlStr = reader.lines().collect(Collectors.joining(System.lineSeparator()));

            saksopplysning.setDokumentXml(xmlStr);
            saksopplysning.setType(SaksopplysningType.SOB_SAK);
            saksopplysning.setVersjon("1.0");

            factory.lagDokument(saksopplysning);

            SobSakDokument dokument = (SobSakDokument) saksopplysning.getDokument();

            assertThat(dokument).isNotNull();
            assertThat(dokument.harEøsBarnetrygd()).isTrue();
            assertThat(dokument.getSakstema()).isNotEmpty();
            assertThat(dokument.getBehandlingskjede()).isNotEmpty();

            for (Behandlingskjede behandlingskjede : dokument.getBehandlingskjede()) {
                assertThat(behandlingskjede.getBehandlingskjedetype()).isNotEmpty();
                assertThat(behandlingskjede.getBehandlingstema()).isNotEmpty();
            }

        }
    }

    @Test
    public void testKonverteringIngenSaker() throws Exception {
        final InputStream kilde = getClass().getClassLoader().getResourceAsStream(INGEN_SAKER);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(kilde, StandardCharsets.UTF_8))) {
            Saksopplysning saksopplysning = new Saksopplysning();

            String xmlStr = reader.lines().collect(Collectors.joining(System.lineSeparator()));

            saksopplysning.setDokumentXml(xmlStr);
            saksopplysning.setType(SaksopplysningType.SOB_SAK);
            saksopplysning.setVersjon("1.0");

            factory.lagDokument(saksopplysning);

            SobSakDokument dokument = (SobSakDokument) saksopplysning.getDokument();

            assertThat(dokument).isNotNull();
            assertThat(dokument.harEøsBarnetrygd()).isFalse();
        }
    }

}
