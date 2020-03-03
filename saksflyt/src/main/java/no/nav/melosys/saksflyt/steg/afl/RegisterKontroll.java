package no.nav.melosys.saksflyt.steg.afl;

import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Kontrollresultat;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.KontrollresultatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("AFLRegisterKontroll")
public class RegisterKontroll extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(RegisterKontroll.class);

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
        final Behandlingstyper behandlingstype = prosessinstans.getBehandling().getType();
        kontrollresultatService.utførKontrollerOgRegistrerFeil(behandlingID);

        Set<Kontrollresultat> kontrollresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID).getKontrollresultater();
        log.info("Funnet kontroller etter registerkontroll: {}",
            kontrollresultat.stream().map(Kontrollresultat::getBegrunnelse).collect(Collectors.toList()));

        if (behandlingstype == Behandlingstyper.UTL_MYND_UTPEKT_NORGE || !kontrollresultat.isEmpty()) {
            prosessinstans.setSteg(ProsessSteg.AFL_OPPRETT_OPPGAVE);
        } else {
            prosessinstans.setSteg(ProsessSteg.AFL_OPPDATER_MEDL);
        }

    }
}
