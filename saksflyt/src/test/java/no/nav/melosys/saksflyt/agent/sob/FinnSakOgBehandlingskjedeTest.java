package no.nav.melosys.saksflyt.agent.sob;

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
import no.nav.melosys.integrasjon.sakogbehandling.behandlingstatus.BehandlingstatusClient;
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.FinnSakOgBehandlingskjedeListeResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FinnSakOgBehandlingskjedeTest {

    @Mock
    private BehandlingskjedeConsumer behandlingskjedeConsumer;

    @Mock
    private BehandlingstatusClient behandlingstatusClient;

    private SakOgBehandlingService sakOgBehandlingService;

    @Before
    public void setup() throws Exception {
        DokumentFactory dokumentFactory = new DokumentFactory(
            new JaxbConfig().jaxb2Marshaller(), new XsltTemplatesFactory());

        sakOgBehandlingService = new SakOgBehandlingService(
            behandlingskjedeConsumer, behandlingstatusClient, dokumentFactory);

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
