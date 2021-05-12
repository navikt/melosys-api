package no.nav.melosys.saksflyt.steg.sed;

import java.util.Collections;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OpprettAnmodningsperiodeFraSed implements StegBehandler {
    private static final Logger log = LoggerFactory.getLogger(OpprettAnmodningsperiodeFraSed.class);

    private final AnmodningsperiodeService anmodningsperiodeService;
    private final BehandlingService behandlingService;

    @Autowired
    public OpprettAnmodningsperiodeFraSed(AnmodningsperiodeService anmodningsperiodeService,
                                          BehandlingService behandlingService) {
        this.anmodningsperiodeService = anmodningsperiodeService;
        this.behandlingService = behandlingService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.OPPRETT_ANMODNINGSPERIODE_FRA_SED;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        Behandling behandling = behandlingService.hentBehandling(prosessinstans.getBehandling().getId());
        SedDokument sedDokument = behandling.hentSedDokument();
        Anmodningsperiode anmodningsperiode = lagAnmodningsperiode(sedDokument);
        anmodningsperiodeService.lagreAnmodningsperioder(prosessinstans.getBehandling().getId(), Collections.singletonList(anmodningsperiode));
        log.info("Opprettet anmodningsperiode for behandling {}", behandling.getId());
    }

    private static Anmodningsperiode lagAnmodningsperiode(SedDokument sedDokument) {
        return new Anmodningsperiode(
            sedDokument.getLovvalgsperiode().getFom(),
            sedDokument.getLovvalgsperiode().getTom(),
            sedDokument.getLovvalgslandKode(),
            sedDokument.getLovvalgBestemmelse(),
            null,
            sedDokument.getUnntakFraLovvalgslandKode(),
            sedDokument.getUnntakFraLovvalgBestemmelse(),
            Trygdedekninger.UTEN_DEKNING
        );
    }
}
