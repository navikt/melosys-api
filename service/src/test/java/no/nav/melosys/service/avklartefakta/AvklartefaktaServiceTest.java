package no.nav.melosys.service.avklartefakta;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import no.nav.melosys.domain.Avklartefakta;
import no.nav.melosys.domain.AvklartefaktaRegistrering;
import no.nav.melosys.domain.AvklartefaktaType;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.AvklarteFaktaRepository;
import no.nav.melosys.repository.BehandlingResultatRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AvklartefaktaServiceTest {

    private AvklartefaktaService avklartefaktaService;

    @Mock
    private AvklarteFaktaRepository avklarteFaktaRepository;

    @Mock
    private BehandlingResultatRepository behandlingResultatRepository;

    @Before
    public void setUp() {
        AvklartefaktaDtoKonverterer avklartefaktaDtoKonverterer = new AvklartefaktaDtoKonverterer();
        avklartefaktaService = new AvklartefaktaService(avklarteFaktaRepository, behandlingResultatRepository, avklartefaktaDtoKonverterer);
    }

    @Test
    public void hentAvklartefakta() throws IkkeFunnetException {
        String referanse = "Referenase";
        String subjektID = "SubjektID";
        String fakta = "NO";
        AvklartefaktaType type = AvklartefaktaType.BOSTEDSLAND;
        String begrunnelsekode = "Begrunnelse";
        String begrunnelsefritekst = "Fritekst";

        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setReferanse(referanse);
        avklartefakta.setSubjekt(subjektID);
        avklartefakta.setFakta(fakta);
        avklartefakta.setType(type);
        avklartefakta.setBegrunnelseFritekst(begrunnelsefritekst);

        AvklartefaktaRegistrering registrering = new AvklartefaktaRegistrering();
        registrering.setAvklartefakta(avklartefakta);
        registrering.setBegrunnelseKode(begrunnelsekode);
        avklartefakta.setRegistreringer(new HashSet<>(Arrays.asList(registrering)));
        Set<Avklartefakta> avklartefaktaSet = new HashSet<>(Arrays.asList(avklartefakta));

        when(avklarteFaktaRepository.findByBehandlingsresultatId(anyLong())).thenReturn(avklartefaktaSet);

        AvklartefaktaDto dto = avklartefaktaService.hentAvklarteFakta(1L).stream().findFirst().get();

        assertEquals(referanse, dto.getReferanse());
        assertEquals(subjektID, dto.getSubjektID());
        assertEquals(Arrays.asList(fakta), dto.getFakta());
        assertEquals(type, dto.getAvklartefaktaType());
        assertEquals(Arrays.asList(begrunnelsekode), dto.getBegrunnelseKoder());
        assertEquals(begrunnelsefritekst, dto.getBegrunnelseFritekst());
    }

    @Test
    public void lagreAvklarteFakta() throws IkkeFunnetException {

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        when(behandlingResultatRepository.findOne(anyLong())).thenReturn(behandlingsresultat);
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setFakta("test fakta");
        when(avklarteFaktaRepository.findByBehandlingsresultatAndReferanseAndSubjekt(any(), any(), any())).
            thenReturn(java.util.Optional.of(avklartefakta));

        HashSet<AvklartefaktaDto> avklartefaktaDtoer = new HashSet<>();
        avklartefaktaDtoer.add(new AvklartefaktaDto(avklartefakta));
        avklartefaktaService.lagreAvklarteFakta(123L, avklartefaktaDtoer);
        verify(avklarteFaktaRepository, times(1)).delete(anyLong());
    }
}
