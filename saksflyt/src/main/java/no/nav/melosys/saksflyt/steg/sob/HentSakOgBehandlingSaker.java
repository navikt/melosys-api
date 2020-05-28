package no.nav.melosys.saksflyt.steg.sob;

import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerRequest;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.AKTØR_ID;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.HENT_SOB_SAKER;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.OPPFRISK_SAKSOPPLYSNINGER;

/**
 * Steget sørger for å hente saker fra SOB
 * <p>
 * Transisjoner:
 * HENT_SOB_SAKER → OPPFRISK_SAKSOPPLYSNINGER hvis alt ok
 * HENT_SOB_SAKER → FEILET_MASKINELT hvis oppslag mot SOB feilet
 */
@Component
public class HentSakOgBehandlingSaker extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(HentSakOgBehandlingSaker.class);

    private final RegisteropplysningerService registeropplysningerService;
    private final TpsFasade tpsFasade;

    @Autowired
    public HentSakOgBehandlingSaker(RegisteropplysningerService registeropplysningerService, TpsFasade tpsFasade) {
        this.registeropplysningerService = registeropplysningerService;
        this.tpsFasade = tpsFasade;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return HENT_SOB_SAKER;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        String aktørId = prosessinstans.getData(AKTØR_ID);
        String fnr = tpsFasade.hentIdentForAktørId(aktørId);

        registeropplysningerService.hentOgLagreOpplysninger(RegisteropplysningerRequest.builder()
            .saksopplysningTyper(RegisteropplysningerRequest.SaksopplysningTyper.builder()
                .sakOgBehandlingopplysninger().build())
            .behandlingID(prosessinstans.getBehandling().getId())
            .fnr(fnr)
            .build());

        prosessinstans.setSteg(OPPFRISK_SAKSOPPLYSNINGER);
        log.info("Hentet saker fra Sak og behandling for prosessinstans {}", prosessinstans.getId());
    }
}
