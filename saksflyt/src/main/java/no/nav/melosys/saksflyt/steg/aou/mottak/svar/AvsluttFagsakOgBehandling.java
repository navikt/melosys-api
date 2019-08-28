package no.nav.melosys.saksflyt.steg.aou.mottak.svar;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.felles.FagsakOgBehandlingFelles;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("AnmodningUnntakMottakSvarAvsluttFagsakOgBehandling")
public class AvsluttFagsakOgBehandling extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(AvsluttFagsakOgBehandling.class);

    private final FagsakOgBehandlingFelles fagsakOgBehandlingFelles;

    @Autowired
    public AvsluttFagsakOgBehandling(FagsakOgBehandlingFelles fagsakOgBehandlingFelles) {
        this.fagsakOgBehandlingFelles = fagsakOgBehandlingFelles;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AOU_MOTTAK_SVAR_AVSLUTT_BEHANDLING;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        fagsakOgBehandlingFelles.avsluttFagsakOgBehandling(prosessinstans.getBehandling(), Saksstatuser.LOVVALG_AVKLART);
        prosessinstans.setSteg(ProsessSteg.AOU_MOTTAK_SVAR_SAK_OG_BEHANDLING_AVSLUTTET);
    }
}
