package no.nav.melosys.saksflyt.agent.jfr;

import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Oppdaterer den aktive behandlingen (hvis tilstede) med status {@code VURDER_DOKUMENT} etter journalføring av nytt dokument på eksisterende sak.
 */
@Component
public class SettVurderDokument extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(SettVurderDokument.class);

    private final FagsakRepository fagsakRepository;

    private final BehandlingRepository behandlingRepository;

    @Autowired
    public SettVurderDokument(FagsakRepository fagsakRepository, BehandlingRepository behandlingRepository) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        log.info("SettVurderDokument initialisert");
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.JFR_SETT_VURDER_DOKUMENT;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        String saksnummer = prosessinstans.getData(ProsessDataKey.SAKSNUMMER);

        Fagsak fagsak = fagsakRepository.findBySaksnummer(saksnummer);
        if (fagsak == null) {
            String feilmelding = "Det finnes ingen fagsak med saksnummer " + saksnummer;
            log.error(feilmelding);
            håndterUnntak(Feilkategori.FUNKSJONELL_FEIL, prosessinstans, feilmelding, null);
            return;
        }

        Behandling behandling = fagsak.getAktivBehandling();
        if (behandling != null) {
            behandling.setStatus(Behandlingsstatus.VURDER_DOKUMENT);
            behandlingRepository.save(behandling);
            log.info("Prosessinstans {} har endret status av behandling {} til {}", prosessinstans.getId(), behandling.getId(), Behandlingsstatus.VURDER_DOKUMENT);
        } else {
            log.debug("Prosessinstans {}: ingen endring for sak {}", prosessinstans.getId(), saksnummer);
        }

        prosessinstans.setSteg(null);
    }
}

