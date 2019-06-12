package no.nav.melosys.saksflyt.agent.ufm;

import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.kodeverk.Avklartefaktatype;
import no.nav.melosys.domain.kodeverk.Unntak_periode_begrunnelser;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RegistreringUnntakValiderer extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(RegistreringUnntakValiderer.class);

    private final AvklartefaktaService avklartefaktaService;

    RegistreringUnntakValiderer(AvklartefaktaService avklartefaktaService) {
        this.avklartefaktaService = avklartefaktaService;
    }

    void registrerFeil(Prosessinstans prosessinstans, Unntak_periode_begrunnelser treffBegrunnelse) throws IkkeFunnetException {
        long behandlingsId = prosessinstans.getBehandling().getId();

        if (!avklartefaktaService.hentVurderingUnntakPeriode(behandlingsId).isPresent()) {
            avklartefaktaService.leggTilAvklarteFakta(prosessinstans.getBehandling().getId(),
                Avklartefaktatype.VURDERING_UNNTAK_PERIODE, Avklartefaktatype.VURDERING_UNNTAK_PERIODE.name(), null, "TRUE");
        }

        log.info("Treff ved validering av periode for behandling {}. Treffbegrunnelse: {}", behandlingsId, treffBegrunnelse);
        avklartefaktaService.leggTilRegistrering(behandlingsId, Avklartefaktatype.VURDERING_UNNTAK_PERIODE, treffBegrunnelse.getKode());
    }
}
