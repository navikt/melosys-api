package no.nav.melosys.saksflyt.steg.ufm;

import java.util.Collection;
import java.util.Collections;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("RegistreringUnntakOppdaterMedl")
public class OppdaterMedl implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OppdaterMedl.class);

    private final MedlPeriodeService medlPeriodeService;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final BehandlingService behandlingService;

    @Autowired
    public OppdaterMedl(MedlPeriodeService medlPeriodeService, LovvalgsperiodeService lovvalgsperiodeService, BehandlingService behandlingService) {
        this.medlPeriodeService = medlPeriodeService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.behandlingService = behandlingService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_OPPDATER_MEDL;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        final long behandlingId = prosessinstans.getBehandling().getId();
        Behandling behandling = behandlingService.hentBehandling(behandlingId);

        SedDokument sedDokument = SaksopplysningerUtils.hentSedDokument(behandling);

        Collection<Lovvalgsperiode> lovvalgsperioder = lovvalgsperiodeService.hentLovvalgsperioder(behandlingId);

        if (lovvalgsperioder.isEmpty()) {
            Collection<Lovvalgsperiode> lagretLovvalgsperiode = lovvalgsperiodeService.lagreLovvalgsperioder(
                behandlingId, Collections.singletonList(sedDokument.opprettInnvilgetLovvalgsperiode())
            );
            Lovvalgsperiode lovvalgsperiode = lagretLovvalgsperiode.iterator().next();
            opprettPeriode(behandling, lagretLovvalgsperiode, !lovvalgsperiode.erArtikkel13());
        } else if (lovvalgsperioder.iterator().next().getMedlPeriodeID() == null) {
            opprettPeriode(behandling, lovvalgsperioder, true);
        } else {
            Lovvalgsperiode lovvalgsperiode = lovvalgsperioder.iterator().next();
            medlPeriodeService.oppdaterPeriodeEndelig(lovvalgsperiode, true);
            log.info("Lovvalgsperiode for behandling {} satt til endelig i Medl", behandlingId);
        }

        prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_VARSLE_UTLAND);
    }

    private void opprettPeriode(Behandling behandling, Collection<Lovvalgsperiode> lovvalgsperioder, boolean erEndelig) throws TekniskException, FunksjonellException {
        String ident = SaksopplysningerUtils.hentPersonDokument(behandling).fnr;
        Lovvalgsperiode lovvalgsperiode = lovvalgsperioder.iterator().next();
        if (erEndelig) {
            medlPeriodeService.opprettPeriodeEndelig(lovvalgsperiode, behandling.getId(), true, ident);
        } else {
            medlPeriodeService.opprettPeriodeForeløpig(lovvalgsperiode, behandling.getId(), true, ident);
        }
    }
}
