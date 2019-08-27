package no.nav.melosys.saksflyt.steg.ufm;

import java.util.Map;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.saksflyt.felles.HentOpplysningerFelles;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.steg.UnntakBehandler;
import no.nav.melosys.saksflyt.steg.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("UnntakFraMedlemskapHentPersonopplysninger")
public class HentPersonopplysninger extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(HentPersonopplysninger.class);

    private final HentOpplysningerFelles hentOpplysningerFelles;

    @Autowired
    public HentPersonopplysninger(HentOpplysningerFelles hentOpplysningerFelles) {
        this.hentOpplysningerFelles = hentOpplysningerFelles;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_HENT_PERSON;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        String aktørId = prosessinstans.getData(ProsessDataKey.AKTØR_ID);
        String ident = hentOpplysningerFelles.hentOgLagrePersonopplysninger(aktørId, prosessinstans.getBehandling());
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, ident);

        prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_HENT_MEDLEMSKAP);
    }
}
