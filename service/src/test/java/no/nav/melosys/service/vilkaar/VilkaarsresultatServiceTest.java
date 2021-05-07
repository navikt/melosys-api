package no.nav.melosys.service.vilkaar;

import java.util.*;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.VilkaarBegrunnelse;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.domain.kodeverk.begrunnelser.Art12_1_begrunnelser;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VilkaarsresultatServiceTest {
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private VilkaarsresultatRepository vilkaarsresultatRepo;

    private VilkaarsresultatService vilkaarsresultatService;

    @Before
    public void setUp() {
        vilkaarsresultatService = new VilkaarsresultatService(behandlingsresultatService, vilkaarsresultatRepo);
    }

    @Test
    public void hentVilkaar() {
        long behandlingID = 1L;
        List<Vilkaarsresultat> vilkaarsresultatListe = new ArrayList<>();
        Vilkaarsresultat vilkaarsresultat = new Vilkaarsresultat();
        vilkaarsresultat.setVilkaar(Vilkaar.ART12_1_FORUTGAAENDE_MEDLEMSKAP);
        vilkaarsresultat.setOppfylt(true);
        vilkaarsresultat.setBegrunnelseFritekst("begrunnelse");
        Set<VilkaarBegrunnelse> beggrunnelser = new HashSet<>();
        vilkaarsresultat.setBegrunnelser(beggrunnelser);
        vilkaarsresultatListe.add(vilkaarsresultat);

        when(vilkaarsresultatRepo.findByBehandlingsresultatId(behandlingID)).thenReturn(vilkaarsresultatListe);

        List<VilkaarDto> vilkaarDtoListe = vilkaarsresultatService.hentVilkaar(behandlingID);
        assertThat(vilkaarDtoListe.size()).isEqualTo(vilkaarsresultatListe.size());
        assertThat(vilkaarDtoListe.get(0).getVilkaar()).isEqualTo(vilkaarsresultatListe.get(0).getVilkaar().getKode());
    }

    @Test
    public void registrerVilkår() throws FunksjonellException {
        long behandlingID = 1L;
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingID)).thenReturn(behandlingsresultat);

        VilkaarDto vilkaarDto = new VilkaarDto();
        vilkaarDto.setVilkaar(Vilkaar.FO_883_2004_ART12_1.getKode());
        Set<String> koder = new HashSet<>();
        koder.add(Art12_1_begrunnelser.ERSTATTER_ANNEN.getKode());
        vilkaarDto.setBegrunnelseKoder(koder);
        vilkaarsresultatService.registrerVilkår(behandlingID, Collections.singletonList(vilkaarDto));

        verify(vilkaarsresultatRepo).deleteByBehandlingsresultatAndVilkaarNotIn(
            eq(behandlingsresultat), eq(Collections.singleton(Vilkaar.FO_883_2004_INNGANGSVILKAAR))
        );
        verify(vilkaarsresultatRepo).flush();
        verify(vilkaarsresultatRepo).save(any(Vilkaarsresultat.class));
    }

    @Test
    public void registrer_inngangsvilkår_feiler() {
        VilkaarDto vilkaarDto = new VilkaarDto();
        vilkaarDto.setVilkaar(Vilkaar.FO_883_2004_INNGANGSVILKAAR.getKode());
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> vilkaarsresultatService.registrerVilkår(1L, Collections.singletonList(vilkaarDto)))
            .withMessageContaining("Kan ikke endre vilkår " + Vilkaar.FO_883_2004_INNGANGSVILKAAR);
    }
}
