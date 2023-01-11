package no.nav.melosys.service.oppgave;

import java.time.LocalDate;
import java.util.List;

import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.oppgave.OppgaveFasade;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OppgaveSoekFilterTest {
    @Mock
    private OppgaveFasade oppgaveFasade;
    @Mock
    private JoarkFasade joarkFasade;
    @Mock
    private PersondataFasade persondataFasade;

    private OppgaveSoekFilter oppgaveSoekFilter;


    @BeforeEach
    public void setUp() {
        this.oppgaveSoekFilter = new OppgaveSoekFilter(
            oppgaveFasade,
            joarkFasade,
            persondataFasade);
    }

    @Test
    @DisplayName("finnBehandlingsoppgaverMedPersonIdent filtrerer oppgaver med journalpost og mottaksdato")
    void finnBehandlingsoppgaverMedPersonIdent() {
        String personIdent = "fnr";
        String journalpostID_1 = "JP_med_dato";
        String journalpostID_2 = "JP_uten_dato";

        Oppgave oppgave1 = new Oppgave.Builder().setJournalpostId(journalpostID_1).build();
        Oppgave oppgave2 = new Oppgave.Builder().setJournalpostId(journalpostID_2).build();
        Oppgave oppgave3 = new Oppgave.Builder().setJournalpostId(null).build();

        when(persondataFasade.hentAktørIdForIdent(personIdent)).thenReturn("aktørID");
        when(oppgaveFasade.finnOppgaverMedAktørId(eq("aktørID"), any())).thenReturn(List.of(oppgave1, oppgave2, oppgave3));
        when(joarkFasade.hentMottaksDatoForJournalpost(journalpostID_1)).thenReturn(LocalDate.EPOCH);


        List<Oppgave> oppgaver = oppgaveSoekFilter.finnBehandlingsoppgaverMedPersonIdent(personIdent);


        assertThat(oppgaver).contains(oppgave1);
    }

    @Test
    @DisplayName("finnBehandlingsoppgaverMedOrgnr filtrerer oppgaver med journalpost og mottaksdato")
    void finnBehandlingsoppgaverMedOrgnr() {
        String orgnr = "986";
        String journalpostID_1 = "JP_med_dato";
        String journalpostID_2 = "JP_uten_dato";

        Oppgave oppgave1 = new Oppgave.Builder().setJournalpostId(journalpostID_1).build();
        Oppgave oppgave2 = new Oppgave.Builder().setJournalpostId(journalpostID_2).build();
        Oppgave oppgave3 = new Oppgave.Builder().setJournalpostId(null).build();

        when(oppgaveFasade.finnOppgaverMedOrgnr(eq(orgnr), any())).thenReturn(List.of(oppgave1, oppgave2, oppgave3));
        when(joarkFasade.hentMottaksDatoForJournalpost(journalpostID_1)).thenReturn(LocalDate.EPOCH);


        List<Oppgave> oppgaver = oppgaveSoekFilter.finnBehandlingsoppgaverMedOrgnr(orgnr);


        assertThat(oppgaver).contains(oppgave1);
    }
}
