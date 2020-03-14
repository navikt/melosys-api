package no.nav.melosys.saksflyt.steg.iv;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.springframework.stereotype.Component;

@Component
public class SendGodkjentUtpekingSed extends AbstraktStegBehandler {

    private final EessiService eessiService;
    private final SaksopplysningerService saksopplysningerService;

    public SendGodkjentUtpekingSed(EessiService eessiService, SaksopplysningerService saksopplysningerService) {
        this.eessiService = eessiService;
        this.saksopplysningerService = saksopplysningerService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.IV_SEND_GODKJENT_UTPEKING;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {

        Behandling behandling = prosessinstans.getBehandling();
        if (!prosessinstans.getBehandling().norgeErUtpekt()) {
            throw new FunksjonellException("Kan ikke sende A012 på en behandling av type " + behandling.getType().getBeskrivelse());
        }

        SedDokument sedDokument = saksopplysningerService.hentSedOpplysninger(behandling.getId());

        if (sedDokument.getErElektronisk()) {
            eessiService.sendGodkjenningArbeidFlereLand(behandling.getId());
        } else {
            throw new UnsupportedOperationException("Sending av brev-A012 er ikke implementert");
        }

        prosessinstans.setSteg(ProsessSteg.IV_OPPRETT_AVGIFTSOPPGAVE);
    }
}
