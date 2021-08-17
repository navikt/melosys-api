package no.nav.melosys.integrasjon.sakogbehandling;

import java.io.StringWriter;
import javax.xml.bind.JAXBException;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningKildesystem;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.sakogbehandling.behandlingskjede.BehandlingskjedeConsumer;
import no.nav.melosys.integrasjon.sakogbehandling.behandlingstatus.BehandlingStatusMapper;
import no.nav.melosys.integrasjon.sakogbehandling.behandlingstatus.BehandlingstatusClient;
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.meldinger.FinnSakOgBehandlingskjedeListeRequest;
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.meldinger.FinnSakOgBehandlingskjedeListeResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SakOgBehandlingService implements SakOgBehandlingFasade {
    private static final String SOB_VERSJON = "1.0";

    private final BehandlingskjedeConsumer behandlingskjedeConsumer;
    private final BehandlingstatusClient behandlingstatusClient;
    private final DokumentFactory dokumentFactory;

    @Autowired
    public SakOgBehandlingService(BehandlingskjedeConsumer behandlingskjedeConsumer, BehandlingstatusClient behandlingstatusClient, DokumentFactory dokumentFactory) {
        this.behandlingskjedeConsumer = behandlingskjedeConsumer;
        this.behandlingstatusClient = behandlingstatusClient;
        this.dokumentFactory = dokumentFactory;
    }

    @Override
    public void sendBehandlingOpprettet(BehandlingStatusMapper mapper) {
        //FIXME: MELOSYS-4655
        //behandlingstatusClient.sendBehandlingOpprettet(mapper);
    }

    @Override
    public void sendBehandlingAvsluttet(BehandlingStatusMapper mapper) {
        //FIXME: MELOSYS-4655
        //behandlingstatusClient.sendBehandlingAvsluttet(mapper);
    }

    @Override
    public Saksopplysning finnSakOgBehandlingskjedeListe(String aktørId) {

        FinnSakOgBehandlingskjedeListeRequest request = new FinnSakOgBehandlingskjedeListeRequest();

        request.setAktoerREF(aktørId);

        FinnSakOgBehandlingskjedeListeResponse response = behandlingskjedeConsumer.finnSakOgBehandlingskjedeListeResponse(request);

        // Response -> xml
        StringWriter xmlWriter = new StringWriter();
        try {
            no.nav.tjeneste.virksomhet.sakogbehandling.v1.FinnSakOgBehandlingskjedeListeResponse xmlRoot = new no.nav.tjeneste.virksomhet.sakogbehandling.v1.FinnSakOgBehandlingskjedeListeResponse();
            xmlRoot.setResponse(response);
            dokumentFactory.createMarshaller().marshal(xmlRoot, xmlWriter);
        } catch (JAXBException e) {
            throw new IntegrasjonException(e);
        }

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.leggTilKildesystemOgMottattDokument(
            SaksopplysningKildesystem.SOB, xmlWriter.toString());
        saksopplysning.setType(SaksopplysningType.SOB_SAK);
        saksopplysning.setVersjon(SOB_VERSJON);

        // xml -> java objekter
        dokumentFactory.lagDokument(saksopplysning);

        return saksopplysning;
    }
}
