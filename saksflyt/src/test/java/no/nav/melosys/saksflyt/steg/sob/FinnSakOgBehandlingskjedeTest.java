package no.nav.melosys.saksflyt.steg.sob;

import java.net.URL;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.XsltTemplatesFactory;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import no.nav.melosys.domain.dokument.sakogbehandling.SobSakDokument;
import no.nav.melosys.integrasjon.sakogbehandling.SakOgBehandlingService;
import no.nav.melosys.integrasjon.sakogbehandling.behandlingskjede.BehandlingskjedeConsumer;
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.FinnSakOgBehandlingskjedeListeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FinnSakOgBehandlingskjedeTest {

    @Mock
    private BehandlingskjedeConsumer behandlingskjedeConsumer;

    private SakOgBehandlingService sakOgBehandlingService;

    @BeforeEach
    public void setup() throws Exception {
        DokumentFactory dokumentFactory = new DokumentFactory(
            JaxbConfig.jaxb2Marshaller(), new XsltTemplatesFactory());

        sakOgBehandlingService = new SakOgBehandlingService(
            behandlingskjedeConsumer, dokumentFactory);

        // Lag respons fra xml
        URL xmlSource = getClass().getClassLoader().getResource("sakogbehandling/eos_barnetrygd.xml");
        Unmarshaller unmarshaller = JAXBContext
            .newInstance(FinnSakOgBehandlingskjedeListeResponse.class).createUnmarshaller();
        FinnSakOgBehandlingskjedeListeResponse response =
            (FinnSakOgBehandlingskjedeListeResponse) unmarshaller.unmarshal(xmlSource);

        when(behandlingskjedeConsumer.finnSakOgBehandlingskjedeListeResponse(any()))
            .thenReturn(response.getResponse());
    }

    @Test
    public void finnSakOgBehandlingskjedeList_expectCorrectlyFormattedDocument() throws Exception {
        Saksopplysning saksopplysning = sakOgBehandlingService.finnSakOgBehandlingskjedeListe("123123123");

        assertThat(saksopplysning).isInstanceOf(Saksopplysning.class);
        assertThat(saksopplysning.getDokument()).isInstanceOf(SobSakDokument.class);

        SobSakDokument dokument = (SobSakDokument) saksopplysning.getDokument();
        assertThat(dokument.getSakstema()).isEqualTo("BAR");
        assertThat(dokument.harEøsBarnetrygd()).isTrue();
        assertThat(dokument.getBehandlingskjede()).hasSize(2);

        assertThat(dokument.getBehandlingskjede())
            .extracting("behandlingskjedetype", "behandlingstema")
            .contains(
                tuple("ad0003", "ab0058"),
                tuple("ukjent", null));
    }
}
