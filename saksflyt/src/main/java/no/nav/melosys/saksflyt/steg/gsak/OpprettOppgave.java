package no.nav.melosys.saksflyt.steg.gsak;

import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.PrioritetType;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.oppgave.OppgaveFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.*;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.GSAK_OPPRETT_OPPGAVE;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.SEND_FORVALTNINGSMELDING;

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

    private final BehandlingService behandlingService;
    private final GsakFasade gsakFasade;

    private static final String PID_MELDING = "{}: {}";
    private static final String STØTTES_IKKE = " er ikke støttet";

    @Autowired
    public OpprettOppgave(BehandlingService behandlingService, @Qualifier("system")GsakFasade gsakFasade) {
        this.behandlingService = behandlingService;
        this.gsakFasade = gsakFasade;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return GSAK_OPPRETT_OPPGAVE;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws FunksjonellException, TekniskException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Long behandlingID = prosessinstans.getBehandling().getId();
        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        Behandlingstyper behandlingstype = behandling.getType();

        Fagsak fagsak = behandling.getFagsak();
        String saksnummer = fagsak.getSaksnummer();
        String aktørID = prosessinstans.getData(AKTØR_ID);
        String journalpostID = prosessinstans.getData(JOURNALPOST_ID);

        validerSakstype(fagsak.getType());
        validerBehandlingstype(behandlingstype);

        boolean skalTilordnes = Optional.ofNullable(prosessinstans.getData(ProsessDataKey.SKAL_TILORDNES, Boolean.class)).orElse(false);

        Oppgave.Builder oppgaveBuilder = OppgaveFactory.lagBehandlingsOppgaveForType(behandlingstype)
            .setAktørId(aktørID)
            .setJournalpostId(journalpostID)
            .setPrioritet(PrioritetType.NORM)
            .setSaksnummer(saksnummer)
            .setTilordnetRessurs(skalTilordnes ? prosessinstans.getData(SAKSBEHANDLER) : null);

        String oppgaveId = gsakFasade.opprettOppgave(oppgaveBuilder.build());

        if (prosessinstans.getType() == ProsessType.JFR_NY_SAK) {
            boolean skalSendesForvaltningsmelding = Optional.ofNullable(prosessinstans.getData(ProsessDataKey.SKAL_SENDES_FORVALTNINGSMELDING, Boolean.class)).orElse(true);
            if (skalSendesForvaltningsmelding) {
                prosessinstans.setSteg(SEND_FORVALTNINGSMELDING);
            } else {
                prosessinstans.setSteg(ProsessSteg.FERDIG);
            }
        } else if (prosessinstans.getType() == ProsessType.JFR_NY_BEHANDLING || prosessinstans.getType() == ProsessType.OPPRETT_NY_SAK) {
            prosessinstans.setSteg(ProsessSteg.FERDIG);
        } else {
            String feilmelding = "ProsessType " + prosessinstans.getType() + STØTTES_IKKE;
            log.error(PID_MELDING, prosessinstans.getId(), feilmelding);
            håndterUnntak(Feilkategori.TEKNISK_FEIL, prosessinstans, feilmelding, null);
            return;
        }
        log.info("Opprettet oppgave {} for prosessinstans {}", oppgaveId, prosessinstans.getId());
    }

    private void validerBehandlingstype(Behandlingstyper behandlingstype) throws FunksjonellException {
        if (behandlingstype != Behandlingstyper.SOEKNAD
            && behandlingstype != Behandlingstyper.SOEKNAD_IKKE_YRKESAKTIV
            && behandlingstype != Behandlingstyper.ENDRET_PERIODE) {
            throw new FunksjonellException("Behandlingstype " + behandlingstype + STØTTES_IKKE);
        }
    }

    private void validerSakstype(Sakstyper sakstype) throws FunksjonellException {
        if (sakstype != Sakstyper.EU_EOS) {
            throw new FunksjonellException("Sakstyper " + sakstype + STØTTES_IKKE);
        }
    }
}
