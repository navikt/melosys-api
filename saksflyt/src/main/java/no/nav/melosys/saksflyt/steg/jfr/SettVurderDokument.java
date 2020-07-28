package no.nav.melosys.saksflyt.steg.jfr;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Oppdaterer den aktive behandlingen (hvis tilstede) med status {@code VURDER_DOKUMENT} etter journalføring av nytt dokument på eksisterende sak.
 */
@Component
public class SettVurderDokument implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(SettVurderDokument.class);

    private final FagsakService fagsakService;

    private final BehandlingRepository behandlingRepository;

    @Autowired
    public SettVurderDokument(FagsakService fagsakService, BehandlingRepository behandlingRepository) {
        this.fagsakService = fagsakService;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.JFR_SETT_VURDER_DOKUMENT;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        String saksnummer = prosessinstans.getData(ProsessDataKey.SAKSNUMMER);

        Fagsak fagsak = fagsakService.hentFagsak(saksnummer);
        Behandling behandling = fagsak.hentAktivBehandling();
        boolean ingenVurdering = prosessinstans.getData(ProsessDataKey.JFR_INGEN_VURDERING, Boolean.class);
        if (behandling != null && !ingenVurdering) {
            behandling.setStatus(Behandlingsstatus.VURDER_DOKUMENT);
            behandlingRepository.save(behandling);
            log.info("Prosessinstans {} har endret status av behandling {} til {}", prosessinstans.getId(), behandling.getId(), Behandlingsstatus.VURDER_DOKUMENT);
        } else {
            log.info("I prosessinstans {}. Nytt dokument krever ingen vurdering ({}) eller ingen aktiv behandling for sak {}.", prosessinstans.getId(), ingenVurdering, saksnummer);
        }

        boolean skalTilordnes = prosessinstans.getData(ProsessDataKey.SKAL_TILORDNES, Boolean.class);
        if (skalTilordnes) {
            prosessinstans.setSteg(ProsessSteg.JFR_TILDEL_BEHANDLINGSOPPGAVE);
        } else {
            prosessinstans.setSteg(ProsessSteg.FERDIG);
        }
    }
}

