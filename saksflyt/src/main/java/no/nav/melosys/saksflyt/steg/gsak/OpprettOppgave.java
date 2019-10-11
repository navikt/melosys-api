package no.nav.melosys.saksflyt.steg.gsak;

import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.oppgave.Behandlingstema;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.PrioritetType;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessDataKey.AKTØR_ID;
import static no.nav.melosys.domain.ProsessDataKey.JOURNALPOST_ID;
import static no.nav.melosys.domain.ProsessSteg.GSAK_OPPRETT_OPPGAVE;
import static no.nav.melosys.domain.ProsessSteg.SEND_FORVALTNINGSMELDING;

/**
 * Oppretter en oppgave i GSAK.
 *
 * Transisjoner:
 * 1) ProsessType.JFR_NY_SAK eller Behandlingstyper.ENDRET_PERIODE:
 *      GSAK_OPPRETT_OPPGAVE -> SEND_FORVALTNINGSMELDING eller FEILET_MASKINELT hvis feil
 * 2) ProsessType.JFR_NY_BEHANDLING:
 *      GSAK_OPPRETT_OPPGAVE -> null eller FEILET_MASKINELT hvis feil
 */
@Component
public class OpprettOppgave extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(OpprettOppgave.class);

    private final BehandlingRepository behandlingRepository;
    private final GsakFasade gsakFasade;

    private static final String PID_MELDING = "{}: {}";
    private static final String STØTTES_IKKE = " er ikke støttet";

    @Autowired
    public OpprettOppgave(BehandlingRepository behandlingRepository, @Qualifier("system")GsakFasade gsakFasade) {
        this.behandlingRepository = behandlingRepository;
        this.gsakFasade = gsakFasade;
        log.info("OpprettOppgave initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return GSAK_OPPRETT_OPPGAVE;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws FunksjonellException, TekniskException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Long behandlingID = prosessinstans.getBehandling().getId();
        Behandling behandling = behandlingRepository.findById(behandlingID)
            .orElseThrow(() -> new FunksjonellException("Behandling " + behandlingID + " finnes ikke."));
        Behandlingstyper behandlingstype = behandling.getType();

        Fagsak fagsak = behandling.getFagsak();
        String saksnummer = fagsak.getSaksnummer();
        String aktørID = prosessinstans.getData(AKTØR_ID);
        String journalpostID = prosessinstans.getData(JOURNALPOST_ID);

        Oppgave.Builder oppgaveBuilder = new Oppgave.Builder();
        oppgaveBuilder.setTema(Tema.MED);

        if (fagsak.getType() == Sakstyper.EU_EOS) {
            oppgaveBuilder.setBehandlingstema(Behandlingstema.EU_EOS);
            oppgaveBuilder.setBehandlingstype(null);
        } else {
            String feilmelding = "Sakstyper " + fagsak.getType() + STØTTES_IKKE;
            log.error(PID_MELDING, prosessinstans.getId(), feilmelding);
            håndterUnntak(Feilkategori.FUNKSJONELL_FEIL, prosessinstans, feilmelding, null);
            return;
        }

        if (behandlingstype == Behandlingstyper.SOEKNAD) {
            oppgaveBuilder.setOppgavetype(Oppgavetyper.BEH_SAK_MK);
        } else if (behandlingstype == Behandlingstyper.ENDRET_PERIODE) {
            oppgaveBuilder.setOppgavetype(Oppgavetyper.VUR);
        } else {
            String feilmelding = "Behandlingstype " + behandlingstype + STØTTES_IKKE;
            log.error(PID_MELDING, prosessinstans.getId(), feilmelding);
            håndterUnntak(Feilkategori.FUNKSJONELL_FEIL, prosessinstans, feilmelding, null);
            return;
        }

        oppgaveBuilder.setAktørId(aktørID);
        oppgaveBuilder.setJournalpostId(journalpostID);
        oppgaveBuilder.setPrioritet(PrioritetType.NORM);
        oppgaveBuilder.setSaksnummer(saksnummer);

        boolean skalTilordnes = Optional.ofNullable(prosessinstans.getData(ProsessDataKey.SKAL_TILORDNES, Boolean.class)).orElse(false);
        if (skalTilordnes) {
            oppgaveBuilder.setTilordnetRessurs(prosessinstans.getData(ProsessDataKey.SAKSBEHANDLER));
        }

        String oppgaveId = gsakFasade.opprettOppgave(oppgaveBuilder.build());

        if (prosessinstans.getType() == ProsessType.JFR_NY_SAK) {
            boolean skalSendesForvaltningsmelding = Optional.ofNullable(prosessinstans.getData(ProsessDataKey.SKAL_SENDES_FORVALTNINGSMELDING, Boolean.class)).orElse(true);
            if (skalSendesForvaltningsmelding) {
                prosessinstans.setSteg(SEND_FORVALTNINGSMELDING);
            } else {
                prosessinstans.setSteg(ProsessSteg.FERDIG);
            }
        } else if (prosessinstans.getType() == ProsessType.JFR_NY_BEHANDLING) {
            prosessinstans.setSteg(ProsessSteg.FERDIG);
        } else {
            String feilmelding = "ProsessType " + prosessinstans.getType() + STØTTES_IKKE;
            log.error(PID_MELDING, prosessinstans.getId(), feilmelding);
            håndterUnntak(Feilkategori.TEKNISK_FEIL, prosessinstans, feilmelding, null);
            return;
        }
        log.info("Opprettet oppgave {} for prosessinstans {}", oppgaveId, prosessinstans.getId());
    }
}
