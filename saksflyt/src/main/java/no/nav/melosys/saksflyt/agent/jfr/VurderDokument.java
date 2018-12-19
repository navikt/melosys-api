package no.nav.melosys.saksflyt.agent.jfr;

import java.util.Map;

import no.nav.melosys.domain.*;
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
 * Oppretter eller oppdaterer behandlingsoppgave i Melosys ved mottak av nytt dokument.
 * https://confluence.adeo.no/pages/viewpage.action?pageId=264973205
 * <p>
 * Transisjoner:
 * 1) Saken har aktiv behandling (behandlingsstatus oppdateres til VURDER_DOKUMENT):
 * JFR_INNKOMMENDE_DOKUMENT → null eller FEILET_MASKINELT hvis feil
 * 2) Saken har ikke aktiv behandling og skal ikke behandles (behandlingstype null):
 * JFR_INNKOMMENDE_DOKUMENT → null eller FEILET_MASKINELT hvis feil *
 * 3) Saken har ikke aktiv behandling og skal behandles:
 * JFR_INNKOMMENDE_DOKUMENT → JFR_AKTØR_ID eller FEILET_MASKINELT hvis feil
 */
@Component
public class VurderDokument extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(VurderDokument.class);

    private final FagsakRepository fagsakRepository;

    private final BehandlingRepository behandlingRepository;

    @Autowired
    public VurderDokument(FagsakRepository fagsakRepository, BehandlingRepository behandlingRepository) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        log.info("OppdaterBehandlingsoppgave initialisert");
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.JFR_INNKOMMENDE_DOKUMENT;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Behandlingstype behandlingstype = prosessinstans.getData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstype.class);
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
            prosessinstans.setSteg(null);
        } else {
            if (behandlingstype == null) {
                prosessinstans.setSteg(null);
            } else {
                // Ny behandling trenges.
                prosessinstans.setSteg(ProsessSteg.JFR_AKTØR_ID);
            }
        }

        log.info("Prosessinstans {} har vurdert behov for behandling av dokument", prosessinstans.getId());
    }
}
