package no.nav.melosys.tjenester.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.core.Response;

import no.nav.melosys.domain.Oppgave;
import no.nav.melosys.domain.gsak.Oppgavetype;
import no.nav.melosys.domain.gsak.Underkategori;
import no.nav.melosys.service.Oppgaveplukker;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.tjenester.gui.dto.PlukkOppgaveInnDto;
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
public class OppgaveTjenesteTest {

    @Mock
    private Oppgaveplukker oppgaveplukker;

    @Mock
    private SpringSubjectHandler springSubjectHandler;
    
    private OppgaveTjeneste tjeneste;

    @Before
    public void setUp() {
        tjeneste = new OppgaveTjeneste(oppgaveplukker);
    }

    @Test
    public void plukkOppgave() {
        PlukkOppgaveInnDto innData = new PlukkOppgaveInnDto();

        List<String> fagområdeKodeListe = new ArrayList<>();
        fagområdeKodeListe.add("MED");
        fagområdeKodeListe.add("UFM");
        innData.setFagområdeKodeListe(fagområdeKodeListe);

        innData.setUnderkategori(Underkategori.BOSTED_MED.toString());

        List<String> oppgavetypeListe = new ArrayList<>();
        oppgavetypeListe.add(Oppgavetype.JFR_MED.toString());
        innData.setOppgavetypeListe(oppgavetypeListe);

        Oppgave oppgave = new Oppgave();
        oppgave.setOppgaveId("1");
        Optional<Oppgave> plukket = Optional.of(oppgave);

        when(oppgaveplukker.plukkOppgave(any(String.class), anyList(), any(String.class), anyList())).thenReturn(plukket);

        Response response = tjeneste.plukkOppgave(innData);

        assertThat(response.getEntity()).isExactlyInstanceOf(Oppgave.class);
        response.getEntity().g


    }
}