package no.nav.melosys.saksflyt.steg.sed.ny_sak;

import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.felles.HentOpplysningerFelles;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import org.springframework.stereotype.Component;

@Component("sedGenerellBehandlingHentPersonOpplysninger")
public class HentPersonopplysninger extends AbstraktStegBehandler {

    private final HentOpplysningerFelles hentOpplysningerFelles;

    public HentPersonopplysninger(HentOpplysningerFelles hentOpplysningerFelles) {
        this.hentOpplysningerFelles = hentOpplysningerFelles;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.SED_GENERELL_SAK_HENT_PERSON;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        String aktørID = prosessinstans.getData(ProsessDataKey.AKTØR_ID);
        hentOpplysningerFelles.hentOgLagrePersonopplysninger(aktørID, prosessinstans.getBehandling());
        prosessinstans.setSteg(ProsessSteg.SED_GENERELL_SAK_OPPRETT_OPPGAVE);
    }
}
