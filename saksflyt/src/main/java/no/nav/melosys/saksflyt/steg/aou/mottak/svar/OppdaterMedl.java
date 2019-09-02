package no.nav.melosys.saksflyt.steg.aou.mottak.svar;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.medl.KildedokumenttypeMedl;
import no.nav.melosys.integrasjon.medl.MedlFasade;
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
        medlFasade.oppdaterPeriodeEndelig(lovvalgsperiode, KildedokumenttypeMedl.SED);

        log.info("Lovvalgsperiode for behandling {} satt til endelig i Medl", behandling.getId());
        prosessinstans.setSteg(ProsessSteg.AOU_MOTTAK_SVAR_SAK_OG_BEHANDLING_AVSLUTTET);
    }
}
