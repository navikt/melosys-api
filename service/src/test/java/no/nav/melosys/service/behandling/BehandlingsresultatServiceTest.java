package no.nav.melosys.service.behandling;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.kodeverk.Utfallregistreringunntak;
import no.nav.melosys.domain.kodeverk.begrunnelser.Henleggelsesgrunner;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BehandlingsresultatServiceTest {
    @Captor
    private ArgumentCaptor<Behandlingsresultat> behandlingsresultatCaptor;

    @Mock
    private BehandlingsresultatRepository behandlingsresultatRepo;

    @Mock
    private BehandlingsresultatService behandlingsresultatService;

    @Mock
    private VilkaarsresultatService vilkaarsresultatService;

    @BeforeEach
    public void setUp() {
        behandlingsresultatService = spy(new BehandlingsresultatService(behandlingsresultatRepo, vilkaarsresultatService));
    }

    @Test
    void tømBehandlingsresultat() {
        long behandlingID = 1L;
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(behandlingID);
        behandlingsresultat.setAvklartefakta(new HashSet<>(Collections.singletonList(new Avklartefakta())));
        behandlingsresultat.setLovvalgsperioder(new HashSet<>(Collections.singletonList(new Lovvalgsperiode())));
        behandlingsresultat.setVilkaarsresultater(new HashSet<>(Collections.singleton(new Vilkaarsresultat())));
        behandlingsresultat.setUtfallRegistreringUnntak(Utfallregistreringunntak.GODKJENT);
        behandlingsresultat.setInnledningFritekst("Innledning fritekst");
        behandlingsresultat.setBegrunnelseFritekst("Begrunnelse fritekst");
        behandlingsresultat.setNyVurderingBakgrunn("ny vurdering bakgrunn");
        behandlingsresultat.setTrygdeavgiftFritekst("trygdeavgift fritekst");
        Fagsak fagsak = FagsakTestFactory.lagFagsak();
        Behandling behandling = new Behandling();
        behandling.setFagsak(fagsak);
        behandling.setId(behandlingID);
        behandlingsresultat.setBehandling(behandling);

        when(behandlingsresultatRepo.findById(anyLong())).thenReturn(Optional.of(behandlingsresultat));


        behandlingsresultatService.tømBehandlingsresultat(1L);


        assertThat(behandlingsresultat.getAvklartefakta()).isEmpty();
        assertThat(behandlingsresultat.getLovvalgsperioder()).isEmpty();
        assertThat(behandlingsresultat.getUtfallRegistreringUnntak()).isNull();
        assertThat(behandlingsresultat.getInnledningFritekst()).isNull();
        assertThat(behandlingsresultat.getBegrunnelseFritekst()).isNull();
        assertThat(behandlingsresultat.getNyVurderingBakgrunn()).isNull();
        assertThat(behandlingsresultat.getTrygdeavgiftFritekst()).isNull();
        verify(vilkaarsresultatService).tømVilkårsresultatFraBehandlingsresultat(behandlingsresultat.getId());
    }

    @Test
    void hentBehandlingsresultat_medTomtResultat_forventerException() {
        when(behandlingsresultatRepo.findById(anyLong())).thenReturn(Optional.empty());
        assertThatExceptionOfType(IkkeFunnetException.class)
            .isThrownBy(() -> behandlingsresultatService.hentBehandlingsresultat(4L))
            .withMessageContaining("Kan ikke finne");
    }

    @Test
    void hentBehandlingsresultat_returnererBehandlingsresultat() {
        Behandlingsresultat resultat = new Behandlingsresultat();
        BehandlingsresultatBegrunnelse begrunnelse = new BehandlingsresultatBegrunnelse();
        begrunnelse.setKode(Henleggelsesgrunner.ANNET.getKode());
        resultat.getBehandlingsresultatBegrunnelser().add(begrunnelse);
        when(behandlingsresultatRepo.findById(anyLong())).thenReturn(Optional.of(resultat));


        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(4L);


        begrunnelse = behandlingsresultat.getBehandlingsresultatBegrunnelser().iterator().next();
        assertThat(begrunnelse.getKode()).isEqualTo(Henleggelsesgrunner.ANNET.getKode());
    }

    @Test
    void lagreNyttBehandlingsresultat_lagresKorrekt() {
        var behandling = new Behandling();


        behandlingsresultatService.lagreNyttBehandlingsresultat(behandling);


        verify(behandlingsresultatRepo).save(behandlingsresultatCaptor.capture());
        var behandlingsresultat = behandlingsresultatCaptor.getValue();
        assertThat(behandlingsresultat.getBehandling()).isEqualTo(behandling);
        assertThat(behandlingsresultat.getBehandlingsmåte()).isEqualTo(Behandlingsmaate.MANUELT);
        assertThat(behandlingsresultat.getType()).isEqualTo(Behandlingsresultattyper.IKKE_FASTSATT);
    }

    @Test
    void lagreBehandlingsResultat_godkjent_erRegistrertUnntak() {
        long behandlingID = 123;
        Utfallregistreringunntak utfallUtpeking = Utfallregistreringunntak.GODKJENT;

        Behandlingsresultat mockBehandlingsresultat = new Behandlingsresultat();
        when(behandlingsresultatRepo.findById(behandlingID)).thenReturn(Optional.of(mockBehandlingsresultat));
        when(behandlingsresultatRepo.findWithKontrollresultaterById(behandlingID)).thenReturn(Optional.of(mockBehandlingsresultat));


        behandlingsresultatService.settUtfallRegistreringUnntakOgType(behandlingID, utfallUtpeking);

        assertEquals(Behandlingsresultattyper.REGISTRERT_UNNTAK, mockBehandlingsresultat.getType());
    }

    @Test
    void lagreBehandlingsResultat_ikkeGodkjent_erFerdigbehandlet() {
        long behandlingID = 123;
        Utfallregistreringunntak utfallUtpeking = Utfallregistreringunntak.IKKE_GODKJENT;

        Behandlingsresultat mockBehandlingsresultat = new Behandlingsresultat();
        when(behandlingsresultatRepo.findById(behandlingID)).thenReturn(Optional.of(mockBehandlingsresultat));
        when(behandlingsresultatRepo.findWithKontrollresultaterById(behandlingID)).thenReturn(Optional.of(mockBehandlingsresultat));

        behandlingsresultatService.settUtfallRegistreringUnntakOgType(behandlingID, utfallUtpeking);
        assertEquals(Behandlingsresultattyper.FERDIGBEHANDLET, mockBehandlingsresultat.getType());
    }

    @Test
    void oppdaterBehandlingsresultattype_idEksisterer_oppdatererBehandlingsresultattype() {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setType(Behandlingsresultattyper.ANMODNING_OM_UNNTAK);
        doReturn(Optional.of(behandlingsresultat)).when(behandlingsresultatRepo).findById(1L);


        behandlingsresultatService.oppdaterBehandlingsresultattype(1L, Behandlingsresultattyper.IKKE_FASTSATT);


        assertThat(behandlingsresultat.getType()).isEqualTo(Behandlingsresultattyper.IKKE_FASTSATT);
        verify(behandlingsresultatRepo).save(behandlingsresultat);
    }

    @Test
    void oppdaterBehandlingsresultattype_idEksistererIkke_gjørIngenting() {
        behandlingsresultatService.oppdaterBehandlingsresultattype(1L, Behandlingsresultattyper.IKKE_FASTSATT);
        verify(behandlingsresultatRepo).findById(1L);
        verify(behandlingsresultatRepo, never()).save(any());
    }

    @Test
    void oppdaterBehandlingsmaate_bhmåteUdefinert_verifiserOppdatert() {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setBehandlingsmåte(Behandlingsmaate.MANUELT);
        when(behandlingsresultatRepo.findById(anyLong())).thenReturn(Optional.of(behandlingsresultat));


        behandlingsresultatService.oppdaterBehandlingsMaate(1L, Behandlingsmaate.AUTOMATISERT);


        verify(behandlingsresultatRepo).save(behandlingsresultat);
        assertThat(behandlingsresultat.getBehandlingsmåte()).isEqualTo(Behandlingsmaate.AUTOMATISERT);
    }

    @Test
    void settUtfallRegistreringUnntakOgType_ikkeSatt_lagres() {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        when(behandlingsresultatRepo.findById(1L)).thenReturn(Optional.of(behandlingsresultat));
        when(behandlingsresultatRepo.findWithKontrollresultaterById(1L)).thenReturn(Optional.of(behandlingsresultat));


        behandlingsresultatService.settUtfallRegistreringUnntakOgType(1, Utfallregistreringunntak.GODKJENT);


        verify(behandlingsresultatRepo).save(behandlingsresultat);
    }

    @Test
    void settUtfallRegistreringUnntakOgType_alleredeSatt_kasterException() {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();


        behandlingsresultat.setUtfallRegistreringUnntak(Utfallregistreringunntak.GODKJENT);


        when(behandlingsresultatRepo.findById(1L)).thenReturn(Optional.of(behandlingsresultat));
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> behandlingsresultatService.settUtfallRegistreringUnntakOgType(1, Utfallregistreringunntak.GODKJENT))
            .withMessageContaining("Utfall for registrering av unntak er allerede satt for behandlingsresultat");
    }

    @Test
    void oppdaterUtfallRegistreringUnntak_alleredeSatt_oppdaterer() {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setUtfallRegistreringUnntak(Utfallregistreringunntak.GODKJENT);
        when(behandlingsresultatRepo.findWithKontrollresultaterById(1L)).thenReturn(Optional.of(behandlingsresultat));


        behandlingsresultatService.oppdaterUtfallRegistreringUnntak(1, Utfallregistreringunntak.DELVIS_GODKJENT);


        verify(behandlingsresultatRepo).save(behandlingsresultat);
    }

    @Test
    void oppdaterBegrunnelser_enBegrunnelse_blirLagret() {
        var behandlingsresultatBegrunnelse = new BehandlingsresultatBegrunnelse();
        behandlingsresultatBegrunnelse.setKode("koden");

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        when(behandlingsresultatRepo.findById(1L)).thenReturn(Optional.of(behandlingsresultat));


        behandlingsresultatService.oppdaterBegrunnelser(1L, Set.of(behandlingsresultatBegrunnelse), "fri");


        verify(behandlingsresultatRepo).save(behandlingsresultat);
        assertThat(behandlingsresultatBegrunnelse.getBehandlingsresultat()).isEqualTo(behandlingsresultat);
    }

    @Test
    void oppdaterFritekster_altOk_blirLagret() {
        ArgumentCaptor<Behandlingsresultat> captor = ArgumentCaptor.forClass(Behandlingsresultat.class);
        var behandlingsresultat = new Behandlingsresultat();
        when(behandlingsresultatRepo.findById(1L)).thenReturn(Optional.of(behandlingsresultat));


        behandlingsresultatService.oppdaterFritekster(
            1L, "fritekst for begrunnelse", "fritekst for innledning", "fritekst for trygdeavgift");


        verify(behandlingsresultatRepo).save(captor.capture());
        Behandlingsresultat capturedBehandlingsresultat = captor.getValue();
        assertThat(capturedBehandlingsresultat.getBegrunnelseFritekst()).isEqualTo("fritekst for begrunnelse");
        assertThat(capturedBehandlingsresultat.getInnledningFritekst()).isEqualTo("fritekst for innledning");
        assertThat(capturedBehandlingsresultat.getTrygdeavgiftFritekst()).isEqualTo("fritekst for trygdeavgift");
    }
}
