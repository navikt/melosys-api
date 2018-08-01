package no.nav.melosys.integrasjon.sakogbehandling;

import java.time.LocalDate;
import java.util.List;

import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.sakogbehandling.behandlingskjede.BehandlingskjedeConsumer;
import no.nav.melosys.integrasjon.sakogbehandling.behandlingskjede.SakDto;
import no.nav.melosys.integrasjon.sakogbehandling.behandlingstatus.BehandlingStatusMapper;
import no.nav.melosys.integrasjon.sakogbehandling.behandlingstatus.BehandlingstatusClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SakOgBehandlingService implements SakOgBehandlingFasade {

    private final BehandlingskjedeConsumer behandlingskjedeConsumer;

    private final BehandlingstatusClient behandlingstatusClient;

    @Autowired
    public SakOgBehandlingService(BehandlingskjedeConsumer behandlingskjedeConsumer, BehandlingstatusClient behandlingstatusClient) {
        this.behandlingskjedeConsumer = behandlingskjedeConsumer;
        this.behandlingstatusClient = behandlingstatusClient;
    }

    @Override
    public List<SakDto> finnSakOgBehandlingskjedeListeResponse(String aktørId, LocalDate tidspunkt) throws IntegrasjonException {
        return behandlingskjedeConsumer.finnSakOgBehandlingskjedeListeResponse(aktørId, tidspunkt);
    }

    @Override
    public void sendBehandlingOpprettet(BehandlingStatusMapper mapper) throws IntegrasjonException {
        behandlingstatusClient.sendBehandlingOpprettet(mapper);
    }

    @Override
    public void sendBehandlingAvsluttet(BehandlingStatusMapper mapper) throws IntegrasjonException {
        behandlingstatusClient.sendBehandlingAvsluttet(mapper);
    }
}
