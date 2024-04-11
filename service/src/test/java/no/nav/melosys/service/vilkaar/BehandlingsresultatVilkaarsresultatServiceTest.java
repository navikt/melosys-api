package no.nav.melosys.service.vilkaar;

import java.util.*;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.domain.kodeverk.begrunnelser.Art12_1_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.service.behandling.BehandlingsresultatVilkaarsresultatService;
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BehandlingsresultatVilkaarsresultatServiceTest {
    @Mock
    private SaksbehandlingRegler saksbehandlingRegler;
    @Mock
    private BehandlingsresultatRepository behandlingsresultatRepo;

    private BehandlingsresultatVilkaarsresultatService behandlingsresultatVilkaarsresultatService;

    @BeforeEach
    public void setUp() {
        behandlingsresultatVilkaarsresultatService = new BehandlingsresultatVilkaarsresultatService(behandlingsresultatRepo, saksbehandlingRegler);
    }

    @Test
    void hentVilkaar() {
        long behandlingID = 1L;
        List<Vilkaarsresultat> vilkaarsresultatListe = new ArrayList<>();
        Vilkaarsresultat vilkaarsresultat = new Vilkaarsresultat();
        vilkaarsresultat.setVilkaar(Vilkaar.ART12_1_FORUTGAAENDE_MEDLEMSKAP);
        vilkaarsresultat.setOppfylt(true);
        vilkaarsresultat.setBegrunnelseFritekst("begrunnelse");
        Set<VilkaarBegrunnelse> beggrunnelser = new HashSet<>();
        vilkaarsresultat.setBegrunnelser(beggrunnelser);
        vilkaarsresultatListe.add(vilkaarsresultat);
        Behandlingsresultat behandlingsresultat = opprettBehandlingsresultatMedBehandling();
        behandlingsresultat.setVilkaarsresultater(new HashSet<>(vilkaarsresultatListe));
        when(behandlingsresultatRepo.findById(behandlingID)).thenReturn(Optional.of(behandlingsresultat));


        List<VilkaarDto> vilkaarDtoListe = behandlingsresultatVilkaarsresultatService.hentVilkaar(behandlingID);


        assertThat(vilkaarDtoListe).hasSize(vilkaarsresultatListe.size());
        assertThat(vilkaarDtoListe.get(0).getVilkaar()).isEqualTo(vilkaarsresultatListe.get(0).getVilkaar().getKode());
    }

    @Test
    void registrerVilkår() {
        long behandlingID = 1L;
        Behandlingsresultat behandlingsresultat = opprettBehandlingsresultatMedBehandling();
        when(behandlingsresultatRepo.findById(behandlingID)).thenReturn(Optional.of(behandlingsresultat));

        VilkaarDto vilkaarDto = new VilkaarDto();
        vilkaarDto.setVilkaar(Vilkaar.FO_883_2004_ART12_1.getKode());
        Set<String> koder = new HashSet<>();
        koder.add(Art12_1_begrunnelser.ERSTATTER_ANNEN.getKode());
        vilkaarDto.setBegrunnelseKoder(koder);


        behandlingsresultatVilkaarsresultatService.registrerVilkår(behandlingID, Collections.singletonList(vilkaarDto));

        verify(behandlingsresultatRepo).save(behandlingsresultat);
        assertThat(behandlingsresultat.getVilkaarsresultater()).hasSize(1);
        Vilkaarsresultat vilkaarsresultat = behandlingsresultat.getVilkaarsresultater().iterator().next();
        assertThat(vilkaarsresultat.getVilkaar()).isEqualTo(Vilkaar.FO_883_2004_ART12_1);
        assertThat(vilkaarsresultat.getBegrunnelser()).hasSize(1);
        assertThat(vilkaarsresultat.getBegrunnelser().iterator().next().getKode()).isEqualTo(Art12_1_begrunnelser.ERSTATTER_ANNEN.getKode());
    }


    @Test
    void registrer_inngangsvilkår_feiler() {
        VilkaarDto vilkaarDto = new VilkaarDto();
        vilkaarDto.setVilkaar(Vilkaar.FO_883_2004_INNGANGSVILKAAR.getKode());


        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> behandlingsresultatVilkaarsresultatService.registrerVilkår(1L, Collections.singletonList(vilkaarDto)))
            .withMessageContaining("Kan ikke endre vilkår " + Vilkaar.FO_883_2004_INNGANGSVILKAAR);
    }


    @Test
    void tømVilkårsresultatFraBehandlingsresultat_sakstypeIkkeEøs_sletterAlleVilkår() {
        long behandlingID = 1L;
        var fagsak = new Fagsak();
        fagsak.setType(Sakstyper.FTRL);
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(behandlingID);
        behandlingsresultat.setBehandling(new Behandling());
        behandlingsresultat.getBehandling().setFagsak(fagsak);
        Vilkaarsresultat vilkaarsresultat = new Vilkaarsresultat();
        vilkaarsresultat.setId(1L);
        behandlingsresultat.setVilkaarsresultater(new HashSet<>(Collections.singleton(vilkaarsresultat)));
        when(behandlingsresultatRepo.findById(behandlingID)).thenReturn(Optional.of(behandlingsresultat));

        behandlingsresultatVilkaarsresultatService.tømVilkårsresultatFraBehandlingsresultat(behandlingID);

        verify(behandlingsresultatRepo).saveAndFlush(behandlingsresultat);
        assertThat(behandlingsresultat.getVilkaarsresultater()).isEmpty();
    }

    @Test
    void tømVilkårsresultatFraBehandlingsresultat_sakstypeEøsMenIngenFlyt_sletterAlleVilkår() {
        long behandlingID = 1L;
        var fagsak = new Fagsak();
        fagsak.setType(Sakstyper.EU_EOS);
        Behandling behandling = new Behandling();
        behandling.setId(behandlingID);
        behandling.setFagsak(fagsak);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setType(Behandlingstyper.HENVENDELSE);
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(behandlingID);
        behandlingsresultat.setBehandling(new Behandling());
        behandlingsresultat.getBehandling().setFagsak(fagsak);
        when(saksbehandlingRegler.harIngenFlyt(any())).thenReturn(true);
        when(behandlingsresultatRepo.findById(behandlingID)).thenReturn(Optional.of(behandlingsresultat));

        behandlingsresultatVilkaarsresultatService.tømVilkårsresultatFraBehandlingsresultat(behandlingID);

        verify(behandlingsresultatRepo).saveAndFlush(behandlingsresultat);
        assertThat(behandlingsresultat.getVilkaarsresultater()).isEmpty();
    }

    @Test
    void tømVilkårsresultatFraBehandlingsresultat_sakstypeEøsOgHarFlyt_sletterIkkeInngangsvilkår() {
        long behandlingID = 1L;
        var fagsak = new Fagsak();
        fagsak.setType(Sakstyper.EU_EOS);
        fagsak.setTema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        var behandling = new Behandling();
        behandling.setFagsak(fagsak);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setType(Behandlingstyper.FØRSTEGANG);
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(behandlingID);
        behandlingsresultat.setBehandling(behandling);

        Vilkaarsresultat vilkaarsresultat = new Vilkaarsresultat();
        vilkaarsresultat.setId(1L);
        vilkaarsresultat.setVilkaar(Vilkaar.FO_883_2004_INNGANGSVILKAAR);
        Vilkaarsresultat vilkaarsresultatSomSkalSlettes = new Vilkaarsresultat();
        vilkaarsresultatSomSkalSlettes.setId(2L);
        vilkaarsresultatSomSkalSlettes.setVilkaar(Vilkaar.FO_883_2004_ART12_1);
        behandlingsresultat.setVilkaarsresultater(new HashSet<>(Set.of(vilkaarsresultat, vilkaarsresultatSomSkalSlettes)));
        when(saksbehandlingRegler.harIngenFlyt(behandling)).thenReturn(false);
        when(behandlingsresultatRepo.findById(behandlingID)).thenReturn(Optional.of(behandlingsresultat));


        behandlingsresultatVilkaarsresultatService.tømVilkårsresultatFraBehandlingsresultat(behandlingID);


        verify(behandlingsresultatRepo).saveAndFlush(behandlingsresultat);
        assertThat(behandlingsresultat.getVilkaarsresultater()).containsExactly(vilkaarsresultat);
    }

    private Behandlingsresultat opprettBehandlingsresultatMedBehandling() {
        var behandlingsresultat = new Behandlingsresultat();
        var behandling = new Behandling();
        var fagsak = new Fagsak();
        behandling.setFagsak(fagsak);
        behandlingsresultat.setBehandling(behandling);
        behandlingsresultat.setId(1L);
        return behandlingsresultat;
    }
}
