package no.nav.melosys.saksflyt.steg.afl;

import java.util.Set;

import no.nav.melosys.domain.Kontrollresultat;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.KontrollresultatService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.springframework.stereotype.Component;

@Component("AFLRegisterKontroll")
public class RegisterKontroll extends AbstraktStegBehandler {

    private final KontrollresultatService kontrollresultatService;
    private final BehandlingsresultatService behandlingsresultatService;

    public RegisterKontroll(KontrollresultatService kontrollresultatService, BehandlingsresultatService behandlingsresultatService) {
        this.kontrollresultatService = kontrollresultatService;
        this.behandlingsresultatService = behandlingsresultatService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AFL_REGISTERKONTROLL;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        final long behandlingID = prosessinstans.getBehandling().getId();
        final Behandlingstema behandlingstema = prosessinstans.getBehandling().getTema();
        kontrollresultatService.utførKontrollerOgRegistrerFeil(behandlingID);

        Set<Kontrollresultat> kontrollresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID).getKontrollresultater();

        if (behandlingstema == Behandlingstema.BESLUTNING_LOVVALG_NORGE || !kontrollresultat.isEmpty()) {
            prosessinstans.setSteg(ProsessSteg.AFL_OPPRETT_OPPGAVE);
        } else {
            prosessinstans.setSteg(ProsessSteg.AFL_OPPDATER_MEDL);
        }
    }
}
