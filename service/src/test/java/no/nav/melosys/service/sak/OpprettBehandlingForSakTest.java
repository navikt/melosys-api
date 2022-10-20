package no.nav.melosys.service.sak;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.service.journalforing.dto.PeriodeDto;
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpprettBehandlingForSakTest {

    public static final String SAKSNUMMER = "MEL-1";
    @Mock
    private ProsessinstansService prosessinstansService;
    @Mock
    private FagsakService fagsakService;
    @Mock
    private BehandlingsresultatRepository behandlingsresultatRepository;

    private OpprettBehandlingForSak opprettBehandlingForSak;

    private static final EasyRandom random = new EasyRandom(getRandomConfig());

    private static EasyRandomParameters getRandomConfig() {
        return new EasyRandomParameters().collectionSizeRange(1, 4)
            .randomize(PeriodeDto.class, () -> new PeriodeDto(LocalDate.now(), LocalDate.now().plusDays(1)))
            .stringLengthRange(2, 4);
    }

    @BeforeEach
    public void setUp() {
        SaksbehandlingRegler saksbehandlingRegler = new SaksbehandlingRegler(behandlingsresultatRepository);
        opprettBehandlingForSak = new OpprettBehandlingForSak(fagsakService, prosessinstansService, saksbehandlingRegler);
    }

    @Test
    void opprett_behandling_med_aktiv_behandling_feiler() {
        Behandling aktivBehandling = lagBehandling();
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);

        when(fagsakService.hentFagsak(SAKSNUMMER)).thenReturn(lagFagsak(aktivBehandling));


        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettBehandlingForSak.opprettBehandling(SAKSNUMMER, opprettSakDto))
            .withMessageContaining(String.format("Det finnes allerede en aktiv behandling på fagsak %s", SAKSNUMMER));
    }

    @Test
    void opprettBehandling_opprettetBehandlingFårTomFlyt_oppretterProsessSomIkkeReplikerer() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setBehandlingstema(Behandlingstema.YRKESAKTIV);
        opprettSakDto.setBehandlingstype(Behandlingstyper.HENVENDELSE);

        when(fagsakService.hentFagsak(SAKSNUMMER)).thenReturn(lagFagsak(null));


        opprettBehandlingForSak.opprettBehandling(SAKSNUMMER, opprettSakDto);


        verify(prosessinstansService).opprettNyBehandlingForSak(SAKSNUMMER, opprettSakDto);
    }

    @Test
    void opprettBehandling_eksisterendeBehandlingKanReplikeres_oppretterProsessSomReplikerer() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        opprettSakDto.setBehandlingstype(Behandlingstyper.NY_VURDERING);

        Behandling eksisterendeBehandling = lagBehandling();
        eksisterendeBehandling.setStatus(Behandlingsstatus.AVSLUTTET);

        Fagsak fagsak = lagFagsak(eksisterendeBehandling);
        eksisterendeBehandling.setFagsak(fagsak);
        when(fagsakService.hentFagsak(SAKSNUMMER)).thenReturn(fagsak);

        Behandlingsresultat eksisterendeResultat = new Behandlingsresultat();
        eksisterendeResultat.setType(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        when(behandlingsresultatRepository.findById(eksisterendeBehandling.getId())).thenReturn(Optional.of(eksisterendeResultat));


        opprettBehandlingForSak.opprettBehandling(SAKSNUMMER, opprettSakDto);


        verify(prosessinstansService).opprettOgReplikerBehandlingForSak(SAKSNUMMER, opprettSakDto);
    }

    private Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setType(Behandlingstyper.FØRSTEGANG);
        return behandling;
    }

    private Fagsak lagFagsak(Behandling behandling) {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(SAKSNUMMER);
        fagsak.setType(Sakstyper.EU_EOS);
        fagsak.setTema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        if (behandling != null) fagsak.getBehandlinger().add(behandling);
        return fagsak;
    }
}
