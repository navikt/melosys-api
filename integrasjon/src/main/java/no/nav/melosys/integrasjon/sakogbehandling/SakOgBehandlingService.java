package no.nav.melosys.integrasjon.sakogbehandling;

import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningKilde;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.sakogbehandling.behandlingskjede.BehandlingskjedeConsumer;
import no.nav.melosys.integrasjon.sakogbehandling.behandlingstatus.BehandlingStatusMapper;
import no.nav.melosys.integrasjon.sakogbehandling.behandlingstatus.BehandlingstatusClient;
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.meldinger.FinnSakOgBehandlingskjedeListeRequest;
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.meldinger.FinnSakOgBehandlingskjedeListeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SakOgBehandlingService implements SakOgBehandlingFasade {

    private static final Logger log = LoggerFactory.getLogger(SakOgBehandlingService.class);

    private static final String SOB_VERSJON = "1.0";

    private final BehandlingskjedeConsumer behandlingskjedeConsumer;

    private final BehandlingstatusClient behandlingstatusClient;

    private DokumentFactory dokumentFactory;

    private final JAXBContext jaxbContext;

    @Autowired
    public SakOgBehandlingService(BehandlingskjedeConsumer behandlingskjedeConsumer, BehandlingstatusClient behandlingstatusClient, DokumentFactory dokumentFactory) {
        this.behandlingskjedeConsumer = behandlingskjedeConsumer;
        this.behandlingstatusClient = behandlingstatusClient;
        this.dokumentFactory = dokumentFactory;

        try {
            jaxbContext = JAXBContext.newInstance(no.nav.tjeneste.virksomhet.sakogbehandling.v1.FinnSakOgBehandlingskjedeListeResponse.class);
        } catch (JAXBException e) {
            log.error("", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendBehandlingOpprettet(BehandlingStatusMapper mapper) throws IntegrasjonException {
        behandlingstatusClient.sendBehandlingOpprettet(mapper);
    }

    @Override
    public void sendBehandlingAvsluttet(BehandlingStatusMapper mapper) throws IntegrasjonException {
        behandlingstatusClient.sendBehandlingAvsluttet(mapper);
    }

    @Override
    public Saksopplysning finnSakOgBehandlingskjedeListe(String aktørId) throws IntegrasjonException {

        FinnSakOgBehandlingskjedeListeRequest request = new FinnSakOgBehandlingskjedeListeRequest();

        request.setAktoerREF(aktørId);

        FinnSakOgBehandlingskjedeListeResponse response = behandlingskjedeConsumer.finnSakOgBehandlingskjedeListeResponse(request);

        // Response -> xml
        StringWriter xmlWriter = new StringWriter();
        try {
            no.nav.tjeneste.virksomhet.sakogbehandling.v1.FinnSakOgBehandlingskjedeListeResponse xmlRoot = new no.nav.tjeneste.virksomhet.sakogbehandling.v1.FinnSakOgBehandlingskjedeListeResponse();
            xmlRoot.setResponse(response);
            jaxbContext.createMarshaller().marshal(xmlRoot, xmlWriter);
        } catch (JAXBException e) {
            throw new IntegrasjonException(e);
        }

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokumentXml(xmlWriter.toString());
        saksopplysning.setKilde(SaksopplysningKilde.SOB);
        saksopplysning.setType(SaksopplysningType.SOB_SAK);
        saksopplysning.setVersjon(SOB_VERSJON);

        // xml -> java objekter
        dokumentFactory.lagDokument(saksopplysning);

        return saksopplysning;
    }
}
