package no.nav.melosys.service.tilgang;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
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
        autoriserSakstilgang(fagsakService.hentFagsak(saksnummer));
    }

    public void autoriserSakstilgang(Fagsak fagsak) {
        tilgangService.validerTilgangTilAktørID(fagsak.hentAktørID());
    }

    @Override
    public void autoriser(long behandlingID) {
        autoriser(behandlingID, LES);
    }

    @Override
    public void autoriser(long behandlingID, Aksesstype aksesstype) {
        autoriser(behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID), aksesstype, Ressurs.UKJENT, false);
    }

    @Override
    public void autoriserSkriv(long behandlingID) {
        autoriser(behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID), SKRIV, Ressurs.UKJENT, false);
    }

    public void autoriserSkrivOgTilordnet(long behandlingID) {
        autoriser(behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID), SKRIV, Ressurs.UKJENT, true);
    }

    @Override
    public void autoriserSkrivTilRessurs(long behandlingID, Ressurs ressurs) {
        autoriser(behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID), SKRIV, ressurs, false);
    }

    @Override
    public void autoriserFolkeregisterIdent(String folkeregisterIdent) {
        tilgangService.validerTilgangTilFolkeregisterIdent(folkeregisterIdent);
    }

    private void autoriser(Behandling behandling, Aksesstype aksesstype, Ressurs ressurs, boolean validerTilordnet) {
        tilgangService.validerTilgangTilAktørID(behandling.getFagsak().hentAktørID());

        if (aksesstype == SKRIV) {
            if (validerTilordnet) {
                redigerbarKontroll.sjekkTilordnetSaksbehandlerOgRedigerbar(behandling, ressurs, SubjectHandler.getInstance().getUserID());
            } else {
                redigerbarKontroll.sjekkRessursRedigerbar(behandling, ressurs);
            }
        }
    }
}
