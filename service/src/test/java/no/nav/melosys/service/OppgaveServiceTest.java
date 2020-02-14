package no.nav.melosys.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.PrioritetType;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.oppgave.dto.BehandlingDto;
import no.nav.melosys.service.oppgave.dto.BehandlingsoppgaveDto;
import no.nav.melosys.service.oppgave.dto.OppgaveDto;
import no.nav.melosys.service.sak.FagsakService;
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
    @Mock
    private FagsakService fagsakService;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private GsakFasade gsakFasade;
    @Mock
    private TpsFasade tpsFasade;
    @Mock
    private SaksopplysningerService saksopplysningerService;
    @Mock
    private BehandlingsgrunnlagService behandlingsgrunnlagService;

    private OppgaveService oppgaveService;

    @Before
    public void setUp() throws FunksjonellException, TekniskException {
        this.oppgaveService = new OppgaveService(
                behandlingService,
                fagsakService,
                gsakFasade,
                saksopplysningerService,
            behandlingsgrunnlagService, tpsFasade);

        Oppgave.Builder oppgaveBuilder = new Oppgave.Builder();
        oppgaveBuilder.setOppgavetype(Oppgavetyper.BEH_SAK_MK);
        oppgaveBuilder.setTilordnetRessurs("Z998877");
        oppgaveBuilder.setSaksnummer("MEL-12345");

        when(gsakFasade.hentOppgaveMedSaksnummer(anyString())).
            thenAnswer((Answer<Oppgave>) invocation -> {
                String string = invocation.getArgument(0);
                if (string.equals("MEL-12345")) {
                    return oppgaveBuilder.build();
                } else {
                    throw new TekniskException("Finner ingen oppgave for fagsak " + string);
                }
            });
    }

    @Test
    public void hentOppgaverMedAnsvarlig() throws MelosysException {

        Collection<Oppgave> oppgaver = new HashSet<>();
        Oppgave.Builder oppgave1 = new Oppgave.Builder();
        oppgave1.setOppgaveId("1");
        oppgave1.setOppgavetype(Oppgavetyper.BEH_SAK_MK);
        oppgave1.setPrioritet(PrioritetType.HOY);
        oppgave1.setSaksnummer("MEL-12345");
        oppgave1.setTilordnetRessurs("12345678901");
        oppgave1.setAktørId("aktørid");
        oppgaver.add(oppgave1.build());

        when(gsakFasade.finnOppgaveListeMedAnsvarlig(anyString())).
                thenAnswer((Answer<Collection<Oppgave>>) invocation -> {
                    String string = invocation.getArgument(0);//AnsvarligID
                    return (string.equals("12345678901")) ? oppgaver : new HashSet<>();
                });

        when(saksopplysningerService.harAktivOppfrisking(anyLong())).thenReturn(true);

        Fagsak fagsak = new Fagsak();
        fagsak.setType(Sakstyper.EU_EOS);
        fagsak.setStatus(Saksstatuser.OPPRETTET);
        List<Behandling> behandlinger = new ArrayList<>();
        behandlinger.add(lagBehandling());
        fagsak.setBehandlinger(behandlinger);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(lagBehandling());
        when(fagsakService.hentFagsak(any(String.class))).thenReturn(fagsak);
        when(saksopplysningerService.finnPersonOpplysninger(anyLong())).thenReturn(Optional.of(lagPersonDokument()));

        List<OppgaveDto> mineSaker = oppgaveService.hentOppgaverMedAnsvarlig("12345678901");
        assertThat(mineSaker.size()).isEqualTo(1);
        BehandlingsoppgaveDto oppgave = (BehandlingsoppgaveDto) mineSaker.get(0);
        assertThat(oppgave.getOppgaveID()).isEqualTo("1");
        assertThat(oppgave.getFnr()).isEqualTo("fnr");
        assertThat(oppgave.getSammensattNavn()).isEqualTo("sammensattNavn");

        BehandlingDto behandlingDto = oppgave.getBehandling();
        assertThat(behandlingDto.isErUnderOppdatering()).isEqualTo(true);
        assertThat(behandlingDto.getRegistrertDato()).isEqualTo(Instant.ofEpochMilli(111L));
        assertThat(behandlingDto.getEndretDato()).isEqualTo(Instant.ofEpochMilli(222L));
        assertThat(behandlingDto.getSvarFrist()).isEqualTo(Instant.ofEpochMilli(333L));

        mineSaker = oppgaveService.hentOppgaverMedAnsvarlig("12346678902");
        assertThat(mineSaker.size()).isEqualTo(0);
    }

    @Test
    public void hentOppgaveForFagsaksnummer_modOppgaveSomFinnes_forventOppgave() throws MelosysException {
        Oppgave oppgave = oppgaveService.hentOppgaveMedFagsaksnummer("MEL-12345");
        assertThat(oppgave.erBehandling()).isEqualTo(true);
    }

    @Test(expected = TekniskException.class)
    public void hentOppgaveForFagsaksnummer_medOppgaveSomIkkeFinnes_forventException() throws MelosysException {
        oppgaveService.hentOppgaveMedFagsaksnummer("MEL-12346");
    }

    private static Behandling lagBehandling() {
        Set<Saksopplysning> saksopplysninger = new HashSet<>();

        Saksopplysning personOpplysning = new Saksopplysning();
        personOpplysning.setType(SaksopplysningType.PERSOPL);
        personOpplysning.setDokument(lagPersonDokument());
        saksopplysninger.add(personOpplysning);

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.SØKNAD);
        saksopplysning.setDokument(lagSoeknadDokument());
        saksopplysninger.add(saksopplysning);

        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setRegistrertDato(Instant.ofEpochMilli(111L));
        behandling.setEndretDato(Instant.ofEpochMilli(222L));
        behandling.setSaksopplysninger(saksopplysninger);
        behandling.setDokumentasjonSvarfristDato(Instant.ofEpochMilli(333L));
        behandling.setStatus(Behandlingsstatus.OPPRETTET);
        return behandling;
    }

    private static PersonDokument lagPersonDokument() {
        PersonDokument personDokument = new PersonDokument();
        personDokument.fnr = "fnr";
        personDokument.sammensattNavn = "sammensattNavn";
        return personDokument;
    }

    private static SoeknadDokument lagSoeknadDokument() {
        SoeknadDokument soeknadDokument = new SoeknadDokument();
        ArbeidUtland arbeidUtland = new ArbeidUtland();
        arbeidUtland.adresse.landkode = new Land(Land.NORGE).getKode();
        soeknadDokument.arbeidUtland = Collections.singletonList(arbeidUtland);

        soeknadDokument.oppholdUtland.oppholdslandkoder = Collections.singletonList(Landkoder.NO.getKode());
        soeknadDokument.oppholdUtland.oppholdsPeriode = new Periode(LocalDate.now(), LocalDate.of(2018, 12, 12));

        soeknadDokument.soeknadsland.landkoder.add(Landkoder.BE.getKode());
        return soeknadDokument;
    }
}
