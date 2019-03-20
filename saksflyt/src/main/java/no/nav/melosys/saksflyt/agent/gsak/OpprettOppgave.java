package no.nav.melosys.saksflyt.agent.gsak;

import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.PrioritetType;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
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
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws FunksjonellException, TekniskException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Behandling behandling = behandlingRepository.findById(prosessinstans.getBehandling().getId()).orElse(null);
        Behandlingstyper behandlingstype = behandling.getType();

        Fagsak fagsak = behandling.getFagsak();
        String saksnummer = fagsak.getSaksnummer();
        String aktørID = prosessinstans.getData(AKTØR_ID);
        String journalpostID = prosessinstans.getData(JOURNALPOST_ID);

        Oppgave oppgave = new Oppgave();
        oppgave.setTema(Tema.MED);

        if (fagsak.getType() == Sakstyper.EU_EOS) {
            // FIXME MELOSYS-1401 Behandlingstema.ARB_EØS bør brukes men mappingen med underkategori er ikke definert og registrert.
            oppgave.setBehandlingstema(null);
        } else {
            String feilmelding = "Sakstyper " + fagsak.getType() + " er ikke støttet";
            log.error("{}: {}", prosessinstans.getId(), feilmelding);
            håndterUnntak(Feilkategori.FUNKSJONELL_FEIL, prosessinstans, feilmelding, null);
            return;
        }

        if (behandlingstype == Behandlingstyper.SOEKNAD || behandlingstype == Behandlingstyper.ENDRET_PERIODE) {
            oppgave.setBehandlingstype(behandlingstype);
            oppgave.setOppgavetype(Oppgavetyper.BEH_SAK_MK);
        } else {
            String feilmelding = "Behandlingstype " + behandlingstype + " er ikke støttet";
            log.error("{}: {}", prosessinstans.getId(), feilmelding);
            håndterUnntak(Feilkategori.FUNKSJONELL_FEIL, prosessinstans, feilmelding, null);
            return;
        }

        oppgave.setAktørId(aktørID);
        oppgave.setJournalpostId(journalpostID);
        oppgave.setPrioritet(PrioritetType.NORM);
        oppgave.setSaksnummer(saksnummer);

        String oppgaveId = gsakFasade.opprettOppgave(oppgave);

        if (prosessinstans.getType() == ProsessType.JFR_NY_SAK) {
            prosessinstans.setSteg(SEND_FORVALTNINGSMELDING);
        } else if (prosessinstans.getType() == ProsessType.JFR_NY_BEHANDLING) {
            prosessinstans.setSteg(null);
        } else {
            String feilmelding = "ProsessType " + prosessinstans.getType() + " er ikke støttet";
            log.error("{}: {}", prosessinstans.getId(), feilmelding);
            håndterUnntak(Feilkategori.TEKNISK_FEIL, prosessinstans, feilmelding, null);
            return;
        }
        log.info("Opprettet oppgave {} for prosessinstans {}", oppgaveId, prosessinstans.getId());
    }
}
