package no.nav.melosys.service;

import java.time.LocalDate;
import java.util.*;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.*;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.Oppgavetype;
import no.nav.melosys.domain.oppgave.PrioritetType;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.oppgave.dto.BehandlingsoppgaveDto;
import no.nav.melosys.service.oppgave.dto.OppgaveDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
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

    @Mock
    private ProsessinstansRepository prosessinstansRepository;

    @Before
    public void setUp() {
        this.oppgaveService = new OppgaveService(
                gsakFasade,
                fagsakRepository,
                tpsFasade,
                prosessinstansRepository);
    }

    @Test
    public void hentMineSaker() throws TekniskException {

        List<Oppgave> oppgaver = new ArrayList<>();
        Oppgave oppgave1 = new Oppgave();
        oppgave1.setOppgaveId("1");
        oppgave1.setOppgavetype(Oppgavetype.BEH_SAK);
        oppgave1.setPrioritet(PrioritetType.HOY);
        oppgave1.setOppgavetype(Oppgavetype.BEH_SAK);
        oppgave1.setGsakSaksnummer(11L);
        oppgave1.setTilordnetRessurs("12345678901");
        oppgaver.add(oppgave1);

        when(gsakFasade.finnOppgaveListeMedAnsvarlig(anyString())).
                thenAnswer((Answer) invocation -> {
                    String string = invocation.getArgument(0);//AnsvarligID
                    return (string.equals("12345678901")) ? oppgaver : new ArrayList<>();
                });

        Behandling behandling = new Behandling();

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        when(prosessinstansRepository.findByStegIsNotNullAndTypeAndBehandling_Id(any(), anyLong())).thenReturn(Optional.of(prosessinstans));

        Fagsak fagsak = new Fagsak();
        fagsak.setType(FagsakType.EU_EØS);
        fagsak.setStatus(FagsakStatus.OPPRETTET);
        List<Behandling> behandlinger = hentBehandlinger();
        fagsak.setBehandlinger(behandlinger);
        when(fagsakRepository.findByGsakSaksnummer(any(Long.class))).thenReturn(fagsak);

        List<OppgaveDto> mineSaker = oppgaveService.hentOppgaverMedAnsvarlig("12345678901");
        assertThat(mineSaker.size()).isEqualTo(1);
        assertThat(mineSaker.get(0).getOppgaveID()).isEqualTo("1");
        assertThat(((BehandlingsoppgaveDto)mineSaker.get(0)).getBehandling().erUnderOppdatering()).isEqualTo(true);

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
        ArbeidUtland arbeidUtland = new ArbeidUtland();
        arbeidUtland.adresse = new StandardAdresse();
        arbeidUtland.adresse.landKode = new Land(Land.NORGE).getKode();
        soeknadDokument.arbeidUtland = Collections.singletonList(arbeidUtland);

        soeknadDokument.oppholdUtland = new OppholdUtland();
        soeknadDokument.oppholdUtland.oppholdslandKoder = Collections.singletonList(new Land(Land.NORGE).getKode());
        soeknadDokument.oppholdUtland.oppholdsPeriode = new Periode(LocalDate.now(), LocalDate.of(2018, 12, 12));

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
