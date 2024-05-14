package no.nav.melosys.service.sak;

import io.getunleash.FakeUnleash;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.saksflytapi.ProsessinstansService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.lovligekombinasjoner.LovligeKombinasjonerSaksbehandlingService;
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpprettBehandlingForSakTest {

    @Mock
    private ProsessinstansService prosessinstansService;
    @Mock
    private FagsakService fagsakService;
    @Mock
    private BehandlingsresultatRepository behandlingsresultatRepository;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;

    private final FakeUnleash unleash = new FakeUnleash();

    private OpprettBehandlingForSak opprettBehandlingForSak;

    @BeforeEach
    public void setUp() {
        LovligeKombinasjonerSaksbehandlingService lovligeKombinasjonerSaksbehandlingService = new LovligeKombinasjonerSaksbehandlingService(fagsakService, behandlingService, behandlingsresultatService, unleash);
        SaksbehandlingRegler saksbehandlingRegler = new SaksbehandlingRegler(behandlingsresultatRepository);
        opprettBehandlingForSak = new OpprettBehandlingForSak(fagsakService, prosessinstansService, saksbehandlingRegler, lovligeKombinasjonerSaksbehandlingService, behandlingService, behandlingsresultatService);
    }

    @Test
    void opprettBehandling_medAktivBehandling_feiler() {
        Behandling aktivBehandling = lagBehandling();
        aktivBehandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        OpprettSakDto opprettSakDto = lagOpprettSakDto();
        when(fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER)).thenReturn(lagFagsak(aktivBehandling));

        when(behandlingsresultatService.hentBehandlingsresultat(aktivBehandling.getId())).thenReturn(new Behandlingsresultat());


        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettBehandlingForSak.opprettBehandling(FagsakTestFactory.SAKSNUMMER, opprettSakDto))
            .withMessageContaining(String.format("Det finnes allerede en aktiv behandling på fagsak %s", FagsakTestFactory.SAKSNUMMER));
    }

    @Test
    void opprettBehandling_medAktivBehandlingMenArtikkel16SendtAnmodningUtland_feilerIkke() {
        Behandling aktivBehandling = lagBehandling();
        aktivBehandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        OpprettSakDto opprettSakDto = lagOpprettSakDto();
        when(fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER)).thenReturn(lagFagsak(aktivBehandling));

        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setSendtUtland(true);
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.getAnmodningsperioder().add(anmodningsperiode);
        when(behandlingsresultatService.hentBehandlingsresultat(aktivBehandling.getId())).thenReturn(behandlingsresultat);


        assertThatNoException().isThrownBy(() -> opprettBehandlingForSak.opprettBehandling(FagsakTestFactory.SAKSNUMMER, opprettSakDto));
    }


    @Test
    void opprettBehandling_utenBehandlingstema_feiler() {
        OpprettSakDto opprettSakDto = lagOpprettSakDto();
        opprettSakDto.setBehandlingstema(null);

        when(fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER)).thenReturn(lagFagsak(lagBehandling()));


        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettBehandlingForSak.opprettBehandling(FagsakTestFactory.SAKSNUMMER, opprettSakDto))
            .withMessageContaining("Behandlingstema mangler");
    }

    @Test
    void opprettBehandling_utenBehandlingstype_feiler() {
        OpprettSakDto opprettSakDto = lagOpprettSakDto();
        opprettSakDto.setBehandlingstype(null);

        when(fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER)).thenReturn(lagFagsak(lagBehandling()));


        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettBehandlingForSak.opprettBehandling(FagsakTestFactory.SAKSNUMMER, opprettSakDto))
            .withMessageContaining("Behandlingstype mangler");
    }

    @Test
    void opprettBehandling_utenMottaksdato_feiler() {
        OpprettSakDto opprettSakDto = lagOpprettSakDto();
        opprettSakDto.setMottaksdato(null);

        when(fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER)).thenReturn(lagFagsak(lagBehandling()));


        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettBehandlingForSak.opprettBehandling(FagsakTestFactory.SAKSNUMMER, opprettSakDto))
            .withMessageContaining("Mottaksdato");
    }

    @Test
    void opprettBehandling_ugyldigBehandlingstype_feiler() {
        OpprettSakDto opprettSakDto = lagOpprettSakDto();
        opprettSakDto.setBehandlingstype(Behandlingstyper.FØRSTEGANG);

        when(fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER)).thenReturn(lagFagsak(lagBehandling()));


        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettBehandlingForSak.opprettBehandling(FagsakTestFactory.SAKSNUMMER, opprettSakDto))
            .withMessageContaining("er ikke en lovlig behandlingstype med de andre valgte verdiene");
    }

    @Test
    void opprettBehandling_ugyldigBehandlingstema_feiler() {
        OpprettSakDto opprettSakDto = lagOpprettSakDto();
        opprettSakDto.setBehandlingstema(Behandlingstema.REGISTRERING_UNNTAK);

        when(fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER)).thenReturn(lagFagsak(lagBehandling()));


        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettBehandlingForSak.opprettBehandling(FagsakTestFactory.SAKSNUMMER, opprettSakDto))
            .withMessageContaining("er ikke et lovlig behandlingstema med de andre valgte verdiene");
    }

    @Test
    void opprettBehandling_fritekstMenFeilType_feiler() {
        OpprettSakDto opprettSakDto = lagOpprettSakDto();
        opprettSakDto.setBehandlingsaarsakFritekst("Fritekst");
        opprettSakDto.setBehandlingsaarsakType(Behandlingsaarsaktyper.SØKNAD);

        when(fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER)).thenReturn(lagFagsak(lagBehandling()));


        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettBehandlingForSak.opprettBehandling(FagsakTestFactory.SAKSNUMMER, opprettSakDto))
            .withMessageContaining("Kan ikke lagre fritekst som årsak når årsakstype");
    }

    @Test
    void opprettBehandling_opprettetBehandlingFårIngenFlyt_oppretterProsessSomIkkeReplikerer() {
        OpprettSakDto opprettSakDto = lagOpprettSakDto();
        opprettSakDto.setBehandlingstema(Behandlingstema.PENSJONIST);
        opprettSakDto.setBehandlingstype(Behandlingstyper.HENVENDELSE);

        when(fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER)).thenReturn(lagFagsak(lagBehandling()));


        opprettBehandlingForSak.opprettBehandling(FagsakTestFactory.SAKSNUMMER, opprettSakDto);


        verify(prosessinstansService).opprettNyBehandlingForSak(FagsakTestFactory.SAKSNUMMER, opprettSakDto.tilOpprettSakRequest());
    }

    @Test
    void opprettBehandling_eksisterendeBehandlingKanReplikeres_oppretterProsessSomReplikerer() {
        OpprettSakDto opprettSakDto = lagOpprettSakDto();
        opprettSakDto.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        opprettSakDto.setBehandlingstype(Behandlingstyper.NY_VURDERING);

        Behandling eksisterendeBehandling = lagBehandling();
        eksisterendeBehandling.setStatus(Behandlingsstatus.AVSLUTTET);

        Fagsak fagsak = lagFagsak(eksisterendeBehandling);
        eksisterendeBehandling.setFagsak(fagsak);
        when(fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER)).thenReturn(fagsak);

        Behandlingsresultat eksisterendeResultat = new Behandlingsresultat();
        eksisterendeResultat.setType(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        when(behandlingsresultatRepository.findById(eksisterendeBehandling.getId())).thenReturn(Optional.of(eksisterendeResultat));


        opprettBehandlingForSak.opprettBehandling(FagsakTestFactory.SAKSNUMMER, opprettSakDto);


        verify(prosessinstansService).opprettOgReplikerBehandlingForSak(FagsakTestFactory.SAKSNUMMER, opprettSakDto.tilOpprettSakRequest());
    }

    private OpprettSakDto lagOpprettSakDto() {
        var opprettsakdto = new OpprettSakDto();
        opprettsakdto.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        opprettsakdto.setBehandlingstype(Behandlingstyper.NY_VURDERING);
        opprettsakdto.setMottaksdato(LocalDate.now());
        opprettsakdto.setBehandlingsaarsakType(Behandlingsaarsaktyper.SØKNAD);
        return opprettsakdto;
    }

    private Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setType(Behandlingstyper.FØRSTEGANG);
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        return behandling;
    }

    private Fagsak lagFagsak(Behandling behandling) {
        Fagsak fagsak = FagsakTestFactory.builder()
            .medBruker()
            .behandlinger(behandling)
            .build();
        behandling.setFagsak(fagsak);
        return fagsak;
    }
}
