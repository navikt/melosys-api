package no.nav.melosys.service.sak;

import java.util.Collections;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.lovligekombinasjoner.LovligeKombinasjonerService;
import org.springframework.stereotype.Component;

@Component
class MuligeManuelleFagsakEndringer {
    private final LovligeKombinasjonerService lovligeKombinasjonerService;

    public MuligeManuelleFagsakEndringer(LovligeKombinasjonerService lovligeKombinasjonerService) {
        this.lovligeKombinasjonerService = lovligeKombinasjonerService;
    }

    Set<Sakstyper> hentMuligeSakstype(Behandling behandling) {
        if (behandling.kanIkkeEndres() && !behandling.getFagsak().kanEndreTypeOgTema()) {
            return Collections.emptySet();
        }
        return lovligeKombinasjonerService.hentMuligeSakstyper();
    }

    Set<Sakstemaer> hentMuligeSakstema(Behandling behandling, Sakstyper sakstype) {
        if (behandling.kanIkkeEndres() && !behandling.getFagsak().kanEndreTypeOgTema()) {
            return Collections.emptySet();
        }
        return lovligeKombinasjonerService.hentMuligeSakstemaer(behandling.getFagsak().getHovedpartRolle(), sakstype);
    }

    void validerNySakstypeMulig(Behandling behandling, Sakstyper sakstype) {
        if (!hentMuligeSakstype(behandling).contains(sakstype)) {
            throw new FunksjonellException(String.format("Behandlingen kan ikke endres til sakstype %s. Gyldige sakstype for behandling %s er %s",
                                                         sakstype, behandling.getId(), hentMuligeSakstype(behandling)));
        }
    }

    void validerNySakstemaMulig(Behandling behandling, Sakstyper sakstype, Sakstemaer sakstemaer) {
        if (!hentMuligeSakstema(behandling, sakstype).contains(sakstemaer)) {
            throw new FunksjonellException(String.format("Behandlingen kan ikke endres til sakstema %s. Gyldige sakstema for behandling %s er %s",
                sakstemaer, behandling.getId(), hentMuligeSakstema(behandling, sakstype)));
        }
    }
}
