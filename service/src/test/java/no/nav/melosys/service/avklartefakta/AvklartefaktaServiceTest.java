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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AvklartefaktaServiceTest {

    @InjectMocks
    private AvklartefaktaService avklartefaktaService;

    @Mock
    private AvklarteFaktaRepository avklarteFaktaRepository;

    @Mock
    private BehandlingResultatRepository behandlingResultatRepository;

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

        when(behandlingResultatRepository.findOne(anyLong())).thenReturn(new Behandlingsresultat());
        when(avklarteFaktaRepository.findByBehandlingsresultatId(anyLong())).thenReturn(avklartefaktaSet);

        AvklartefaktaDto dto = avklartefaktaService.hentAvklarteFakta(1L).stream().findFirst().get();

        assertEquals(referanse, dto.getReferanse());
        assertEquals(subjektID, dto.getSubjektID());
        assertEquals(Arrays.asList(fakta), dto.getFakta());
        assertEquals(type, dto.getAvklartefaktaType());
        assertEquals(Arrays.asList(begrunnelsekode), dto.getBegrunnelseKoder());
        assertEquals(begrunnelsefritekst, dto.getBegrunnelseFritekst());
    }
}
