package no.nav.melosys.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.Diskresjonskode;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.mottatteopplysninger.AnmodningEllerAttest;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.domain.mottatteopplysninger.Soeknad;
import no.nav.melosys.domain.mottatteopplysninger.data.MedfolgendeFamilie;
import no.nav.melosys.domain.mottatteopplysninger.data.Periode;
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.oppgave.OppgaveFasade;
import no.nav.melosys.integrasjon.oppgave.OppgaveOppdatering;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.UtledMottaksdato;
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService;
import no.nav.melosys.service.oppgave.OppgaveFactory;
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
    private static final String SAKSNUMMER = "MEL-12345";
    private static final String BEH_OPPG_ID = "1";
    private static final String JFR_OPPG_ID = "2";
    private static final String TILORDNET_RESSURS = "Z123456";

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
    private UtledMottaksdato utledMottaksdato;
    @Mock
    private SaksopplysningerService saksopplysningerService;
    @Mock
    private MottatteOpplysningerService mottatteOpplysningerService;

    private OppgaveService oppgaveService;

    @Captor
    private ArgumentCaptor<OppgaveOppdatering> oppgaveOppdateringCaptor;
    @Captor
    private ArgumentCaptor<Oppgave> oppgaveCaptor;

    private Oppgave oppgave;

    private final OppgaveFactory oppgaveFactory = new OppgaveFactory();

    @BeforeEach
    public void setUp() {
        this.oppgaveService = new OppgaveService(
            behandlingService,
            fagsakService,
            oppgaveFasade,
            saksopplysningerService,
            mottatteOpplysningerService,
            persondataFasade,
            eregFasade,
            utledMottaksdato,
            oppgaveFactory);

        oppgave = new Oppgave.Builder()
            .setOppgavetype(Oppgavetyper.BEH_SAK_MK)
            .setTilordnetRessurs(TILORDNET_RESSURS)
            .setOppgaveId(BEH_OPPG_ID)
            .setSaksnummer(SAKSNUMMER)
            .build();
    }

    @Test
    void hentOppgaverMedAnsvarlig() {
        Oppgave.Builder oppgave1 = new Oppgave.Builder();
        oppgave1.setOppgaveId(BEH_OPPG_ID);
        oppgave1.setOppgavetype(Oppgavetyper.BEH_SAK_MK);
        oppgave1.setSaksnummer(SAKSNUMMER);

        Oppgave.Builder oppgave2 = new Oppgave.Builder();
        oppgave2.setOppgaveId(JFR_OPPG_ID);
        oppgave2.setOppgavetype(Oppgavetyper.JFR);

        Set<Oppgave> oppgaver = Set.of(oppgave1.build(), oppgave2.build());

        when(oppgaveFasade.finnOppgaverMedAnsvarlig(TILORDNET_RESSURS)).thenReturn(oppgaver);

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(SAKSNUMMER);
        fagsak.setType(Sakstyper.EU_EOS);
        fagsak.setStatus(Saksstatuser.OPPRETTET);
        Behandling behandling = lagBehandling();
        fagsak.setBehandlinger(List.of(behandling));
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(fagsakService.hentFagsak(any(String.class))).thenReturn(fagsak);
        when(mottatteOpplysningerService.finnMottatteOpplysninger(behandling.getId())).thenReturn(Optional.of(lagMottatteOpplysninger()));


        List<OppgaveDto> mineSaker = oppgaveService.hentOppgaverMedAnsvarlig(TILORDNET_RESSURS);


        assertThat(mineSaker).hasSize(2);

        Optional<OppgaveDto> behOppgOpt = mineSaker.stream()
            .filter(o -> o.oppgaveID.equals(BEH_OPPG_ID))
            .findFirst();

        assertThat(behOppgOpt).isPresent().get().isInstanceOf(BehandlingsoppgaveDto.class);
        assertThat(((BehandlingsoppgaveDto) behOppgOpt.get()).behandling.behandlingID).isEqualTo(behandling.getId());
        assertThat(((BehandlingsoppgaveDto) behOppgOpt.get()).land).isNotNull();

        Optional<OppgaveDto> jfrOppgOpt = mineSaker.stream()
            .filter(o -> o.oppgaveID.equals(JFR_OPPG_ID))
            .findFirst();

        assertThat(jfrOppgOpt).isPresent().get().isInstanceOf(JournalfoeringsoppgaveDto.class);
    }

    @Test
    void hentOppgaverMedAnsvarlig_mottatteopplysningerFinnesIkke_mappesKorrekt() {
        var behandling = lagBehandling();
        var fagsak = lagFagsak(behandling);

        when(oppgaveFasade.finnOppgaverMedAnsvarlig(TILORDNET_RESSURS)).thenReturn(Set.of(oppgave));
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(fagsakService.hentFagsak(any(String.class))).thenReturn(fagsak);
        when(mottatteOpplysningerService.finnMottatteOpplysninger(behandling.getId())).thenReturn(Optional.empty());


        List<OppgaveDto> mineSaker = oppgaveService.hentOppgaverMedAnsvarlig(TILORDNET_RESSURS);


        assertThat(mineSaker).hasSize(1);

        Optional<OppgaveDto> behOppgOpt = mineSaker.stream()
            .filter(o -> o.oppgaveID.equals(BEH_OPPG_ID))
            .findFirst();

        assertThat(behOppgOpt).isPresent().get().isInstanceOf(BehandlingsoppgaveDto.class);
        assertThat(((BehandlingsoppgaveDto) behOppgOpt.get()).behandling.behandlingID).isEqualTo(behandling.getId());
        assertThat(((BehandlingsoppgaveDto) behOppgOpt.get()).land).isNull();
    }

    @Test
    void hentOppgaverMedAnsvarlig_mottatteopplysningerDataErAnmodningEllerAttest_mappesKorrekt() {
        var behandling = lagBehandling();
        var fagsak = lagFagsak(behandling);
        var mottatteOpplysninger = lagMottatteOpplysninger();
        mottatteOpplysninger.setMottatteOpplysningerData(new AnmodningEllerAttest());

        when(oppgaveFasade.finnOppgaverMedAnsvarlig(TILORDNET_RESSURS)).thenReturn(Set.of(oppgave));
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(fagsakService.hentFagsak(any(String.class))).thenReturn(fagsak);
        when(mottatteOpplysningerService.finnMottatteOpplysninger(behandling.getId())).thenReturn(Optional.of(mottatteOpplysninger));


        List<OppgaveDto> mineSaker = oppgaveService.hentOppgaverMedAnsvarlig(TILORDNET_RESSURS);


        assertThat(mineSaker).hasSize(1);

        Optional<OppgaveDto> behOppgOpt = mineSaker.stream()
            .filter(o -> o.oppgaveID.equals(BEH_OPPG_ID))
            .findFirst();

        assertThat(behOppgOpt).isPresent().get().isInstanceOf(BehandlingsoppgaveDto.class);
        assertThat(((BehandlingsoppgaveDto) behOppgOpt.get()).behandling.behandlingID).isEqualTo(behandling.getId());
    }

    @Test
    void hentOppgaverMedAnsvarlig_notaterEksisterer_forventSisteNotat() {
        var behandlingsnotat1 = new Behandlingsnotat();
        behandlingsnotat1.setRegistrertDato(Instant.now());
        behandlingsnotat1.setTekst("Test1");
        var behandlingsnotat2 = new Behandlingsnotat();
        behandlingsnotat2.setRegistrertDato(Instant.now().plusMillis(2000));
        behandlingsnotat2.setTekst("Test2");
        var behandling = lagBehandling();
        behandling.setBehandlingsnotater(Set.of(behandlingsnotat1, behandlingsnotat2));
        var fagsak = lagFagsak(behandling);

        when(oppgaveFasade.finnOppgaverMedAnsvarlig(TILORDNET_RESSURS)).thenReturn(Set.of(oppgave));
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(fagsakService.hentFagsak(any(String.class))).thenReturn(fagsak);


        List<OppgaveDto> mineSaker = oppgaveService.hentOppgaverMedAnsvarlig(TILORDNET_RESSURS);


        BehandlingsoppgaveDto behandlingsOppgave = (BehandlingsoppgaveDto) mineSaker.get(0);
        assertThat(behandlingsOppgave.sisteNotat).isEqualTo(behandlingsnotat2.getTekst());
    }

    @Test
    void hentOppgaverMedAnsvarlig_aktøridOgOrgnrErNull_forventUkjentIdOgNavn() {
        var oppgave = new Oppgave.Builder()
            .setOppgaveId(JFR_OPPG_ID)
            .setOppgavetype(Oppgavetyper.JFR)
            .setAktørId(null)
            .setOrgnr(null)
            .build();
        when(oppgaveFasade.finnOppgaverMedAnsvarlig(TILORDNET_RESSURS)).thenReturn(Set.of(oppgave));


        List<OppgaveDto> mineSaker = oppgaveService.hentOppgaverMedAnsvarlig(TILORDNET_RESSURS);


        assertThat(mineSaker).hasSize(1);

        Optional<OppgaveDto> jfrOppgDt = mineSaker.stream().findFirst();

        assertThat(jfrOppgDt).isPresent();
        OppgaveDto oppgaveDto = jfrOppgDt.get();

        assertThat(oppgaveDto.hovedpartIdent).isEqualTo("UKJENT");
        assertThat(oppgaveDto.navn).isEqualTo("UKJENT");
    }

    @Test
    void hentOppgaverMedAnsvarlig_aktørIdEksisterer_forventFnrOgSammensattNavn() {
        var oppgave = new Oppgave.Builder()
            .setOppgaveId(JFR_OPPG_ID)
            .setOppgavetype(Oppgavetyper.JFR)
            .setAktørId("1111")
            .setOrgnr(null)
            .build();

        when(oppgaveFasade.finnOppgaverMedAnsvarlig(TILORDNET_RESSURS)).thenReturn(Set.of(oppgave));
        when(persondataFasade.finnFolkeregisterident("1111")).thenReturn(Optional.of("fnr"));
        when(persondataFasade.hentSammensattNavn("fnr")).thenReturn("sammensatt navn");


        List<OppgaveDto> mineSaker = oppgaveService.hentOppgaverMedAnsvarlig(TILORDNET_RESSURS);


        assertThat(mineSaker).hasSize(1);

        Optional<OppgaveDto> jfrOppgDt = mineSaker.stream().findFirst();

        assertThat(jfrOppgDt).isPresent();
        OppgaveDto oppgaveDto = jfrOppgDt.get();

        assertThat(oppgaveDto.hovedpartIdent).isEqualTo("fnr");
        assertThat(oppgaveDto.navn).isEqualTo("sammensatt navn");
    }

    @Test
    void hentOppgaverMedAnsvarlig_orgnrEksisterer_forventOrgnrOgNavn() {
        var oppgave = new Oppgave.Builder()
            .setOppgaveId(JFR_OPPG_ID)
            .setOppgavetype(Oppgavetyper.JFR)
            .setAktivDato(null)
            .setOrgnr("2222")
            .build();
        when(oppgaveFasade.finnOppgaverMedAnsvarlig(TILORDNET_RESSURS)).thenReturn(Set.of(oppgave));
        when(eregFasade.hentOrganisasjonNavn("2222")).thenReturn("organisasjonsnavn");


        List<OppgaveDto> mineSaker = oppgaveService.hentOppgaverMedAnsvarlig(TILORDNET_RESSURS);


        assertThat(mineSaker).hasSize(1);

        Optional<OppgaveDto> jfrOppgDt = mineSaker.stream().findFirst();

        assertThat(jfrOppgDt).isPresent();
        OppgaveDto oppgaveDto = jfrOppgDt.get();

        assertThat(oppgaveDto.hovedpartIdent).isEqualTo("2222");
        assertThat(oppgaveDto.navn).isEqualTo("organisasjonsnavn");
    }

    @Test
    void hentOppgaveForFagsaksnummer_oppgaveEksisterer_forventOppgave() {
        when(oppgaveFasade.finnÅpneBehandlingsoppgaverMedSaksnummer(SAKSNUMMER)).thenReturn(List.of(oppgave));

        Oppgave oppgave = oppgaveService.hentÅpenBehandlingsoppgaveMedFagsaksnummer(SAKSNUMMER);
        assertThat(oppgave.erBehandling()).isTrue();
    }

    @Test
    void finnÅpenBehandlingsoppgaveMedFagsaksnummer_returnererOppgaveViStøtter_filtrererIkkeUtOppgave() {
        when(oppgaveFasade.finnÅpneBehandlingsoppgaverMedSaksnummer(SAKSNUMMER)).thenReturn(List.of(
            new Oppgave.Builder()
                .setTema(Tema.MED)
                .setOppgavetype(Oppgavetyper.BEH_SAK)
                .build()));

        var oppgave = oppgaveService.finnÅpenBehandlingsoppgaveMedFagsaksnummer(SAKSNUMMER);

        assertThat(oppgave).isPresent();
    }

    @Test
    void finnÅpenBehandlingsoppgaveMedFagsaksnummer_returnererTrygdeavgiftOppgave_filtrererUtOppgave() {
        when(oppgaveFasade.finnÅpneBehandlingsoppgaverMedSaksnummer(SAKSNUMMER)).thenReturn(List.of(
            new Oppgave.Builder()
                .setTema(Tema.TRY)
                .setOppgavetype(Oppgavetyper.VUR)
                .build()));

        var oppgave = oppgaveService.finnÅpenBehandlingsoppgaveMedFagsaksnummer(SAKSNUMMER);

        assertThat(oppgave).isNotPresent();
    }

    @Test
    void opprettEllerGjenbrukBehandlingsoppgave_ingenEksisterendeOppgave_oppgaveBlirOpprettet() {
        Behandling behandling = lagBehandling();
        when(behandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(utledMottaksdato.getMottaksdato(behandling)).thenReturn(LocalDate.now());


        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(behandling, "222", "333", "Z99999");


        verify(oppgaveFasade).opprettOppgave(any(Oppgave.class));
        verify(oppgaveFasade, never()).opprettSensitivOppgave(any(Oppgave.class));

    }

    @Test
    void opprettEllerGjenbrukBehandlingsoppgave_oppgaveOpprettElektroniskSøknad_oppgaveBlirOpprettetMedBeskrivelse() {
        Behandling behandling = lagBehandling();
        behandling.setMottatteOpplysninger(new MottatteOpplysninger());
        behandling.getMottatteOpplysninger().setMottatteOpplysningerData(new MottatteOpplysningerData());
        behandling.getMottatteOpplysninger().setType(Mottatteopplysningertyper.SØKNAD_A1_UTSENDTE_ARBEIDSTAKERE_EØS);
        when(behandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(utledMottaksdato.getMottaksdato(behandling)).thenReturn(LocalDate.now());


        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(behandling, "222", "333", "Z99999");


        verify(oppgaveFasade).opprettOppgave(oppgaveCaptor.capture());
        assertThat(oppgaveCaptor.getValue().getBeskrivelse()).isEqualTo(behandling.getTema().getBeskrivelse());
    }

    @Test
    void opprettEllerGjenbrukBehandlingsoppgave_oppgaveNyVurdering_oppgaveBlirOpprettetMedBeskrivelse() {
        Behandling behandling = lagBehandling();
        behandling.setType(Behandlingstyper.NY_VURDERING);
        when(behandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(utledMottaksdato.getMottaksdato(behandling)).thenReturn(LocalDate.now());


        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(behandling, "222", "333", "Z99999");


        verify(oppgaveFasade).opprettOppgave(oppgaveCaptor.capture());
        assertThat(oppgaveCaptor.getValue().getBeskrivelse()).isEqualTo(behandling.getTema().getBeskrivelse());
    }

    @Test
    void opprettEllerGjenbrukBehandlingsoppgave_oppgaveEksistererSaksbehandlerErTilordnet_oppgaveBlirIkkeOpprettetEllerOppdatert() {
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.FØRSTEGANG);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setFagsak(new Fagsak());
        behandling.getFagsak().setSaksnummer(SAKSNUMMER);
        when(oppgaveFasade.finnÅpneBehandlingsoppgaverMedSaksnummer(SAKSNUMMER)).thenReturn(List.of(oppgave));

        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(behandling, "222", "333", oppgave.getTilordnetRessurs());
        verify(oppgaveFasade, never()).opprettOppgave(any());
        verify(oppgaveFasade, never()).oppdaterOppgave(any(), any());
    }

    @Test
    void opprettEllerGjenbrukBehandlingsoppgave_oppgaveEksistererTilordnetAnnenRessurs_oppdaterTilordnetRessurs() {
        final String tilordnetRessurs = "Z12332123";
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.FØRSTEGANG);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setFagsak(new Fagsak());
        behandling.getFagsak().setSaksnummer(SAKSNUMMER);
        when(oppgaveFasade.finnÅpneBehandlingsoppgaverMedSaksnummer(SAKSNUMMER)).thenReturn(List.of(oppgave));

        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(behandling, "222", "333", tilordnetRessurs);
        verify(oppgaveFasade, never()).opprettOppgave(any());
        verify(oppgaveFasade).oppdaterOppgave(eq(oppgave.getOppgaveId()), oppgaveOppdateringCaptor.capture());
        assertThat(oppgaveOppdateringCaptor.getValue().getTilordnetRessurs()).isEqualTo(tilordnetRessurs);
    }

    @Test
    void opprettEllerGjenbrukBehandlingsoppgave_personHarBeskyttelsesbehov_sensitivOppgaveBlirOpprettet() {
        Behandling behandling = lagBehandling();
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(persondataFasade.harStrengtFortroligAdresse("aktørID")).thenReturn(true);
        when(utledMottaksdato.getMottaksdato(behandling)).thenReturn(LocalDate.now());


        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(behandling, "222", "333", "Z99999");


        verify(oppgaveFasade, never()).opprettOppgave(any(Oppgave.class));
        verify(oppgaveFasade).opprettSensitivOppgave(any(Oppgave.class));
    }

    @Test
    void opprettEllerGjenbrukBehandlingsoppgave_barnHarBeskyttelsesbehov_sensitivOppgaveBlirOpprettet() {
        Behandling behandling = lagBehandling();
        behandling.setMottatteOpplysninger(new MottatteOpplysninger());
        behandling.getMottatteOpplysninger().setMottatteOpplysningerData(new MottatteOpplysningerData());
        behandling.getMottatteOpplysninger().getMottatteOpplysningerData().personOpplysninger.setMedfolgendeFamilie(
            List.of(MedfolgendeFamilie.tilBarnFraFnrOgNavn("fnrBarn", null))
        );
        when(persondataFasade.harStrengtFortroligAdresse("aktørID")).thenReturn(false);
        when(persondataFasade.harStrengtFortroligAdresse("fnrBarn")).thenReturn(true);
        when(behandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(utledMottaksdato.getMottaksdato(behandling)).thenReturn(LocalDate.now());


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
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(oppgaveFasade.finnAvsluttetBehandlingsoppgaverMedSaksnummer(SAKSNUMMER)).thenReturn(List.of(oppgave1, oppgave2));
        when(fagsakService.hentFagsak(SAKSNUMMER)).thenReturn(fagsak);
        when(utledMottaksdato.getMottaksdato(behandling)).thenReturn(LocalDate.now());


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

        when(oppgaveFasade.finnÅpneBehandlingsoppgaverMedSaksnummer(saksnummer)).thenReturn(List.of(oppgave));
        assertThat(oppgaveService.saksbehandlerErTilordnetOppgaveForSaksnummer(saksbehandler, saksnummer)).isTrue();
    }

    @Test
    void saksbehandlerErTilordnetOppgaveForSaksnummer_erIkkeTilordnet_erIkkeSann() {
        final var saksnummer = "MEL-0";
        final var saksbehandler = "Z12111";
        final var oppgave = new Oppgave.Builder().build();

        when(oppgaveFasade.finnÅpneBehandlingsoppgaverMedSaksnummer(saksnummer)).thenReturn(List.of(oppgave));
        assertThat(oppgaveService.saksbehandlerErTilordnetOppgaveForSaksnummer(saksbehandler, saksnummer)).isFalse();
    }

    @Test
    void saksbehandlerErTilordnetOppgaveForSaksnummer_finnesIngenOppgaver_erIkkeSann() {
        final var saksnummer = "MEL-0";

        when(oppgaveFasade.finnÅpneBehandlingsoppgaverMedSaksnummer(saksnummer)).thenReturn(Collections.emptyList());
        assertThat(oppgaveService.saksbehandlerErTilordnetOppgaveForSaksnummer("Z12111", saksnummer)).isFalse();
    }

    private Fagsak lagFagsak(Behandling behandling) {
        var fagsak = new Fagsak();
        fagsak.setSaksnummer(SAKSNUMMER);
        fagsak.setType(Sakstyper.EU_EOS);
        fagsak.setStatus(Saksstatuser.OPPRETTET);
        fagsak.setBehandlinger(List.of(behandling));
        return fagsak;
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
        personDokument.setDiskresjonskode(new Diskresjonskode());
        return personDokument;
    }

    private static MottatteOpplysninger lagMottatteOpplysninger() {
        MottatteOpplysninger mottatteOpplysninger = new MottatteOpplysninger();
        mottatteOpplysninger.setMottatteOpplysningerData(lagSoeknadDokument());
        return mottatteOpplysninger;
    }

    private static Soeknad lagSoeknadDokument() {
        Soeknad soeknad = new Soeknad();
        FysiskArbeidssted fysiskArbeidssted = new FysiskArbeidssted();
        fysiskArbeidssted.getAdresse().setLandkode(new Land(Land.NORGE).getKode());
        soeknad.arbeidPaaLand.setFysiskeArbeidssteder(Collections.singletonList(fysiskArbeidssted));

        soeknad.oppholdUtland.setOppholdslandkoder(Collections.singletonList(Landkoder.NO.getKode()));
        soeknad.oppholdUtland.setOppholdsPeriode(new Periode(LocalDate.now(), LocalDate.of(2018, 12, 12)));

        soeknad.soeknadsland.getLandkoder().add(Landkoder.BE.getKode());
        return soeknad;
    }
}
