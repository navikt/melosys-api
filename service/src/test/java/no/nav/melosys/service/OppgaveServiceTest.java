package no.nav.melosys.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad;
import no.nav.melosys.domain.behandlingsgrunnlag.data.MedfolgendeFamilie;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Periode;
import no.nav.melosys.domain.behandlingsgrunnlag.data.arbeidssteder.FysiskArbeidssted;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.Diskresjonskode;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.integrasjon.ereg.EregFasade;
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
import no.nav.melosys.service.saksopplysninger.SaksopplysningerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.BRUKER;
import static org.assertj.core.api.Assertions.assertThat;
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
    private EregFasade eregFasade;
    @Mock
    private SaksopplysningerService saksopplysningerService;
    @Mock
    private BehandlingsgrunnlagService behandlingsgrunnlagService;

    private final FakeUnleash unleash = new FakeUnleash();

    private OppgaveService oppgaveService;

    @Captor
    private ArgumentCaptor<OppgaveOppdatering> oppgaveOppdateringCaptor;

    @Captor
    private ArgumentCaptor<Oppgave> oppgaveCaptor;

    private Oppgave oppgave;
    private static final String SAKSNUMMER = "MEL-12345";

    @BeforeEach
    public void setUp() {
        this.oppgaveService = new OppgaveService(
            behandlingService,
            fagsakService,
            oppgaveFasade,
            saksopplysningerService,
            behandlingsgrunnlagService,
            persondataFasade,
            eregFasade,
            unleash);

        unleash.enableAll();
        oppgave = new Oppgave.Builder()
            .setOppgavetype(Oppgavetyper.BEH_SAK_MK)
            .setTilordnetRessurs("Z998877")
            .setSaksnummer(SAKSNUMMER)
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
        oppgave1.setSaksnummer(SAKSNUMMER);

        Oppgave.Builder oppgave2 = new Oppgave.Builder();
        oppgave2.setOppgaveId(jfrOppgID);
        oppgave2.setOppgavetype(Oppgavetyper.JFR);

        Set<Oppgave> oppgaver = Set.of(oppgave1.build(), oppgave2.build());

        when(oppgaveFasade.finnOppgaverMedAnsvarlig(tilordnetRessurs)).thenReturn(oppgaver);

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(SAKSNUMMER);
        fagsak.setType(Sakstyper.EU_EOS);
        fagsak.setStatus(Saksstatuser.OPPRETTET);
        Behandling behandling = lagBehandling();
        fagsak.setBehandlinger(List.of(behandling));
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(fagsakService.hentFagsak(any(String.class))).thenReturn(fagsak);
        when(behandlingsgrunnlagService.finnBehandlingsgrunnlag(behandling.getId())).thenReturn(Optional.of(lagBehandlingsgrunnlag()));

        List<OppgaveDto> mineSaker = oppgaveService.hentOppgaverMedAnsvarlig(tilordnetRessurs);

        assertThat(mineSaker).hasSize(2);

        Optional<OppgaveDto> behOppgOpt = mineSaker.stream()
            .filter(o -> o.getOppgaveID().equals(behOppgID))
            .findFirst();

        assertThat(behOppgOpt).isPresent().get().isInstanceOf(BehandlingsoppgaveDto.class);
        assertThat(((BehandlingsoppgaveDto) behOppgOpt.get()).getBehandling().getBehandlingID()).isEqualTo(behandling.getId());
        assertThat(((BehandlingsoppgaveDto) behOppgOpt.get()).getLand()).isNotNull();

        Optional<OppgaveDto> jfrOppgOpt = mineSaker.stream()
            .filter(o -> o.getOppgaveID().equals(jfrOppgID))
            .findFirst();

        assertThat(jfrOppgOpt).isPresent().get().isInstanceOf(JournalfoeringsoppgaveDto.class);
    }

    @Test
    void hentOppgaverMedAnsvarlig_behandlingsgrunnlagFinnesIkke_mappesKorrekt() {
        final String behOppgID = "1";
        final String tilordnetRessurs = "Z2222";
        Oppgave.Builder oppgave = new Oppgave.Builder();
        oppgave.setOppgaveId(behOppgID);
        oppgave.setOppgavetype(Oppgavetyper.BEH_SAK_MK);
        oppgave.setSaksnummer(SAKSNUMMER);

        Set<Oppgave> oppgaver = Set.of(oppgave.build());

        when(oppgaveFasade.finnOppgaverMedAnsvarlig(tilordnetRessurs)).thenReturn(oppgaver);

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(SAKSNUMMER);
        fagsak.setType(Sakstyper.EU_EOS);
        fagsak.setStatus(Saksstatuser.OPPRETTET);
        Behandling behandling = lagBehandling();
        fagsak.setBehandlinger(List.of(behandling));
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(fagsakService.hentFagsak(any(String.class))).thenReturn(fagsak);
        when(behandlingsgrunnlagService.finnBehandlingsgrunnlag(behandling.getId())).thenReturn(Optional.empty());

        List<OppgaveDto> mineSaker = oppgaveService.hentOppgaverMedAnsvarlig(tilordnetRessurs);

        assertThat(mineSaker).hasSize(1);

        Optional<OppgaveDto> behOppgOpt = mineSaker.stream()
            .filter(o -> o.getOppgaveID().equals(behOppgID))
            .findFirst();

        assertThat(behOppgOpt).isPresent().get().isInstanceOf(BehandlingsoppgaveDto.class);
        assertThat(((BehandlingsoppgaveDto) behOppgOpt.get()).getBehandling().getBehandlingID()).isEqualTo(behandling.getId());
        assertThat(((BehandlingsoppgaveDto) behOppgOpt.get()).getLand()).isNull();
    }

    @Test
    void hentOppgaverMedAnsvarlig_aktøridOgOrgnrErNull_forventUkjentIdOgNavn() {
        final String jfrOppgID = "2";
        final String tilordnetRessurs = "Z2222";

        Oppgave.Builder oppgave = new Oppgave.Builder();
        oppgave.setOppgaveId(jfrOppgID);
        oppgave.setOppgavetype(Oppgavetyper.JFR);
        oppgave.setAktørId(null);
        oppgave.setOrgnr(null);
        Set<Oppgave> oppgaver = Set.of(oppgave.build());

        when(oppgaveFasade.finnOppgaverMedAnsvarlig(tilordnetRessurs)).thenReturn(oppgaver);

        List<OppgaveDto> mineSaker = oppgaveService.hentOppgaverMedAnsvarlig(tilordnetRessurs);

        assertThat(mineSaker).hasSize(1);

        Optional<OppgaveDto> jfrOppgDt = mineSaker.stream().findFirst();

        assertThat(jfrOppgDt).isPresent();
        OppgaveDto oppgaveDto = jfrOppgDt.get();

        assertThat(oppgaveDto.getHovedpartIdent()).isEqualTo("UKJENT");
        assertThat(oppgaveDto.getNavn()).isEqualTo("UKJENT");
    }

    @Test
    void hentOppgaverMedAnsvarlig_aktørIdEksisterer_forventFnrOgSammensattNavn() {
        final String jfrOppgID = "2";
        final String tilordnetRessurs = "Z2222";

        Oppgave.Builder oppgave = new Oppgave.Builder();
        oppgave.setOppgaveId(jfrOppgID);
        oppgave.setOppgavetype(Oppgavetyper.JFR);
        oppgave.setAktørId("1111");
        oppgave.setOrgnr(null);
        Set<Oppgave> oppgaver = Set.of(oppgave.build());

        when(oppgaveFasade.finnOppgaverMedAnsvarlig(tilordnetRessurs)).thenReturn(oppgaver);
        when(persondataFasade.finnFolkeregisterident("1111")).thenReturn(Optional.of("fnr"));
        when(persondataFasade.hentSammensattNavn("fnr")).thenReturn("sammensatt navn");

        List<OppgaveDto> mineSaker = oppgaveService.hentOppgaverMedAnsvarlig(tilordnetRessurs);

        assertThat(mineSaker).hasSize(1);

        Optional<OppgaveDto> jfrOppgDt = mineSaker.stream().findFirst();

        assertThat(jfrOppgDt).isPresent();
        OppgaveDto oppgaveDto = jfrOppgDt.get();

        assertThat(oppgaveDto.getHovedpartIdent()).isEqualTo("fnr");
        assertThat(oppgaveDto.getNavn()).isEqualTo("sammensatt navn");
    }

    @Test
    void hentOppgaverMedAnsvarlig_orgnrEksisterer_forventOrgnrOgNavn() {
        final String jfrOppgID = "2";
        final String tilordnetRessurs = "Z2222";

        Oppgave.Builder oppgave = new Oppgave.Builder();
        oppgave.setOppgaveId(jfrOppgID);
        oppgave.setOppgavetype(Oppgavetyper.JFR);
        oppgave.setAktivDato(null);
        oppgave.setOrgnr("2222");
        Set<Oppgave> oppgaver = Set.of(oppgave.build());

        when(oppgaveFasade.finnOppgaverMedAnsvarlig(tilordnetRessurs)).thenReturn(oppgaver);
        when(eregFasade.hentOrganisasjonNavn("2222")).thenReturn("organisasjonsnavn");

        List<OppgaveDto> mineSaker = oppgaveService.hentOppgaverMedAnsvarlig(tilordnetRessurs);

        assertThat(mineSaker).hasSize(1);

        Optional<OppgaveDto> jfrOppgDt = mineSaker.stream().findFirst();

        assertThat(jfrOppgDt).isPresent();
        OppgaveDto oppgaveDto = jfrOppgDt.get();

        assertThat(oppgaveDto.getHovedpartIdent()).isEqualTo("2222");
        assertThat(oppgaveDto.getNavn()).isEqualTo("organisasjonsnavn");
    }

    @Test
    void hentOppgaveForFagsaksnummer_oppgaveEksisterer_forventOppgave() {
        when(oppgaveFasade.finnÅpneOppgaverMedSaksnummer(SAKSNUMMER)).thenReturn(List.of(oppgave));

        Oppgave oppgave = oppgaveService.hentÅpenOppgaveMedFagsaksnummer(SAKSNUMMER);
        assertThat(oppgave.erBehandling()).isTrue();
    }

    @Test
    void opprettEllerGjenbrukBehandlingsoppgave_ingenEksisterendeOppgave_oppgaveBlirOpprettet() {
        Behandling behandling = lagBehandling();
        behandling.setBehandlingsgrunnlag(new Behandlingsgrunnlag());
        behandling.getBehandlingsgrunnlag().setBehandlingsgrunnlagdata(new BehandlingsgrunnlagData());
        when(behandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);

        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(behandling, "222", "333", "Z99999");
        verify(oppgaveFasade).opprettOppgave(any(Oppgave.class));
        verify(oppgaveFasade, never()).opprettSensitivOppgave(any(Oppgave.class));

    }

    @Test
    void opprettEllerGjenbrukBehandlingsoppgave_oppgaveOpprettElektroniskSøknad_oppgaveBlirOpprettetMedBeskrivelse() {
        Behandling behandling = lagBehandling();
        behandling.setBehandlingsgrunnlag(new Behandlingsgrunnlag());
        behandling.getBehandlingsgrunnlag().setBehandlingsgrunnlagdata(new BehandlingsgrunnlagData());
        behandling.getBehandlingsgrunnlag().setType(Behandlingsgrunnlagtyper.SØKNAD_A1_UTSENDTE_ARBEIDSTAKERE_EØS);
        when(behandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);

        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(behandling, "222", "333", "Z99999");

        verify(oppgaveFasade).opprettOppgave(oppgaveCaptor.capture());
        assertThat(oppgaveCaptor.getValue().getBeskrivelse()).isEqualTo(behandling.getTema().getBeskrivelse());
    }

    @Test
    void opprettEllerGjenbrukBehandlingsoppgave_oppgaveOpprettElektroniskSøknad_oppgaveBlirOpprettetMedBeskrvielse() {
        unleash.disableAll();

        final String mottattString = "Mottatt elektronisk søknad";
        Behandling behandling = lagBehandling();
        behandling.setBehandlingsgrunnlag(new Behandlingsgrunnlag());
        behandling.getBehandlingsgrunnlag().setBehandlingsgrunnlagdata(new BehandlingsgrunnlagData());
        behandling.getBehandlingsgrunnlag().setType(Behandlingsgrunnlagtyper.SØKNAD_A1_UTSENDTE_ARBEIDSTAKERE_EØS);
        when(behandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);

        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(behandling, "222", "333", "Z99999");

        verify(oppgaveFasade).opprettOppgave(oppgaveCaptor.capture());
        assertThat(oppgaveCaptor.getValue().getBeskrivelse()).isEqualTo(mottattString);
    }

    @Test
    void opprettEllerGjenbrukBehandlingsoppgave_oppgaveNyVurdering_oppgaveBlirOpprettetMedBeskrivelse() {
        Behandling behandling = lagBehandling();
        behandling.setType(Behandlingstyper.NY_VURDERING);
        when(behandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);

        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(behandling, "222", "333", "Z99999");

        verify(oppgaveFasade).opprettOppgave(oppgaveCaptor.capture());
        assertThat(oppgaveCaptor.getValue().getBeskrivelse()).isEqualTo(behandling.getTema().getBeskrivelse());
    }

    @Test
    void opprettEllerGjenbrukBehandlingsoppgave_oppgaveNyVurdering_oppgaveBlirOpprettetMedBeskrvielse() {
        unleash.disableAll();

        final String mottattString = "Ny vurdering";
        Behandling behandling = lagBehandling();
        behandling.setType(Behandlingstyper.NY_VURDERING);
        when(behandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);

        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(behandling, "222", "333", "Z99999");

        verify(oppgaveFasade).opprettOppgave(oppgaveCaptor.capture());
        assertThat(oppgaveCaptor.getValue().getBeskrivelse()).isEqualTo(mottattString);
    }

    @Test
    void opprettEllerGjenbrukBehandlingsoppgave_oppgaveEksistererSaksbehandlerErTilordnet_oppgaveBlirIkkeOpprettetEllerOppdatert() {
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setFagsak(new Fagsak());
        behandling.getFagsak().setSaksnummer(SAKSNUMMER);
        when(oppgaveFasade.finnÅpneOppgaverMedSaksnummer(SAKSNUMMER)).thenReturn(List.of(oppgave));

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
        behandling.getFagsak().setSaksnummer(SAKSNUMMER);
        when(oppgaveFasade.finnÅpneOppgaverMedSaksnummer(SAKSNUMMER)).thenReturn(List.of(oppgave));

        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(behandling, "222", "333", tilordnetRessurs);
        verify(oppgaveFasade, never()).opprettOppgave(any());
        verify(oppgaveFasade).oppdaterOppgave(eq(oppgave.getOppgaveId()), oppgaveOppdateringCaptor.capture());
        assertThat(oppgaveOppdateringCaptor.getValue().getTilordnetRessurs()).isEqualTo(tilordnetRessurs);
    }

    @Test
    void opprettEllerGjenbrukBehandlingsoppgave_personHarBeskyttelsesbehov_sensitivOppgaveBlirOpprettet() {
        Behandling behandling = lagBehandling();
        behandling.setBehandlingsgrunnlag(new Behandlingsgrunnlag());
        behandling.getBehandlingsgrunnlag().setBehandlingsgrunnlagdata(new BehandlingsgrunnlagData());
        when(behandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);
        when(persondataFasade.harStrengtFortroligAdresse("aktørID")).thenReturn(true);

        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(behandling, "222", "333", "Z99999");

        verify(oppgaveFasade, never()).opprettOppgave(any(Oppgave.class));
        verify(oppgaveFasade).opprettSensitivOppgave(any(Oppgave.class));
    }

    @Test
    void opprettEllerGjenbrukBehandlingsoppgave_barnHarBeskyttelsesbehov_sensitivOppgaveBlirOpprettet() {
        Behandling behandling = lagBehandling();
        behandling.setBehandlingsgrunnlag(new Behandlingsgrunnlag());
        behandling.getBehandlingsgrunnlag().setBehandlingsgrunnlagdata(new BehandlingsgrunnlagData());
        behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata().personOpplysninger.medfolgendeFamilie
            = List.of(MedfolgendeFamilie.tilBarnFraFnrOgNavn("fnrBarn", null));
        when(persondataFasade.harStrengtFortroligAdresse("aktørID")).thenReturn(false);
        when(persondataFasade.harStrengtFortroligAdresse("fnrBarn")).thenReturn(true);
        when(behandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);

        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(behandling, "222", "333", "Z99999");

        verify(oppgaveFasade, never()).opprettOppgave(any(Oppgave.class));
        verify(oppgaveFasade).opprettSensitivOppgave(any(Oppgave.class));
    }

    @Test
    void opprettOppgaveForSak_oppretterNyOppgaveForFagsak() {
        var behandling = lagBehandling();
        var fagsak = behandling.getFagsak();
        fagsak.setBehandlinger(List.of(behandling));
        var oppgave1 = new Oppgave.Builder()
            .setTilordnetRessurs("tilordnet ressurs 1").setOpprettetTidspunkt(LocalDate.now().atStartOfDay(ZoneId.systemDefault())).setStatus("FERDIGSTILT").build();
        var oppgave2 = new Oppgave.Builder()
            .setTilordnetRessurs("tilordnet ressurs 2").setOpprettetTidspunkt(LocalDate.now().minusDays(2).atStartOfDay(ZoneId.systemDefault())).setStatus("FERDIGSTILT").build();

        when(persondataFasade.harStrengtFortroligAdresse("aktørID")).thenReturn(false);
        when(behandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);
        when(oppgaveFasade.finnAvsluttetOppgaverMedSaksnummer(SAKSNUMMER)).thenReturn(List.of(oppgave1, oppgave2));
        when(fagsakService.hentFagsak(SAKSNUMMER)).thenReturn(fagsak);

        oppgaveService.opprettOppgaveForSak(SAKSNUMMER);

        verify(oppgaveFasade).opprettOppgave(oppgaveCaptor.capture());
        assertThat(oppgaveCaptor.getValue().getSaksnummer()).isEqualTo(SAKSNUMMER);
        assertThat(oppgaveCaptor.getValue().getTilordnetRessurs()).isEqualTo(oppgave1.getTilordnetRessurs());
    }

    @Test
    void saksbehandlerErTilordnetOppgaveForSaksnummer_erTilordnet_erSann() {
        final var saksnummer = "MEL-0";
        final var saksbehandler = "Z12111";
        final var oppgave = new Oppgave.Builder().setTilordnetRessurs(saksbehandler).build();

        when(oppgaveFasade.finnÅpneOppgaverMedSaksnummer(saksnummer)).thenReturn(List.of(oppgave));
        assertThat(oppgaveService.saksbehandlerErTilordnetOppgaveForSaksnummer(saksbehandler, saksnummer)).isTrue();
    }

    @Test
    void saksbehandlerErTilordnetOppgaveForSaksnummer_erIkkeTilordnet_erIkkeSann() {
        final var saksnummer = "MEL-0";
        final var saksbehandler = "Z12111";
        final var oppgave = new Oppgave.Builder().build();

        when(oppgaveFasade.finnÅpneOppgaverMedSaksnummer(saksnummer)).thenReturn(List.of(oppgave));
        assertThat(oppgaveService.saksbehandlerErTilordnetOppgaveForSaksnummer(saksbehandler, saksnummer)).isFalse();
    }

    @Test
    void saksbehandlerErTilordnetOppgaveForSaksnummer_finnesIngenOppgaver_erIkkeSann() {
        final var saksnummer = "MEL-0";

        when(oppgaveFasade.finnÅpneOppgaverMedSaksnummer(saksnummer)).thenReturn(Collections.emptyList());
        assertThat(oppgaveService.saksbehandlerErTilordnetOppgaveForSaksnummer("Z12111", saksnummer)).isFalse();
    }

    private static Behandling lagBehandling() {
        Set<Saksopplysning> saksopplysninger = new HashSet<>();

        Saksopplysning personOpplysning = new Saksopplysning();
        personOpplysning.setType(SaksopplysningType.PERSOPL);
        personOpplysning.setDokument(lagPersonDokument());
        saksopplysninger.add(personOpplysning);

        Behandling behandling = new Behandling();
        final var fagsak = new Fagsak();
        fagsak.setSaksnummer(SAKSNUMMER);
        fagsak.setType(Sakstyper.EU_EOS);
        fagsak.setTema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        Aktoer bruker = new Aktoer();
        bruker.setRolle(BRUKER);
        bruker.setAktørId("aktørID");
        fagsak.getAktører().add(bruker);
        behandling.setFagsak(fagsak);
        behandling.setType(Behandlingstyper.FØRSTEGANG);
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
        personDokument.setFnr("fnr");
        personDokument.setSammensattNavn("sammensattNavn");
        personDokument.setDiskresjonskode(new Diskresjonskode(null));
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
        fysiskArbeidssted.adresse.setLandkode(new Land(Land.NORGE).getKode());
        soeknad.arbeidPaaLand.fysiskeArbeidssteder = Collections.singletonList(fysiskArbeidssted);

        soeknad.oppholdUtland.oppholdslandkoder = Collections.singletonList(Landkoder.NO.getKode());
        soeknad.oppholdUtland.oppholdsPeriode = new Periode(LocalDate.now(), LocalDate.of(2018, 12, 12));

        soeknad.soeknadsland.landkoder.add(Landkoder.BE.getKode());
        return soeknad;
    }
}
