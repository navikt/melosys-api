// FIXME: Må flyttes ned til relevant pakke
package no.nav.melosys.saksflyt.agent;

import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.Oppgavetype;
import no.nav.melosys.domain.oppgave.PrioritetType;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.ProsessDataKey.*;
import static no.nav.melosys.domain.ProsessSteg.OPPRETT_OPPGAVE;

/**
 * Oppretter en oppgave i GSAK.
 *
 * Transisjoner:
 * OPPRETT_OPPGAVE -> null eller FEILET_MASKINELT hvis feil
 */
@Component
public class OpprettOppgave extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OpprettOppgave.class);

    private final GsakFasade gsakFasade;

    @Autowired
    public OpprettOppgave(@Qualifier("system")GsakFasade gsakFasade) {
        this.gsakFasade = gsakFasade;
        log.info("OpprettOppgave initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return OPPRETT_OPPGAVE;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Transactional
    @Override
    public void utfør(Prosessinstans prosessinstans) throws SikkerhetsbegrensningException, FunksjonellException, TekniskException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Boolean oppfriskSaksopplysning = prosessinstans.getData(ProsessDataKey.OPPFRISK_SAKSOPPLYSNING, Boolean.class);
        if (oppfriskSaksopplysning != null && oppfriskSaksopplysning) {
            prosessinstans.setSteg(null);
            log.info("Oppfrisking av saksopplysning er ferdig for prosessinstans {}", prosessinstans.getId());
            return;
        }

        BehandlingType behandlingType = prosessinstans.getBehandling().getType(); // Forutsetter at ingen tidligere steg har endret denne
        String gsakSakID = prosessinstans.getData(GSAK_SAK_ID);
        String aktørID = prosessinstans.getData(AKTØR_ID);
        String journalpostID = prosessinstans.getData(JOURNALPOST_ID);

        Oppgave oppgave = new Oppgave();

        oppgave.setGsakSaksnummer(gsakSakID);

        if (behandlingType == BehandlingType.SØKNAD) {
            oppgave.setTema(Tema.MED);
            oppgave.setOppgavetype(Oppgavetype.BEH_SAK);
            oppgave.setPrioritet(PrioritetType.NORM);
        } else if (behandlingType == BehandlingType.UNNTAK_MEDL) {
            oppgave.setTema(Tema.UFM);
            oppgave.setOppgavetype(Oppgavetype.BEH_SAK);
            oppgave.setPrioritet(PrioritetType.NORM);
        } else {
            String feilmelding = "behandlingType " + behandlingType + " er ikke støttet";
            log.error("{}: {}", prosessinstans.getId(), feilmelding);
            håndterUnntak(Feilkategori.FUNKSJONELL_FEIL, prosessinstans, feilmelding, null);
            return;
        }

        //builder.medUnderkategori() FIXME Venter. Ekisterer det i den nye GSAK tjenesten?
        oppgave.setAktørId(aktørID);
        oppgave.setJournalpostId(journalpostID);
        //builder.medMottattDato(); FIXME settes? Are T.
        //builder.medNormertBehandlingsTidInnen(); FIXME settes?
        // FIXME: MELOSYS-1401 skal støtte behandlingstype,behandlingstema,temagruppe
        String oppgaveId = gsakFasade.opprettOppgave(oppgave);

        prosessinstans.setSteg(null);
        log.info("Opprettet oppgave {} for prosessinstans {}", oppgaveId, prosessinstans.getId());
    }
}
