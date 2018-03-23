package no.nav.melosys.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import no.nav.melosys.aggregate.OppgaveAG;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingStatus;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Oppgave;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.FagsakRepository;
import org.junit.Ignore;
import no.nav.melosys.repository.SaksopplysningRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MineSakerTest {

    private OppgaveService oppgaveService;

    @Mock
    private FagsakRepository fagsakRepository;

    @Mock
    private GsakFasade gsakFasade;

    @Mock
    private BehandlingRepository behandlingRepository;

    @Before
    public void setUp() {
        this.oppgaveService = new OppgaveService(gsakFasade,
                fagsakRepository,
                mock(SaksopplysningRepository.class),
                mock(SoeknadService.class),
                behandlingRepository);

    }

    @Test
    @Ignore
    public void henteMineSaker(){

        List<Oppgave> oppgaver = new ArrayList<>();
        Oppgave oppgave1 = new Oppgave("1", "HOY_MED");
        oppgave1.setSaksnummer("11");
        oppgave1.setAnsvarligId("12345678901");
        oppgaver.add(oppgave1);

        when(gsakFasade.finnOppgaveListe(anyString(), anyString(), anyString(), anyString(),anyString(), anyString())).
                thenAnswer((Answer) invocation -> {
                    String string = invocation.getArgument(0);
                    return (string.equals("12345678901")) ? oppgaver : new ArrayList<>();
                });

        Fagsak fagsak = new Fagsak();
        when(fagsakRepository.findByGsakSaksnummer(any(Long.class))).thenReturn(fagsak);

        List<Behandling> behandlinger = getBehandlings();
        when(behandlingRepository.findBySaksnummer(any(Long.class))).thenReturn(behandlinger).getMock();

        List<OppgaveAG> mineSaker = oppgaveService.hentMineSaker("12345678901");
        assertThat(mineSaker.size()).isEqualTo(1);
        assertThat(mineSaker.get(0).getOppgave().getOppgaveId()).isEqualTo("1");

        mineSaker = oppgaveService.hentMineSaker("12346678902");
        assertThat(mineSaker.size()).isEqualTo(0);

    }

    private List<Behandling> getBehandlings() {
        Set<Saksopplysning> saksopplysninger = new HashSet<>();
        PersonDokument personDokument = new PersonDokument();
        personDokument.fnr = "111111111111";

        Saksopplysning personOpplysning = new Saksopplysning();
        personOpplysning.setType(SaksopplysningType.PERSONOPPLYSNING);
        personOpplysning.setDokument(personDokument);
        saksopplysninger.add(personOpplysning);

        SoeknadDokument soeknadDokument = new SoeknadDokument();
        soeknadDokument.fnr = "111111111111";

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.SØKNAD);
        saksopplysning.setDokument(soeknadDokument);
        saksopplysninger.add(saksopplysning);

        List<Behandling> behandlinger = new ArrayList<>();
        Behandling behandling = new Behandling();
        behandling.setSaksopplysninger(saksopplysninger);
        behandling.setStatus(BehandlingStatus.OPPRETTET);
        behandlinger.add(behandling);
        return behandlinger;
    }
}
