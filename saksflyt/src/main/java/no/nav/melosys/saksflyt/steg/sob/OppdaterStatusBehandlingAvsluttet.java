package no.nav.melosys.saksflyt.steg.sob;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.sob.SobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.IV_STATUS_BEH_AVSL;

/**
 * Steget sørger for å skrive til Sak og Behandling når en behandling avsluttes
 *
 * Transisjoner:
 * IV_STATUS_BEH_AVSL → null hvis alt ok
 * IV_STATUS_BEH_AVSL → FEILET_MASKINELT hvis oppdatering av status feilet
 */
@Component
public class OppdaterStatusBehandlingAvsluttet implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OppdaterStatusBehandlingAvsluttet.class);

    private final SobService sobService;

    @Autowired
    public OppdaterStatusBehandlingAvsluttet(SobService sobService) {
        this.sobService = sobService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return IV_STATUS_BEH_AVSL;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws FunksjonellException, TekniskException {
        Behandling behandling = prosessinstans.getBehandling();

        Fagsak fagsak = behandling.getFagsak();
        String saksnummer = fagsak.getSaksnummer();

        Aktoer aktør = fagsak.hentBruker();
        String aktørID = null;
        if (aktør != null) {
            aktørID = aktør.getAktørId();
        }

        if (aktør == null || aktørID == null) {
            throw new FunksjonellException("Sak " + saksnummer + " har ingen bruker." );
        }

        sobService.sakOgBehandlingAvsluttet(saksnummer, behandling.getId(), aktørID);
        log.info("Oppdatert sob-status til avsluttet for prosessinstans {}", prosessinstans.getId());
    }
}
