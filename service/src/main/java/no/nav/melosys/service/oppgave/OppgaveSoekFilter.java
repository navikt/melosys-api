package no.nav.melosys.service.oppgave;


import java.util.List;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.oppgave.OppgaveFasade;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.kodeverk.Oppgavetyper.*;

@Component
public class OppgaveSoekFilter {
    private final OppgaveFasade oppgaveFasade;
    private final JoarkFasade joarkFasade;
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

    public OppgaveSoekFilter(OppgaveFasade oppgaveFasade,
                             JoarkFasade joarkFasade,
                             PersondataFasade persondataFasade,
                             Unleash unleash) {
        this.oppgaveFasade = oppgaveFasade;
        this.joarkFasade = joarkFasade;
        this.persondataFasade = persondataFasade;
        this.unleash = unleash;
    }

    public List<Oppgave> finnBehandlingsoppgaverMedPersonIdent(String personIdent) {
        String aktørId = persondataFasade.hentAktørIdForIdent(personIdent);

        if (aktørId == null) {
            throw new IkkeFunnetException("Finner ikke aktørId for ident " + personIdent);
        }
        if (unleash.isEnabled("melosys.behandle_alle_saker")) {
            return filtrerMedJournalpostOgMottaksdato(oppgaveFasade.finnOppgaverMedAktørId(aktørId, BEHANDLINGSOPPGAVE_TYPER_UTVIDET));
        } else {
            return filtrerMedJournalpostOgMottaksdato(oppgaveFasade.finnOppgaverMedAktørId(aktørId, BEHANDLINGSOPPGAVE_TYPER));
        }
    }

    public List<Oppgave> finnBehandlingsoppgaverMedOrgnr(String orgnr) {
        if (unleash.isEnabled("melosys.behandle_alle_saker")) {
            return filtrerMedJournalpostOgMottaksdato(oppgaveFasade.finnOppgaverMedOrgnr(orgnr, BEHANDLINGSOPPGAVE_TYPER_UTVIDET));
        } else {
            return filtrerMedJournalpostOgMottaksdato(oppgaveFasade.finnOppgaverMedOrgnr(orgnr, BEHANDLINGSOPPGAVE_TYPER));
        }
    }

    private List<Oppgave> filtrerMedJournalpostOgMottaksdato(List<Oppgave> oppgaveListe) {
        return oppgaveListe.stream()
            .filter(oppgave -> oppgave.getJournalpostId() != null)
            .filter(oppgave -> joarkFasade.hentMottaksDatoForJournalpost(oppgave.getJournalpostId()) != null)
            .toList();
    }
}
