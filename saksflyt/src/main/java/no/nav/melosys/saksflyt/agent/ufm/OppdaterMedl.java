package no.nav.melosys.saksflyt.agent.ufm;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.medl.KildedokumenttypeMedl;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.saksflyt.felles.OppdaterMedlFelles;
import no.nav.melosys.saksflyt.felles.UnntaksperiodeUtils;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.LovvalgsperiodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("RegistreringUnntakOppdaterMedl")
public class OppdaterMedl extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OppdaterMedl.class);

    private final MedlFasade medlFasade;
    private final OppdaterMedlFelles felles;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final BehandlingService behandlingService;

    @Autowired
    public OppdaterMedl(MedlFasade medlFasade, OppdaterMedlFelles felles, LovvalgsperiodeService lovvalgsperiodeService, BehandlingService behandlingService) {
        this.medlFasade = medlFasade;
        this.felles = felles;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.behandlingService = behandlingService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_OPPDATER_MEDL;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
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
                behandlingId, Collections.singletonList(UnntaksperiodeUtils.opprettLovvalgsperiode(sedDokument))
            );

            String ident = SaksopplysningerUtils.hentPersonDokument(behandling).fnr;

            Lovvalgsperiode lovvalgsperiode = lagretLovvalgsperiode.iterator().next();
            Long medlId = medlFasade.opprettPeriodeEndelig(ident, lovvalgsperiode, KildedokumenttypeMedl.SED);
            felles.lagreMedlPeriodeId(medlId, lovvalgsperiode, behandlingId);
            log.info("Lovvalgsperiode opprettet for behandling {} med medlId {}", behandlingId, medlId);
        } else {
            Lovvalgsperiode lovvalgsperiode = lovvalgsperioder.iterator().next();
            medlFasade.oppdaterPeriodeEndelig(lovvalgsperiode, KildedokumenttypeMedl.SED);
            log.info("Lovvalgsperiode for behandling {} satt til endelig i Medl", behandlingId);
        }

        prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_AVSLUTT_BEHANDLING);
    }
}
