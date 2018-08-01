package no.nav.melosys.integrasjon.sakogbehandling;

import java.time.LocalDate;
import java.util.List;

import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.sakogbehandling.behandlingskjede.BehandlingskjedeConsumer;
import no.nav.melosys.integrasjon.sakogbehandling.behandlingskjede.SakDto;
import no.nav.melosys.integrasjon.sakogbehandling.behandlingstatus.BehandlingStatusMapper;
import no.nav.melosys.integrasjon.sakogbehandling.behandlingstatus.SakOgBehandlingClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SakOgBehandlingService implements SakOgBehandlingFasade {

    private final BehandlingskjedeConsumer behandlingskjedeConsumer;

    private final SakOgBehandlingClient sakOgBehandlingClient;

    @Autowired
    public SakOgBehandlingService(BehandlingskjedeConsumer behandlingskjedeConsumer, SakOgBehandlingClient sakOgBehandlingClient) {
        this.behandlingskjedeConsumer = behandlingskjedeConsumer;
        this.sakOgBehandlingClient = sakOgBehandlingClient;
    }

    @Override
    public List<SakDto> finnSakOgBehandlingskjedeListeResponse(String aktørId, LocalDate tidspunkt) throws IntegrasjonException {
        return behandlingskjedeConsumer.finnSakOgBehandlingskjedeListeResponse(aktørId, tidspunkt);
    }

    @Override
    public void sendBehandlingOpprettet(BehandlingStatusMapper mapper) throws IntegrasjonException {
        sakOgBehandlingClient.sendBehandlingOpprettet(mapper);
    }

    @Override
    public void sendBehandlingAvsluttet(BehandlingStatusMapper mapper) throws IntegrasjonException {
        sakOgBehandlingClient.sendBehandlingAvsluttet(mapper);
    }
}
