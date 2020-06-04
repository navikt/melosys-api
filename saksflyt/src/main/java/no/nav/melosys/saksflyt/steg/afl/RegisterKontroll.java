package no.nav.melosys.saksflyt.steg.afl;

import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsmaate;
import no.nav.melosys.domain.Kontrollresultat;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.kontroll.KontrollresultatService;
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
        final Behandling behandling = prosessinstans.getBehandling();
        final Behandlingstema behandlingstema = behandling.getTema();
        kontrollresultatService.utførKontrollerOgRegistrerFeil(behandling.getId());

        Set<Kontrollresultat> kontrollresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.getId()).getKontrollresultater();

        if (behandlingstema == Behandlingstema.BESLUTNING_LOVVALG_NORGE) {
            log.info("Norge er utpekt i behandling {}. Flyttet til manuell behandling.", behandling.getId());
            prosessinstans.setSteg(ProsessSteg.AFL_OPPRETT_OPPGAVE);
        } else if (!kontrollresultat.isEmpty()) {
            String registreringerStr = kontrollresultat.stream()
                .map(Kontrollresultat::getBegrunnelse).map(Kontroll_begrunnelser::getKode).collect(Collectors.joining(", "));
            log.info("Funnet treff {} for behandling {}. Flyttet til manuell behandling.", registreringerStr, behandling.getId());

            behandlingsresultatService.oppdaterBehandlingsMaate(behandling.getId(), Behandlingsmaate.DELVIS_AUTOMATISERT);
            prosessinstans.setSteg(ProsessSteg.AFL_OPPRETT_OPPGAVE);
        } else {
            log.info("Behandling {}, tema {} blir registrert automatisk", behandling.getId(), prosessinstans.getBehandling().getTema());
            behandlingsresultatService.oppdaterBehandlingsMaate(behandling.getId(), Behandlingsmaate.AUTOMATISERT);
            prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_OPPDATER_MEDL);
        }
    }
}
