package no.nav.melosys.service.oppgave;


import java.util.List;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.oppgave.OppgaveFasade;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.kodeverk.Oppgavetyper.*;

@Component
public class OppgaveSoek {
    private static final Logger log = LoggerFactory.getLogger(OppgaveSoek.class);

    private final OppgaveFasade oppgaveFasade;
    private final PersondataFasade persondataFasade;
    private final Unleash unleash;

    private static final String[] BEHANDLINGSOPPGAVE_TYPER = new String[]{
        BEH_SAK_MK.getKode(),
        BEH_SAK.getKode(),
        BEH_SED.getKode(),
    };

    private static final String[] BEHANDLINGSOPPGAVE_TYPER_UTVIDET = new String[]{
        BEH_SAK_MK.getKode(),
        BEH_SAK.getKode(),
        BEH_SED.getKode(),
        VUR.getKode(),
        VURD_HENV.getKode()
    };

    public OppgaveSoek(OppgaveFasade oppgaveFasade,
                       PersondataFasade persondataFasade,
                       Unleash unleash) {
        this.oppgaveFasade = oppgaveFasade;
        this.persondataFasade = persondataFasade;
        this.unleash = unleash;
    }

    public List<Oppgave> finnBehandlingsoppgaverMedPersonIdent(String personIdent) {
        String aktørId = persondataFasade.hentAktørIdForIdent(personIdent);

        if (aktørId == null) {
            throw new IkkeFunnetException("Finner ikke aktørId for ident " + personIdent);
        }
        if (unleash.isEnabled("melosys.behandle_alle_saker")) {
            return filtrerOppgaverMedJournalpost(oppgaveFasade.finnOppgaverMedAktørId(aktørId, BEHANDLINGSOPPGAVE_TYPER_UTVIDET));
        } else {
            return filtrerOppgaverMedJournalpost(oppgaveFasade.finnOppgaverMedAktørId(aktørId, BEHANDLINGSOPPGAVE_TYPER));
        }
    }

    public List<Oppgave> finnBehandlingsoppgaverMedOrgnr(String orgnr) {
        if (unleash.isEnabled("melosys.behandle_alle_saker")) {
            return filtrerOppgaverMedJournalpost(oppgaveFasade.finnOppgaverMedOrgnr(orgnr, BEHANDLINGSOPPGAVE_TYPER_UTVIDET));
        } else {
            return filtrerOppgaverMedJournalpost(oppgaveFasade.finnOppgaverMedOrgnr(orgnr, BEHANDLINGSOPPGAVE_TYPER));
        }
    }

    private List<Oppgave> filtrerOppgaverMedJournalpost(List<Oppgave> oppgaveListe) {
        return oppgaveListe.stream()
            .filter(oppgave -> oppgave.getJournalpostId() != null)
            .toList();
    }
}
