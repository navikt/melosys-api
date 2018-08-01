package no.nav.melosys.integrasjon.sakogbehandling;

import java.time.LocalDate;
import java.util.List;

import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.sakogbehandling.behandlingskjede.SakDto;
import no.nav.melosys.integrasjon.sakogbehandling.behandlingstatus.BehandlingStatusMapper;

public interface SakOgBehandlingFasade {

    List<SakDto> finnSakOgBehandlingskjedeListeResponse(String aktørId, LocalDate tidspunkt) throws IntegrasjonException;

    void sendBehandlingOpprettet(BehandlingStatusMapper mapper) throws IntegrasjonException;

    void sendBehandlingAvsluttet(BehandlingStatusMapper mapper) throws IntegrasjonException;
}
