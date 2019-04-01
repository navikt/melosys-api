package no.nav.melosys.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
import no.nav.melosys.domain.dokument.soeknad.OppholdUtland;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.PrioritetType;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.FagsakRepository;
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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OppgaveServiceTest {

    private OppgaveService oppgaveService;

    @Mock
    private FagsakRepository fagsakRepository;

    @Mock
    private BehandlingRepository behandlingRepository;

    @Mock
    private GsakFasade gsakFasade;

    @Mock
    private TpsFasade tpsFasade;

    @Mock
    private SaksopplysningerService saksopplysningerService;

    @Before
    public void setUp() {
        this.oppgaveService = new OppgaveService(
                gsakFasade,
                fagsakRepository,
                behandlingRepository,
                tpsFasade,
            saksopplysningerService);
    }

    @Test
    public void hentOppgaverMedAnsvarlig() throws MelosysException {

        List<Oppgave> oppgaver = new ArrayList<>();
        Oppgave oppgave1 = new Oppgave();
        oppgave1.setOppgaveId("1");
        oppgave1.setOppgavetype(Oppgavetyper.BEH_SAK_MK);
        oppgave1.setPrioritet(PrioritetType.HOY);
        oppgave1.setSaksnummer("MEL-12345");
        oppgave1.setTilordnetRessurs("12345678901");
        oppgave1.setAktørId("aktørid");
        oppgaver.add(oppgave1);

        when(gsakFasade.finnOppgaveListeMedAnsvarlig(anyString())).
                thenAnswer((Answer<List<Oppgave>>) invocation -> {
                    String string = invocation.getArgument(0);//AnsvarligID
                    return (string.equals("12345678901")) ? oppgaver : new ArrayList<>();
                });

        when(saksopplysningerService.harAktivOppfrisking(anyLong())).thenReturn(true);
        doReturn("fnr").when(tpsFasade).hentIdentForAktørId("aktørid");
        doReturn("sammensattNavn").when(tpsFasade).hentSammensattNavn("fnr");

        Fagsak fagsak = new Fagsak();
        fagsak.setType(Sakstyper.EU_EOS);
        fagsak.setStatus(Saksstatuser.OPPRETTET);
        List<Behandling> behandlinger = new ArrayList<>();
        behandlinger.add(lagBehandling());
        fagsak.setBehandlinger(behandlinger);
        when(fagsakRepository.findBySaksnummer(any(String.class))).thenReturn(fagsak);
        when(behandlingRepository.findWithSaksopplysningerById(anyLong())).thenReturn(lagBehandling());

        List<OppgaveDto> mineSaker = oppgaveService.hentOppgaverMedAnsvarlig("12345678901");
        assertThat(mineSaker.size()).isEqualTo(1);
        assertThat(mineSaker.get(0).getOppgaveID()).isEqualTo("1");
        assertThat(((BehandlingsoppgaveDto)mineSaker.get(0)).getBehandling().isErUnderOppdatering()).isEqualTo(true);
        assertThat(mineSaker.get(0).getFnr()).isEqualTo("fnr");
        assertThat(mineSaker.get(0).getSammensattNavn()).isEqualTo("sammensattNavn");

        mineSaker = oppgaveService.hentOppgaverMedAnsvarlig("12346678902");
        assertThat(mineSaker.size()).isEqualTo(0);
    }

    @Test
    public void testHentOppgaveForFagsaksnummer() throws MelosysException {
        Oppgave oppgave1 = new Oppgave();
        oppgave1.setOppgavetype(Oppgavetyper.BEH_SAK_MK);
        oppgave1.setSaksnummer("MEL-12345");

        when(gsakFasade.finnOppgaveMedSaksnummer(anyString())).
            thenAnswer((Answer<Optional<Oppgave>>) invocation -> {
                String string = invocation.getArgument(0);
                if (string.equals("MEL-12345")) {
                    return Optional.of(oppgave1);
                } else {
                    return Optional.empty();
                }
            });

        Optional<Oppgave> oppgave = oppgaveService.hentOppgaveMedFagsaksnummer("MEL-12345");
        assertThat(oppgave.filter(Oppgave::erBehandling).isPresent()).isEqualTo(true);

        oppgave = oppgaveService.hentOppgaveMedFagsaksnummer("MEL-12346");
        assertThat(oppgave.isPresent()).isEqualTo(false);
    }

    private static Behandling lagBehandling() {
        Set<Saksopplysning> saksopplysninger = new HashSet<>();
        PersonDokument personDokument = new PersonDokument();
        personDokument.fnr = "111111111111";

        Saksopplysning personOpplysning = new Saksopplysning();
        personOpplysning.setType(SaksopplysningType.PERSONOPPLYSNING);
        personOpplysning.setDokument(personDokument);
        saksopplysninger.add(personOpplysning);

        SoeknadDokument soeknadDokument = new SoeknadDokument();
        ArbeidUtland arbeidUtland = new ArbeidUtland();
        arbeidUtland.adresse = new StrukturertAdresse();
        arbeidUtland.adresse.landKode = new Land(Land.NORGE).getKode();
        soeknadDokument.arbeidUtland = Collections.singletonList(arbeidUtland);

        soeknadDokument.oppholdUtland = new OppholdUtland();
        soeknadDokument.oppholdUtland.oppholdslandKoder = Collections.singletonList(new Land(Land.NORGE).getKode());
        soeknadDokument.oppholdUtland.oppholdsPeriode = new Periode(LocalDate.now(), LocalDate.of(2018, 12, 12));

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.SØKNAD);
        saksopplysning.setDokument(soeknadDokument);
        saksopplysninger.add(saksopplysning);

        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setEndretDato(Instant.now());
        behandling.setSaksopplysninger(saksopplysninger);
        behandling.setStatus(Behandlingsstatus.OPPRETTET);
        return behandling;
    }
}
