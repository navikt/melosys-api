package no.nav.melosys.integrasjon.sakogbehandling.behandlingstatus;

import no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer.v1.hendelseshandtererbehandlingsstatus.BehandlingAvsluttet;
import no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer.v1.hendelseshandtererbehandlingsstatus.BehandlingOpprettet;
import no.nav.melosys.exception.IntegrasjonException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("mocking") //FIXME MELOSYS-1284
@Component
public class BehandlingstatusClientMock extends BehandlingstatusClientImpl implements BehandlingstatusClient {
    private static final Logger log = LoggerFactory.getLogger(BehandlingstatusClientMock.class);

    public BehandlingstatusClientMock() throws IntegrasjonException {
        super(null, null);
    }

    @Override
    public void sendBehandlingOpprettet(BehandlingStatusMapper mapper) throws IntegrasjonException {
        BehandlingOpprettet behandlingOpprettet = mapper.tilBehandlingOpprettet();
        String xml = behandlingStatusTilXml(behandlingOpprettet);
        log.info("Mock klient ble bedt om å opprette en behandling.");
        log.info("XML som ville sendes til Sak og Behandling: {}", xml);
    }

    @Override
    public void sendBehandlingAvsluttet(BehandlingStatusMapper mapper) throws IntegrasjonException {
        BehandlingAvsluttet behandlingAvsluttet = mapper.tilBehandlingAvsluttet();
        String xml = behandlingStatusTilXml(behandlingAvsluttet);
        log.info("Mock klient ble bedt om å avslutte en behandling.");
        log.info("XML som ville sendes til Sak og Behandling: {}", xml);
    }
}
