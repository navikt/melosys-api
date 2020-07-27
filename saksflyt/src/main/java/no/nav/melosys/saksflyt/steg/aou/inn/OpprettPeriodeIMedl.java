package no.nav.melosys.saksflyt.steg.aou.inn;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OpprettPeriodeIMedl extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(OpprettPeriodeIMedl.class);

    private final BehandlingsresultatService behandlingsresultatService;
    private final MedlPeriodeService medlPeriodeService;

    @Autowired
    public OpprettPeriodeIMedl(BehandlingsresultatService behandlingsresultatService, MedlPeriodeService medlPeriodeService) {
        this.behandlingsresultatService = behandlingsresultatService;
        this.medlPeriodeService = medlPeriodeService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AOU_MOTTAK_OPPRETT_PERIODE_MEDL;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        Long behandlingID = prosessinstans.getBehandling().getId();

        Anmodningsperiode anmodningsperiode = behandlingsresultatService.hentBehandlingsresultat(behandlingID).hentValidertAnmodningsperiode();
        medlPeriodeService.opprettPeriodeUnderAvklaring(anmodningsperiode, behandlingID, true);

        prosessinstans.setSteg(ProsessSteg.AOU_MOTTAK_OPPRETT_OPPGAVE);
    }
}
