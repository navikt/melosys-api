package no.nav.melosys.service.vilkaar;

import java.util.*;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.VilkaarBegrunnelse;
import no.nav.melosys.domain.VilkaarType;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.domain.begrunnelse.Artikkel12_1;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class VilkaarsresultatServiceTest {

    @Mock
    private BehandlingsresultatRepository behandlingsresultatRepo;

    @Mock
    private VilkaarsresultatRepository vilkaarsresultatRepo;

    private VilkaarsresultatService vilkaarsresultatService;

    @Before
    public void setUp() {
        vilkaarsresultatService = new VilkaarsresultatService(behandlingsresultatRepo, vilkaarsresultatRepo);
    }

    @Test
    public void hentVilkaar() {
        long behandlingID = 1L;
        List<Vilkaarsresultat> vilkaarsresultatListe = new ArrayList<>();
        Vilkaarsresultat vilkaarsresultat = new Vilkaarsresultat();
        vilkaarsresultat.setVilkaar(VilkaarType.ART12_1_FORUTGÅENDE_MEDLEMSKAP);
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
    public void registrerVilkår() throws IkkeFunnetException {
        long behandlingID = 1L;
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        when(behandlingsresultatRepo.findById(behandlingID)).thenReturn(Optional.of(behandlingsresultat));

        VilkaarDto vilkaarDto = new VilkaarDto();
        vilkaarDto.setVilkaar(VilkaarType.FO_883_2004_ART12_1.getKode());
        List<String> koder = new ArrayList<>();
        koder.add(Artikkel12_1.ERSTATTER_ANNEN.getKode());
        vilkaarDto.setBegrunnelseKoder(koder);
        vilkaarsresultatService.registrerVilkår(behandlingID, Arrays.asList(vilkaarDto));

        verify(vilkaarsresultatRepo, times(1)).deleteByBehandlingsresultat(any());
        verify(vilkaarsresultatRepo, times(1)).save(any(Vilkaarsresultat.class));
    }

    @Test(expected = IkkeFunnetException.class)
    public void registrerVilkår_resIkkeFunnet() throws IkkeFunnetException {
        long behandlingID = 1L;
        when(behandlingsresultatRepo.findById(behandlingID)).thenReturn(Optional.empty());

        VilkaarDto vilkaarDto = new VilkaarDto();
        vilkaarDto.setVilkaar(VilkaarType.FO_883_2004_ART12_1.getKode());
        List<String> koder = new ArrayList<>();
        koder.add(Artikkel12_1.ERSTATTER_ANNEN.getKode());
        vilkaarDto.setBegrunnelseKoder(koder);
        vilkaarsresultatService.registrerVilkår(behandlingID, Arrays.asList(vilkaarDto));
    }
}