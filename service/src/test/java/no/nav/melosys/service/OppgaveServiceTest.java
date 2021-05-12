package no.nav.melosys.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad;
import no.nav.melosys.domain.behandlingsgrunnlag.data.MedfolgendeFamilie;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Periode;
import no.nav.melosys.domain.behandlingsgrunnlag.data.arbeidssteder.FysiskArbeidssted;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.Diskresjonskode;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.integrasjon.oppgave.OppgaveFasade;
import no.nav.melosys.integrasjon.oppgave.OppgaveOppdatering;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.oppgave.dto.BehandlingsoppgaveDto;
import no.nav.melosys.service.oppgave.dto.JournalfoeringsoppgaveDto;
import no.nav.melosys.service.oppgave.dto.OppgaveDto;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OppgaveServiceTest {
    @Mock
    private FagsakService fagsakService;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private OppgaveFasade oppgaveFasade;
    @Mock
    private PersondataFasade persondataFasade;
    @Mock
    private SaksopplysningerService saksopplysningerService;
    @Mock
    private BehandlingsgrunnlagService behandlingsgrunnlagService;

    private OppgaveService oppgaveService;

    @Captor
    private ArgumentCaptor<OppgaveOppdatering> oppgaveOppdateringCaptor;

    private Oppgave oppgave;
    private final String saksnummer = "MEL-12345";

    @BeforeEach
    public void setUp() {
        this.oppgaveService = new OppgaveService(
                behandlingService,
                fagsakService,
            oppgaveFasade,
                saksopplysningerService,
            behandlingsgrunnlagService, persondataFasade);

        oppgave = new Oppgave.Builder()
            .setOppgavetype(Oppgavetyper.BEH_SAK_MK)
            .setTilordnetRessurs("Z998877")
            .setSaksnummer(saksnummer)
            .build();
    }

    @Test
    void hentOppgaverMedAnsvarlig() {
        final String behOppgID = "1";
        final String jfrOppgID = "2";


        final String tilordnetRessurs = "Z2222";
        Oppgave.Builder oppgave1 = new Oppgave.Builder();
        oppgave1.setOppgaveId(behOppgID);
        oppgave1.setOppgavetype(Oppgavetyper.BEH_SAK_MK);
        oppgave1.setSaksnummer(saksnummer);

        Oppgave.Builder oppgave2 = new Oppgave.Builder();
        oppgave2.setOppgaveId(jfrOppgID);
        oppgave2.setOppgavetype(Oppgavetyper.JFR);

        Set<Oppgave> oppgaver = Set.of(oppgave1.build(), oppgave2.build());

        when(oppgaveFasade.finnOppgaverMedAnsvarlig(eq(tilordnetRessurs))).thenReturn(oppgaver);

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(saksnummer);
        fagsak.setType(Sakstyper.EU_EOS);
        fagsak.setStatus(Saksstatuser.OPPRETTET);
        Behandling behandling = lagBehandling();
        fagsak.setBehandlinger(List.of(behandling));
        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandling);
        when(fagsakService.hentFagsak(any(String.class))).thenReturn(fagsak);
        when(saksopplysningerService.finnPersonOpplysninger(anyLong())).thenReturn(Optional.of(lagPersonDokument()));
        when(behandlingsgrunnlagService.hentBehandlingsgrunnlag(eq(behandling.getId()))).thenReturn(lagBehandlingsgrunnlag());

        List<OppgaveDto> mineSaker = oppgaveService.hentOppgaverMedAnsvarlig(tilordnetRessurs);

        assertThat(mineSaker.size()).isEqualTo(2);

        OppgaveDto behOppg = mineSaker.stream()
            .filter(o -> o.getOppgaveID().equals(behOppgID))
            .findFirst()
            .get();

        assertThat(behOppg).isInstanceOf(BehandlingsoppgaveDto.class);
        assertThat(((BehandlingsoppgaveDto) behOppg).getBehandling().getBehandlingID()).isEqualTo(behandling.getId());

        OppgaveDto jfrOppg = mineSaker.stream()
            .filter(o -> o.getOppgaveID().equals(jfrOppgID))
            .findFirst()
            .get();

        assertThat(jfrOppg).isInstanceOf(JournalfoeringsoppgaveDto.class);
    }

    @Test
    void hentOppgaveForFagsaksnummer_oppgaveEksisterer_forventOppgave() {
        when(oppgaveFasade.finnOppgaverMedSaksnummer(eq(saksnummer))).thenReturn(List.of(oppgave));

        Oppgave oppgave = oppgaveService.hentOppgaveMedFagsaksnummer(saksnummer);
        assertThat(oppgave.erBehandling()).isTrue();
    }

    @Test
    void opprettEllerGjenbrukBehandlingsoppgave_ingenEksisterendeOppgave_oppgaveBlirOpprettet() {
        Behandling behandling = lagBehandling();
        behandling.setFagsak(new Fagsak());
        behandling.getFagsak().setSaksnummer("MEL-11111");
        behandling.setBehandlingsgrunnlag(new Behandlingsgrunnlag());
        behandling.getBehandlingsgrunnlag().setBehandlingsgrunnlagdata(new BehandlingsgrunnlagData());
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);

        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(behandling, "222", "333", "Z99999");
        verify(oppgaveFasade).opprettOppgave(any(Oppgave.class));
        verify(oppgaveFasade, never()).opprettSensitivOppgave(any(Oppgave.class));
    }

    @Test
    void opprettEllerGjenbrukBehandlingsoppgave_oppgaveEksistererSaksbehandlerErTilordnet_oppgaveBlirIkkeOpprettetEllerOppdatert() {
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setFagsak(new Fagsak());
        behandling.getFagsak().setSaksnummer(saksnummer);
        when(oppgaveFasade.finnOppgaverMedSaksnummer(eq(saksnummer))).thenReturn(List.of(oppgave));

        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(behandling, "222", "333", oppgave.getTilordnetRessurs());
        verify(oppgaveFasade, never()).opprettOppgave(any());
        verify(oppgaveFasade, never()).oppdaterOppgave(any(), any());
    }

    @Test
    void opprettEllerGjenbrukBehandlingsoppgave_oppgaveEksistererTilordnetAnnenRessurs_oppdaterTilordnetRessurs() {
        final String tilordnetRessurs = "Z12332123";
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setFagsak(new Fagsak());
        behandling.getFagsak().setSaksnummer(saksnummer);
        when(oppgaveFasade.finnOppgaverMedSaksnummer(eq(saksnummer))).thenReturn(List.of(oppgave));

        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(behandling, "222", "333", tilordnetRessurs);
        verify(oppgaveFasade, never()).opprettOppgave(any());
        verify(oppgaveFasade).oppdaterOppgave(eq(oppgave.getOppgaveId()), oppgaveOppdateringCaptor.capture());
        assertThat(oppgaveOppdateringCaptor.getValue().getTilordnetRessurs()).isEqualTo(tilordnetRessurs);
    }

    @Test
    void opprettEllerGjenbrukBehandlingsoppgave_personHarBeskyttelsesbehov_sensitivOppgaveBlirOpprettet() {
        Behandling behandling = lagBehandling();
        behandling.setFagsak(new Fagsak());
        behandling.getFagsak().setSaksnummer("MEL-11111");
        behandling.setBehandlingsgrunnlag(new Behandlingsgrunnlag());
        behandling.getBehandlingsgrunnlag().setBehandlingsgrunnlagdata(new BehandlingsgrunnlagData());
        behandling.hentPersonDokument().diskresjonskode = new Diskresjonskode("SPSF");
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);

        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(behandling, "222", "333", "Z99999");
        verify(oppgaveFasade, never()).opprettOppgave(any(Oppgave.class));
        verify(oppgaveFasade).opprettSensitivOppgave(any(Oppgave.class));
    }

    @Test
    void opprettEllerGjenbrukBehandlingsoppgave_barnHarBeskyttelsesbehov_sensitivOppgaveBlirOpprettet() {
        Behandling behandling = lagBehandling();
        behandling.setFagsak(new Fagsak());
        behandling.getFagsak().setSaksnummer("MEL-11111");
        behandling.setBehandlingsgrunnlag(new Behandlingsgrunnlag());
        behandling.getBehandlingsgrunnlag().setBehandlingsgrunnlagdata(new BehandlingsgrunnlagData());
        behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata().personOpplysninger.medfolgendeFamilie
            = List.of(MedfolgendeFamilie.tilBarnFraFnrOgNavn("fnrBarn", null));
        when(persondataFasade.harStrengtFortroligAdresse("fnrBarn")).thenReturn(true);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);

        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(behandling, "222", "333", "Z99999");
        verify(oppgaveFasade, never()).opprettOppgave(any(Oppgave.class));
        verify(oppgaveFasade).opprettSensitivOppgave(any(Oppgave.class));
    }

    private static Behandling lagBehandling() {
        Set<Saksopplysning> saksopplysninger = new HashSet<>();

        Saksopplysning personOpplysning = new Saksopplysning();
        personOpplysning.setType(SaksopplysningType.PERSOPL);
        personOpplysning.setDokument(lagPersonDokument());
        saksopplysninger.add(personOpplysning);

        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
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
        personDokument.diskresjonskode = new Diskresjonskode(null);
        return personDokument;
    }

    private static Behandlingsgrunnlag lagBehandlingsgrunnlag() {
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(lagSoeknadDokument());
        return behandlingsgrunnlag;
    }

    private static Soeknad lagSoeknadDokument() {
        Soeknad soeknad = new Soeknad();
        FysiskArbeidssted fysiskArbeidssted = new FysiskArbeidssted();
        fysiskArbeidssted.adresse.landkode = new Land(Land.NORGE).getKode();
        soeknad.arbeidPaaLand.fysiskeArbeidssteder = Collections.singletonList(fysiskArbeidssted);

        soeknad.oppholdUtland.oppholdslandkoder = Collections.singletonList(Landkoder.NO.getKode());
        soeknad.oppholdUtland.oppholdsPeriode = new Periode(LocalDate.now(), LocalDate.of(2018, 12, 12));

        soeknad.soeknadsland.landkoder.add(Landkoder.BE.getKode());
        return soeknad;
    }
}
