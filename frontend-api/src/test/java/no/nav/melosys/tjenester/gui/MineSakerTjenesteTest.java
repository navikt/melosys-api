package no.nav.melosys.tjenester.gui;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.ws.rs.core.Response;

import no.nav.melosys.aggregate.OppgaveAggregate;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingStatus;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.FagsakStatus;
import no.nav.melosys.domain.FagsakType;
import no.nav.melosys.domain.Oppgave;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.service.OppgaveService;
import no.nav.melosys.service.Oppgaveplukker;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.sikkerhet.context.TestSubjectHandler;
import no.nav.melosys.tjenester.gui.dto.MinSakDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MineSakerTjenesteTest {

    OppgaveTjeneste tjeneste;
    @Mock
    private Oppgaveplukker oppgaveplukker;
    @Mock
    private OppgaveService oppgaveService;

    @Before
    public void setUp() {
        tjeneste = new OppgaveTjeneste(oppgaveplukker, oppgaveService);
        SpringSubjectHandler.set(new TestSubjectHandler());
    }

    @Test
    public void mineSaker() {

        OppgaveAggregate oppgaveAggregate = new OppgaveAggregate();
        List<OppgaveAggregate> oppgaver = new ArrayList<>();
        Oppgave oppgave1 = new Oppgave("1", "HOY_MED");
        oppgave1.setSaksnummer("11");
        oppgave1.setAnsvarligId("12345678901");
        oppgaveAggregate.setOppgave(oppgave1);

        Fagsak fagsak = new Fagsak();
        fagsak.setType(FagsakType.EU_EØS);
        fagsak.setStatus(FagsakStatus.OPPRETTET);
        List<Behandling> behandlinger = new ArrayList<>();
        Behandling behandling = new Behandling();
        behandling.setStatus(BehandlingStatus.UNDER_BEHANDLING);
        behandlinger.add(behandling); // Felleskodeverk finnes
        fagsak.setBehandlinger(behandlinger);
        oppgaveAggregate.setFagsak(fagsak);

        PersonDokument personDokument = new PersonDokument();
        personDokument.fnr = "111111111111";
        oppgaveAggregate.setPersonDokument(personDokument);

        SoeknadDokument soeknadDokument = new SoeknadDokument();
        soeknadDokument.fnr = "111111111111";
        soeknadDokument.arbeidUtland = new ArbeidUtland();
        soeknadDokument.arbeidUtland.arbeidsland = Arrays.asList(new Land(Land.NORGE));
        soeknadDokument.arbeidUtland.arbeidsperiode = new Periode(LocalDate.of(2018, 01, 21), LocalDate.of(2018, 05, 21));
        oppgaveAggregate.setSoeknadDokument(soeknadDokument);
        oppgaver.add(oppgaveAggregate);

        when(oppgaveService.hentMineSaker(anyString())).thenReturn(oppgaver);
        Response response = tjeneste.mineSaker();
        assertThat(response.getEntity()).isExactlyInstanceOf(ArrayList.class);
        List<MinSakDto> entity = (List<MinSakDto>) response.getEntity();
        assertThat(entity.get(0).getOppgaveId()).isEqualTo("1");
        assertThat(entity.get(0).getLand().get(0)).isEqualTo("NOR");

    }
}