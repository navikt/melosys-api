package no.nav.melosys.saksflyt.steg.sed.ny_sak;

import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerRequest;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import org.springframework.stereotype.Component;

@Component("sedGenerellBehandlingHentPersonOpplysninger")
public class HentPersonopplysninger extends AbstraktStegBehandler {

    private final RegisteropplysningerService registeropplysningerService;
    private final TpsFasade tpsFasade;

    public HentPersonopplysninger(RegisteropplysningerService registeropplysningerService, TpsFasade tpsFasade) {
        this.registeropplysningerService = registeropplysningerService;
        this.tpsFasade = tpsFasade;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.SED_GENERELL_SAK_HENT_PERSON;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        String aktørID = prosessinstans.getData(ProsessDataKey.AKTØR_ID);
        String fnr = tpsFasade.hentIdentForAktørId(aktørID);

        registeropplysningerService.hentOgLagreOpplysninger(
            RegisteropplysningerRequest.builder()
                .behandlingID(prosessinstans.getBehandling().getId())
                .saksopplysningTyper(RegisteropplysningerRequest.SaksopplysningTyper.builder()
                    .personopplysninger()
                    .build())
                .fnr(fnr)
                .build()
        );

        prosessinstans.setSteg(ProsessSteg.SED_GENERELL_SAK_OPPRETT_OPPGAVE);
    }
}
