package no.nav.melosys.saksflyt.steg.aou.inn.svar;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.InnvilgelsesResultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.medl.KildedokumenttypeMedl;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.integrasjon.medl.StatusaarsakMedl;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("AnmodningUnntakMottakSvarOppdaterMedl")
public class OppdaterMedl extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(OppdaterMedl.class);

    private final MedlPeriodeService medlPeriodeService;
    private final MedlFasade medlFasade;
    private final BehandlingService behandlingService;

    @Autowired
    public OppdaterMedl(MedlPeriodeService medlPeriodeService, MedlFasade medlFasade, BehandlingService behandlingService) {
        this.medlPeriodeService = medlPeriodeService;
        this.medlFasade = medlFasade;
        this.behandlingService = behandlingService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AOU_MOTTAK_SVAR_OPPDATER_MEDL;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        Behandling behandling = behandlingService.hentBehandling(prosessinstans.getBehandling().getId());

        Lovvalgsperiode lovvalgsperiode = medlPeriodeService.hentLovvalgsperiode(behandling);
        if (lovvalgsperiode.getInnvilgelsesresultat() == InnvilgelsesResultat.INNVILGET) {
            medlFasade.oppdaterPeriodeEndelig(lovvalgsperiode, KildedokumenttypeMedl.SED);
            log.info("Lovvalgsperiode for behandling {} satt til endelig i Medl", behandling.getId());
        } else {
            medlFasade.avvisPeriode(lovvalgsperiode.getMedlPeriodeID(), StatusaarsakMedl.AVVIST);
            log.info("Lovvalgsperiode for behandling {} satt til avvist i Medl", behandling.getId());
        }

        SedDokument sedDokument = SaksopplysningerUtils.hentSedDokument(behandling);
        if (sedDokument.getErElektronisk()) {
            prosessinstans.setSteg(ProsessSteg.AOU_MOTTAK_SVAR_SEND_SED);
        } else {
            prosessinstans.setSteg(ProsessSteg.AOU_MOTTAK_SVAR_OPPRETT_JOURNALPOST);
        }
    }
}
