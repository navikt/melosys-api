package no.nav.melosys.service;

import java.time.LocalDate;
import java.util.*;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.Oppgavetype;
import no.nav.melosys.domain.oppgave.PrioritetType;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.oppgave.dto.OppgaveDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OppgaveServiceTest {

    private OppgaveService oppgaveService;

    @Mock
    private FagsakRepository fagsakRepository;

    @Mock
    private GsakFasade gsakFasade;

    @Mock
    private TpsFasade tpsFasade;


    @Before
    public void setUp() {
        this.oppgaveService = new OppgaveService(
                gsakFasade,
                fagsakRepository,
                tpsFasade);
    }

    @Test
    public void henteMineSaker() throws MelosysException {

        List<Oppgave> oppgaver = new ArrayList<>();
        Oppgave oppgave1 = new Oppgave();
        oppgave1.setOppgaveId("1");
        oppgave1.setPrioritet(PrioritetType.HOY);
        oppgave1.setOppgavetype(Oppgavetype.BEH_SAK);
        oppgave1.setGsakSaksnummer("11");
        oppgave1.setTilordnetRessurs("12345678901");
        oppgaver.add(oppgave1);

        when(gsakFasade.finnOppgaveListeMedAnsvarlig(anyString())).
                thenAnswer((Answer) invocation -> {
                    String string = invocation.getArgument(0);//AnsvarligID
                    return (string.equals("12345678901")) ? oppgaver : new ArrayList<>();
                });

        Fagsak fagsak = new Fagsak();
        fagsak.setType(FagsakType.EU_EØS);
        fagsak.setStatus(FagsakStatus.OPPRETTET);
        List<Behandling> behandlinger = hentBehandlinger();
        fagsak.setBehandlinger(behandlinger);
        when(fagsakRepository.findByGsakSaksnummer(any(String.class))).thenReturn(fagsak);

        List<OppgaveDto> mineSaker = oppgaveService.hentOppgaverMedAnsvarlig("12345678901");
        assertThat(mineSaker.size()).isEqualTo(1);
        assertThat(mineSaker.get(0).getOppgaveID()).isEqualTo("1");

        mineSaker = oppgaveService.hentOppgaverMedAnsvarlig("12346678902");
        assertThat(mineSaker.size()).isEqualTo(0);

    }

    private List<Behandling> hentBehandlinger() {
        Set<Saksopplysning> saksopplysninger = new HashSet<>();
        PersonDokument personDokument = new PersonDokument();
        personDokument.fnr = "111111111111";

        Saksopplysning personOpplysning = new Saksopplysning();
        personOpplysning.setType(SaksopplysningType.PERSONOPPLYSNING);
        personOpplysning.setDokument(personDokument);
        saksopplysninger.add(personOpplysning);

        SoeknadDokument soeknadDokument = new SoeknadDokument();
        soeknadDokument.fnr = "111111111111";
        soeknadDokument.arbeidUtland = new ArbeidUtland();
        soeknadDokument.arbeidUtland.arbeidsland = Collections.singletonList(new Land(Land.NORGE));
        soeknadDokument.arbeidUtland.arbeidsperiode = new Periode(LocalDate.of(2018, 1, 21), LocalDate.of(2018, 5, 21));

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
