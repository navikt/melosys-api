package no.nav.melosys.integrasjon.sakogbehandling;

import java.time.LocalDate;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.sakogbehandling.behandlingstatus.BehandlingStatusMapper;

public interface SakOgBehandlingFasade {

    void sendBehandlingOpprettet(BehandlingStatusMapper mapper) throws IntegrasjonException;

    void sendBehandlingAvsluttet(BehandlingStatusMapper mapper) throws IntegrasjonException;

    Saksopplysning finnSakOgBehandlingskjedeListe(String aktørId) throws IntegrasjonException;
}
