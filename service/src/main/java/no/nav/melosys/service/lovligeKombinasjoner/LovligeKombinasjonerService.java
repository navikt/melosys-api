package no.nav.melosys.service.lovligeKombinasjoner;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.service.behandling.BehandlingService;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class LovligeKombinasjonerService {
    private BehandlingService behandlingService;

    public LovligeKombinasjonerService(BehandlingService behandlingService) {
        this.behandlingService = behandlingService;
    }

    public Set<Sakstyper> hentAlleMuligeSakstyper() {
        return LovligeSakKombinasjoner.hentAlleMuligeSakstyper();
    }

    public Set<Sakstemaer> hentAlleSakstemaer(
        Aktoersroller hovedpart,
        Sakstyper sakstype
    ) {
        return LovligeSakKombinasjoner.hentAlleSakstemaer(hovedpart, sakstype);
    }

    public Set<Behandlingstema> hentAlleMuligeBehandlingstemaer(
        Aktoersroller hovedpart,
        Sakstyper sakstype,
        Sakstemaer sakstema,
        Behandlingstema sistBehandlingstema
    ) {
        return LovligeSakKombinasjoner.hentAlleMuligeBehandlingstemaer(hovedpart, sakstype, sakstema, sistBehandlingstema);
    }

    public Set<Behandlingstyper> hentAlleMuligeBehandlingstyper(
        Aktoersroller hovedpart,
        Sakstyper sakstype,
        Sakstemaer sakstema,
        Behandlingstema behandlingstema,
        Long sisteBehandlingsID
    ) {
        Behandling sisteBehandling = null;
        if (sisteBehandlingsID != null) {
            sisteBehandling = behandlingService.hentBehandling(sisteBehandlingsID);
        }
        return LovligeSakKombinasjoner.hentAlleMuligeBehandlingstyper(
            hovedpart,
            sakstype,
            sakstema,
            behandlingstema,
            sisteBehandling != null ? sisteBehandling.getTema() : null,
            sisteBehandling != null ? sisteBehandling.getType() : null,
            sisteBehandling != null && sisteBehandling.getFagsak() != null ? sisteBehandling.getFagsak().getStatus() : null);
    }
}
