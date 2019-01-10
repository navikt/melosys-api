package no.nav.melosys.saksflyt.agent.jfr;

import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessSteg.JFR_OPPDATER_JOURNALPOST;

/**
 * Ved mottak av nytt dokument på eksisterende sak.
 * <p>
 * Transisjoner:
 * 1) Saken har aktiv behandling (behandlingsstatus oppdateres til VURDER_DOKUMENT):
 * JFR_INNKOMMENDE_DOKUMENT → JFR_OPPDATER_JOURNALPOST eller FEILET_MASKINELT hvis feil
 * 2) Saken har ikke aktiv behandling og skal ikke behandles (behandlingstype null):
 * JFR_INNKOMMENDE_DOKUMENT → JFR_OPPDATER_JOURNALPOST eller FEILET_MASKINELT hvis feil
 * 3) Saken har ikke aktiv behandling og skal behandles:
 * JFR_INNKOMMENDE_DOKUMENT → JFR_AKTØR_ID eller FEILET_MASKINELT hvis feil
 */
@Component
public class InnkommendeDokument extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(InnkommendeDokument.class);

    private final FagsakRepository fagsakRepository;

    @Autowired
    public InnkommendeDokument(FagsakRepository fagsakRepository) {
        this.fagsakRepository = fagsakRepository;
        log.info("InnkommendeDokument initialisert");
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

        String saksnummer = prosessinstans.getData(ProsessDataKey.SAKSNUMMER);
        Behandlingstype nyBehandlingstype = prosessinstans.getData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstype.class);

        Fagsak fagsak = fagsakRepository.findBySaksnummer(saksnummer);
        if (fagsak == null) {
            String feilmelding = "Det finnes ingen fagsak med saksnummer " + saksnummer;
            log.error(feilmelding);
            håndterUnntak(Feilkategori.FUNKSJONELL_FEIL, prosessinstans, feilmelding, null);
            return;
        }

        Behandling aktivBehandling = fagsak.getAktivBehandling();
        if (aktivBehandling == null && nyBehandlingstype != null) {
            // Ny behandling trenges.
            prosessinstans.setType(ProsessType.JFR_NY_BEHANDLING);
            prosessinstans.setSteg(ProsessSteg.JFR_AKTØR_ID);
        } else {
            // Dokumentet journalføres direkte.
            prosessinstans.setSteg(JFR_OPPDATER_JOURNALPOST);
        }

        log.info("Prosessinstans {} har vurdert behov for behandling av dokument", prosessinstans.getId());
    }
}
