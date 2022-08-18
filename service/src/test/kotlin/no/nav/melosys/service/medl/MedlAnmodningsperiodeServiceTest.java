package no.nav.melosys.service.medl;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.integrasjon.medl.MedlService;
import no.nav.melosys.integrasjon.medl.StatusaarsakMedl;
import no.nav.melosys.repository.AnmodningsperiodeRepository;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedlAnmodningsperiodeServiceTest {

    private static final long MOCKED_FORRIGE_BEHANDLING_MEDL_PERIODE_ID = 123456780L;
    @Mock
    private MedlService medlService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private AnmodningsperiodeRepository anmodningsperiodeRepository;
    private MedlAnmodningsperiodeService medlAnmodningsperiodeService;
    private Behandling nyBehandling;
    private Behandlingsresultat behandlingsresultat;
    private Fagsak fagsak;

    @BeforeEach
    void setup() {
        medlAnmodningsperiodeService = new MedlAnmodningsperiodeService(
            medlService,
            behandlingsresultatService,
            anmodningsperiodeRepository
        );

        nyBehandling = new Behandling();
        nyBehandling.setTema(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);
        nyBehandling.setRegistrertDato(Instant.now());
        fagsak = new Fagsak();
        behandlingsresultat = lagBehandlingsresultatMedAnmodningsperiode();
    }

    @Test
    void avsluttTidligereAnmodningsperiode_avslutterTidligereAnmodningsperiode() {
        fagsak.setBehandlinger(List.of(
            lagA001Behandling(1L, Instant.now().minusSeconds(5)),
            nyBehandling
        ));
        nyBehandling.setId(2L);
        nyBehandling.setFagsak(fagsak);
        when(behandlingsresultatService.hentBehandlingsresultat(1L)).thenReturn(behandlingsresultat);


        medlAnmodningsperiodeService.avsluttTidligereAnmodningsperiode(nyBehandling);


        verify(medlService).avvisPeriode(MOCKED_FORRIGE_BEHANDLING_MEDL_PERIODE_ID, StatusaarsakMedl.AVVIST);
    }

    @Test
    void avsluttTidligereAnmodningsperiode_avslutterTidligereAnmodningsperiode_medFlereTidligereBehandlinger() {
        fagsak.setBehandlinger(List.of(
            lagA001Behandling(1L, Instant.now().minusSeconds(15)),
            lagA001Behandling(2L, Instant.now().minusSeconds(10)),
            lagA001Behandling(3L, Instant.now().minusSeconds(5)),
            nyBehandling
        ));
        nyBehandling.setId(4L);
        nyBehandling.setFagsak(fagsak);
        when(behandlingsresultatService.hentBehandlingsresultat(3L)).thenReturn(behandlingsresultat);


        medlAnmodningsperiodeService.avsluttTidligereAnmodningsperiode(nyBehandling);


        verify(medlService).avvisPeriode(MOCKED_FORRIGE_BEHANDLING_MEDL_PERIODE_ID, StatusaarsakMedl.AVVIST);
    }


    private Behandlingsresultat lagBehandlingsresultatMedAnmodningsperiode() {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setMedlPeriodeID(MOCKED_FORRIGE_BEHANDLING_MEDL_PERIODE_ID);
        behandlingsresultat.setAnmodningsperioder(Set.of(anmodningsperiode));
        return behandlingsresultat;
    }

    private Behandling lagA001Behandling(Long ID, Instant registrertDato) {
        Behandling tidligereBehandling = new Behandling();
        tidligereBehandling.setId(ID);
        tidligereBehandling.setTema(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);
        tidligereBehandling.setRegistrertDato(registrertDato);
        return tidligereBehandling;
    }
}
