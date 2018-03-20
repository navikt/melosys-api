package no.nav.melosys.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.Oppgave;
import no.nav.melosys.domain.gsak.Underkategori;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.repository.OppgaveTilbakeleggingRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OppgaveplukkerTest {

    @Mock
    private GsakFasade gsakFasade;

    @Mock
    private OppgaveTilbakeleggingRepository oppgaveTilbakkeleggingRepo;

    private Oppgaveplukker oppgaveplukker;

    @Before
    public void setUp() {
        this.oppgaveplukker = new Oppgaveplukker(gsakFasade, oppgaveTilbakkeleggingRepo);
    }

    @Test
    public void plukkOppgave() {
        List<Oppgave> oppgaver = new ArrayList<>();
        Oppgave oppgave1 = new Oppgave("1", "HOY_MED");
        oppgaver.add(oppgave1);
        Oppgave oppgave2 = new Oppgave("2", "HOY_MED");
        oppgaver.add(oppgave2);
        Oppgave oppgave3 = new Oppgave("3", "HOY_MED");
        oppgaver.add(oppgave3);

        when(gsakFasade.finnUtildelteOppgaverEtterFrist(anyList(), any(String.class), anyList())).thenReturn(oppgaver);

        String ident = "Z01234";

        List<String> fagområdeListe = new ArrayList<>();
        fagområdeListe.add("MED");
        fagområdeListe.add("UFM");

        List<String> sakstyper = new ArrayList<>();
        sakstyper.add(Underkategori.MIDL_LOVVALG_MED.toString());

        List<String> oppgavetypeListe = new ArrayList<>();
        oppgavetypeListe.add("");

        Optional<Oppgave> oppgave = oppgaveplukker.plukkOppgave(ident, sakstyper, oppgavetypeListe);

        assertThat(oppgave.isPresent()).isTrue();
        assertThat(oppgave.get().getOppgaveId()).isEqualTo("1");
    }
}