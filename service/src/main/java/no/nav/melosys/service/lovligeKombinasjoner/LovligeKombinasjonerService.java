package no.nav.melosys.service.lovligeKombinasjoner;

import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.service.behandling.BehandlingService;

public class LovligeKombinasjonerService {
    private BehandlingService behandlingService;
    private LovligeKombinasjoner lovligeKombinasjoner;

    public LovligeKombinasjonerService(BehandlingService behandlingService, LovligeKombinasjoner lovligeKombinasjoner) {
        this.behandlingService = behandlingService;
        this.lovligeKombinasjoner = lovligeKombinasjoner;
    }

    public Set<Sakstyper> hentAlleMuligeSakstyper() {
        return LovligeKombinasjoner.hentAlleMuligeSakstyper();
    }

    public Set<Sakstemaer> hentAlleSakstemaer(
        Aktoersroller hovedpart,
        Sakstyper sakstype
    ) {
        return LovligeKombinasjoner.hentAlleSakstemaer(hovedpart, sakstype);
    }

    public Set<Behandlingstema> hentAlleMuligeBehandlingstemaer(
        Aktoersroller hovedpart,
        Sakstyper sakstype,
        Sakstemaer sakstema,
        Behandlingstema sistBehandlingstema
    ) {
        return LovligeKombinasjoner.hentAlleMuligeBehandlingstemaer(hovedpart, sakstype, sakstema, sistBehandlingstema);
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
        return lovligeKombinasjoner.hentAlleMuligeBehandlingstyper(
            hovedpart,
            sakstype,
            sakstema,
            behandlingstema,
            sisteBehandling != null ? sisteBehandling.getTema() : null,
            sisteBehandling != null ? sisteBehandling.getType() : null,
            sisteBehandling != null && sisteBehandling.getFagsak() != null ? sisteBehandling.getFagsak().getStatus() : null);
    }
}
