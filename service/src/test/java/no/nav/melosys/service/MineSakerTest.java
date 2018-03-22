package no.nav.melosys.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import no.nav.melosys.aggregate.OppgaveAG;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingType;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Oppgave;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.FagsakRepository;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MineSakerTest {
    @Mock
    private OppgaveService oppgaveService;

    @Mock
    private FagsakRepository fagsakRepository;

    @Mock
    private GsakFasade gsakFasade;

    @Mock
    private BehandlingRepository behandlingRepository;

    @Test
    @Ignore
    public void henteMineSaker(){

        List<Oppgave> oppgaver = new ArrayList<>();
        Oppgave oppgave1 = mock(Oppgave.class);
        when(oppgave1.getSaksnummer()).thenReturn("1");
        oppgaver.add(oppgave1);
        when(gsakFasade.finnOppgaveListe(any(String.class),any(String.class),any(String.class),any(String.class),any(String.class),any(String.class))).thenReturn(oppgaver);

        Fagsak fagsak = mock(Fagsak.class);
        when(fagsak.getId()).thenReturn(12L);
        when(fagsakRepository.findByGsakSaksnummer(any(Long.class))).thenReturn(fagsak);


        SoeknadDokument soeknadDokument = mock(SoeknadDokument.class);
        soeknadDokument.fnr ="111111111111";


        Set<Saksopplysning> saksopplysninger = new HashSet<>();
        Saksopplysning saksopplysning = Mockito.spy(Saksopplysning.class);
        saksopplysning.setType(mock(SaksopplysningType.class));
        when(saksopplysning.getType().getBeskrivelse()).thenReturn("søkand");
        doReturn(soeknadDokument).when(saksopplysning).getDokument();

        saksopplysninger.add(saksopplysning);

        List<Behandling> behandlinger = new ArrayList<>();
        Behandling behandling = mock(Behandling.class);
        behandling.setSaksopplysninger(saksopplysninger);
        when(behandling.getStatus().getKode()).thenReturn("Opprettet");
        behandlinger.add(behandling);
        when(behandlingRepository.findBySaksnummer(any(Long.class))).thenReturn(behandlinger);

        List<OppgaveAG> mineSaker = oppgaveService.hentMineSaker(any(String.class));

        assertThat(mineSaker.get(0).getFagsak()).isEqualTo(12L);






    }
}
