package no.nav.melosys.saksflyt.steg.aou.inn.svar;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.medl.KildedokumenttypeMedl;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.integrasjon.medl.StatusaarsakMedl;
import no.nav.melosys.saksflyt.felles.OppdaterMedlFelles;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("AnmodningUnntakMottakSvarOppdaterMedl")
public class OppdaterMedl extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(OppdaterMedl.class);

    private final OppdaterMedlFelles oppdaterMedlFelles;
    private final MedlFasade medlFasade;

    @Autowired
    public OppdaterMedl(OppdaterMedlFelles oppdaterMedlFelles, MedlFasade medlFasade) {
        this.oppdaterMedlFelles = oppdaterMedlFelles;
        this.medlFasade = medlFasade;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AOU_MOTTAK_SVAR_OPPDATER_MEDL;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        Behandling behandling = prosessinstans.getBehandling();

        Lovvalgsperiode lovvalgsperiode = oppdaterMedlFelles.hentLovvalgsperiode(behandling);
        if (lovvalgsperiode.getInnvilgelsesresultat() == InnvilgelsesResultat.INNVILGET) {
            medlFasade.oppdaterPeriodeEndelig(lovvalgsperiode, KildedokumenttypeMedl.SED);
        } else {
            medlFasade.avvisPeriode(lovvalgsperiode.getMedlPeriodeID(), StatusaarsakMedl.AVVIST);
        }

        log.info("Lovvalgsperiode for behandling {} satt til endelig i Medl", behandling.getId());
        prosessinstans.setSteg(ProsessSteg.AOU_MOTTAK_SVAR_SAK_OG_BEHANDLING_AVSLUTTET);
    }
}
