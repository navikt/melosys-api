package no.nav.melosys.saksflyt.steg.jfr;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.GSAK_OPPRETT_OPPGAVE;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.REPLIKER_BEHANDLING;


/**
 * Ferdigstiller en journalpost i Joark.
 *
 * Transisjoner:
 *     REPLIKER_BEHANDLING -> EN NY FLOTT GREIE eller FEILET_MASKINELT hvis feil
 */
@Component
public class ReplikerBehandling extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(ReplikerBehandling.class);

    private FagsakService fagsakService;
    private BehandlingService behandlingService;

    @Autowired
    public ReplikerBehandling(FagsakService fagsakService, BehandlingService behandlingService) {
        this.fagsakService = fagsakService;
        this.behandlingService = behandlingService;
        log.info("ReplikerBehandling initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return REPLIKER_BEHANDLING;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        String saksnummer = prosessinstans.getData(ProsessDataKey.SAKSNUMMER);
        Fagsak fagsak = fagsakService.hentFagsak(saksnummer);

        Behandling behandling;

        if (fagsak.getTidligsteInaktiveBehandling() == null) {
            throw new FunksjonellException("Finner ingen avsluttet behandling på fagsak " + fagsak.getSaksnummer());
        }

        behandling = behandlingService.replikerBehandlingOgBehandlingsresultat(
            fagsak.getTidligsteInaktiveBehandling(),
            Behandlingsstatus.OPPRETTET,
            prosessinstans.getData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.class)
        );

        prosessinstans.setBehandling(behandling);

        fagsak.setStatus(Saksstatuser.OPPRETTET);
        fagsakService.lagre(fagsak);

        prosessinstans.setSteg(GSAK_OPPRETT_OPPGAVE);
        log.info("Prosessinstans {} har replikert behandling for {}", prosessinstans.getId(), saksnummer);
    }
}
