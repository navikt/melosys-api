package no.nav.melosys.saksflyt.steg.afl;

import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerFactory;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerRequest;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import org.springframework.stereotype.Component;

@Component("AFLHentRegisteropplysninger")
public class HentRegisteropplysninger extends AbstraktStegBehandler {

    private final RegisteropplysningerFactory registeropplysningerFactory;
    private final RegisteropplysningerService registeropplysningerService;

    public HentRegisteropplysninger(RegisteropplysningerFactory registeropplysningerFactory,
                                    RegisteropplysningerService registeropplysningerService) {
        this.registeropplysningerFactory = registeropplysningerFactory;
        this.registeropplysningerService = registeropplysningerService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AFL_HENT_REGISTEROPPLYSNINGER;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {

        MelosysEessiMelding melosysEessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);
        String fnr = prosessinstans.getData(ProsessDataKey.BRUKER_ID);

        registeropplysningerService.hentOgLagreOpplysninger(
            RegisteropplysningerRequest.builder()
                .behandlingID(prosessinstans.getBehandling().getId())
                .saksopplysningTyper(registeropplysningerFactory.hentSaksopplysningTyperForBeslutningOmLovvalg())
                .fom(melosysEessiMelding.getPeriode().getFom())
                .tom(melosysEessiMelding.getPeriode().getTom())
                .fnr(fnr)
                .build());

        prosessinstans.setSteg(ProsessSteg.AFL_OPPRETT_BEHANDLINGSGRUNNLAG);
    }
}
