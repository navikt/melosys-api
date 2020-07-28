package no.nav.melosys.saksflyt.steg.hs;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.HS_HENLEGG_SAK;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.HS_SEND_BREV;

/**
 * Henlegger Sak
 *
 * Transisjoner:
 * HS_HENLEGG_SAK -> HS_SEND_BREV eller FEILET_MASKINELT hvis feil
 */
@Component
public class HenleggSak implements StegBehandler {
    private static final Logger log = LoggerFactory.getLogger(HenleggSak.class);

    private final FagsakService fagsakService;

    @Autowired
    public HenleggSak(FagsakService fagsakService) {
        this.fagsakService = fagsakService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return HS_HENLEGG_SAK;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Fagsak fagsak = fagsakService.hentFagsak(prosessinstans.getBehandling().getFagsak().getSaksnummer());
        fagsakService.avsluttFagsakOgBehandling(fagsak, Saksstatuser.HENLAGT);

        log.info("Satt sak til henlagt for prosessinstans {}", prosessinstans.getId());
        prosessinstans.setSteg(HS_SEND_BREV);
    }
}
