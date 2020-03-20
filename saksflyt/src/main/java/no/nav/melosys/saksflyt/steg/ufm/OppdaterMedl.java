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
import no.nav.melosys.integrasjon.medl.KildedokumenttypeMedl;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("RegistreringUnntakOppdaterMedl")
public class OppdaterMedl extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OppdaterMedl.class);

    private final MedlFasade medlFasade;
    private final MedlPeriodeService medlPeriodeService;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final BehandlingService behandlingService;

    @Autowired
    public OppdaterMedl(MedlFasade medlFasade, MedlPeriodeService medlPeriodeService, LovvalgsperiodeService lovvalgsperiodeService, BehandlingService behandlingService) {
        this.medlFasade = medlFasade;
        this.medlPeriodeService = medlPeriodeService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.behandlingService = behandlingService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_OPPDATER_MEDL;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
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
            Long medlId = opprettOgLagrePeriode(behandlingId, behandling, lagretLovvalgsperiode, !lovvalgsperiode.erArtikkel13());
            log.info("Lovvalgsperiode opprettet for behandling {} med medlId {}", behandlingId, medlId);
        } else if (lovvalgsperioder.iterator().next().getMedlPeriodeID() == null) {
            Long medlId = opprettOgLagrePeriode(behandlingId, behandling, lovvalgsperioder, true);
            log.info("Lovvalgsperiode for behandling {} opprettet og lagret i Medl med medlId {}", behandlingId, medlId);
        } else {
            Lovvalgsperiode lovvalgsperiode = lovvalgsperioder.iterator().next();
            medlFasade.oppdaterPeriodeEndelig(lovvalgsperiode, KildedokumenttypeMedl.SED);
            log.info("Lovvalgsperiode for behandling {} satt til endelig i Medl", behandlingId);
        }

        prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_AVSLUTT_BEHANDLING);
    }

    private Long opprettOgLagrePeriode(long behandlingId, Behandling behandling, Collection<Lovvalgsperiode> lovvalgsperioder, boolean erEndelig) throws TekniskException, FunksjonellException {
        String ident = SaksopplysningerUtils.hentPersonDokument(behandling).fnr;
        Lovvalgsperiode lovvalgsperiode = lovvalgsperioder.iterator().next();
        Long medlId = erEndelig
            ? medlFasade.opprettPeriodeEndelig(ident, lovvalgsperiode, KildedokumenttypeMedl.SED)
            : medlFasade.opprettPeriodeForeløpig(ident, lovvalgsperiode, KildedokumenttypeMedl.SED);
        medlPeriodeService.lagreMedlPeriodeId(medlId, lovvalgsperiode, behandlingId);

        return medlId;
    }
}
