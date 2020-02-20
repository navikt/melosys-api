package no.nav.melosys.saksflyt.steg.ufm;

import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandlingsmaate;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Kontrollresultat;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.BehandlingsresultatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BestemBehandlingsMaate extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(BestemBehandlingsMaate.class);

    private final BehandlingsresultatService behandlingsresultatService;

    @Autowired
    public BestemBehandlingsMaate(BehandlingsresultatService behandlingsresultatService) {
        this.behandlingsresultatService = behandlingsresultatService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_BESTEM_BEHANDLINGSMAATE;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(prosessinstans.getBehandling().getId());

        Set<Kontrollresultat> kontrollresultater = behandlingsresultat.getKontrollresultater();
        if (kontrollresultater.isEmpty()) {
            behandlingsresultat.setBehandlingsmåte(Behandlingsmaate.AUTOMATISERT);
            log.info("Behandling {}, type {} blir registrer automatisk",
                prosessinstans.getBehandling().getId(), prosessinstans.getBehandling().getType());
            prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_OPPDATER_MEDL);
        } else {
            String registreringerStr = kontrollresultater.stream()
                .map(Kontrollresultat::getBegrunnelse).map(Kontroll_begrunnelser::getKode).collect(Collectors.joining(", "));
            log.info("Funnet treff {} for behandling {}. Flyttet til manuell behandling.",
                registreringerStr, prosessinstans.getBehandling().getId());

            behandlingsresultat.setBehandlingsmåte(Behandlingsmaate.DELVIS_AUTOMATISERT);
            prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_OPPRETT_OPPGAVE);
        }

        behandlingsresultatService.lagre(behandlingsresultat);
    }
}
