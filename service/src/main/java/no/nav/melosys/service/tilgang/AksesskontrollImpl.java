package no.nav.melosys.service.tilgang;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.sak.FagsakService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.service.tilgang.Aksesstype.LES;
import static no.nav.melosys.service.tilgang.Aksesstype.SKRIV;

@Component
@Transactional(readOnly = true)
public class AksesskontrollImpl implements Aksesskontroll {

    private final FagsakService fagsakService;
    private final BehandlingService behandlingService;
    private final TilgangService tilgangService;
    private final RedigerbarKontroll redigerbarKontroll;

    public AksesskontrollImpl(FagsakService fagsakService,
                              BehandlingService behandlingService,
                              TilgangService tilgangService,
                              RedigerbarKontroll redigerbarKontroll) {
        this.fagsakService = fagsakService;
        this.behandlingService = behandlingService;
        this.tilgangService = tilgangService;
        this.redigerbarKontroll = redigerbarKontroll;
    }

    @Override
    public void autoriserSakstilgang(String saksnummer) {
        tilgangService.validerTilgangTilAktørID(fagsakService.hentFagsak(saksnummer).hentAktørID());
    }

    @Override
    public void autoriser(long behandlingID) {
        autoriser(behandlingID, LES);
    }

    @Override
    public void autoriser(long behandlingID, Aksesstype aksesstype) {
        autoriser(behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID), aksesstype, Ressurs.UKJENT);
    }

    @Override
    public void autoriserSkrivTilRessurs(long behandlingID, Ressurs ressurs) {
        autoriser(behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID), SKRIV, ressurs);
    }

    private void autoriser(Behandling behandling, Aksesstype aksesstype, Ressurs ressurs) {
        tilgangService.validerTilgangTilAktørID(behandling.getFagsak().hentAktørID());

        if (aksesstype == SKRIV) {
            redigerbarKontroll.sjekkRessursRedigerbar(behandling, ressurs);
        }
    }
}
