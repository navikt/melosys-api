package no.nav.melosys.service.sob;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.TemaFactory;
import no.nav.melosys.integrasjon.sakogbehandling.SakOgBehandlingFasade;
import no.nav.melosys.integrasjon.sakogbehandling.behandlingstatus.BehandlingStatusMapper;
import no.nav.melosys.service.behandling.BehandlingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SobService {
    private final BehandlingService behandlingService;
    private final SakOgBehandlingFasade sakOgBehandlingFasade;

    @Autowired
    public SobService(BehandlingService behandlingService, SakOgBehandlingFasade sakOgBehandlingFasade) {
        this.behandlingService = behandlingService;
        this.sakOgBehandlingFasade = sakOgBehandlingFasade;
    }

    private static BehandlingStatusMapper lagBehandlingStatusMapper(String saksnummer, Behandling behandling, String aktørID) {
        return new BehandlingStatusMapper.Builder()
            .medBehandlingsId(behandling.getId())
            .medSaksnummer(saksnummer)
            .medArkivtema(TemaFactory.fraBehandlingstema(behandling.getTema()).getKode())
            .medAktørID(aktørID)
            .build();
    }

    public Saksopplysning finnSakOgBehandlingskjedeListe(String aktørID) {
        return sakOgBehandlingFasade.finnSakOgBehandlingskjedeListe(aktørID);
    }

    public void sakOgBehandlingOpprettet(long behandlingID) {
        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);
        Fagsak fagsak = behandling.getFagsak();
        sakOgBehandlingFasade.sendBehandlingOpprettet(
            lagBehandlingStatusMapper(fagsak.getSaksnummer(), behandling, fagsak.hentAktørID())
        );
    }

    public void sakOgBehandlingAvsluttet(long behandlingID) {
        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);
        Fagsak fagsak = behandling.getFagsak();
        sakOgBehandlingFasade.sendBehandlingAvsluttet(
            lagBehandlingStatusMapper(fagsak.getSaksnummer(), behandling, fagsak.hentAktørID())
        );
    }
}
