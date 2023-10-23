package no.nav.melosys.saksflyt.steg.sed;

import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.mottatteopplysninger.SedGrunnlag;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.ProsessSteg;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService;
import org.springframework.stereotype.Component;

@Component
public class OpprettSedGrunnlag implements StegBehandler {

    private final MottatteOpplysningerService mottatteOpplysningerService;
    private final EessiService eessiService;

    public OpprettSedGrunnlag(MottatteOpplysningerService mottatteOpplysningerService, EessiService eessiService) {
        this.mottatteOpplysningerService = mottatteOpplysningerService;
        this.eessiService = eessiService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.OPPRETT_SED_GRUNNLAG;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        MelosysEessiMelding melosysEessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);
        SedGrunnlag sedGrunnlag = eessiService.hentSedGrunnlag(melosysEessiMelding.getRinaSaksnummer(), melosysEessiMelding.getSedId());
        mottatteOpplysningerService.opprettSedGrunnlag(prosessinstans.getBehandling().getId(), sedGrunnlag);
    }
}
