package no.nav.melosys.service.avklartefakta;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import no.nav.melosys.domain.YrkesgruppeType;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.avklartefakta.AvklartefaktaRegistrering;
import no.nav.melosys.domain.avklartefakta.AvklartefaktaType;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.AvklarteFaktaRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AvklartefaktaServiceTest {

    @InjectMocks
    private AvklartefaktaService avklartefaktaService;

    @Mock
    private AvklarteFaktaRepository avklarteFaktaRepository;

    @Test
    public void hentAvklartefakta() {
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

        AvklartefaktaDto dto = avklartefaktaService.hentAlleAvklarteFakta(1L).stream().findFirst().get();

        assertEquals(referanse, dto.getReferanse());
        assertEquals(subjektID, dto.getSubjektID());
        assertEquals(Arrays.asList(fakta), dto.getFakta());
        assertEquals(type, dto.getAvklartefaktaType());
        assertEquals(Arrays.asList(begrunnelsekode), dto.getBegrunnelseKoder());
        assertEquals(begrunnelsefritekst, dto.getBegrunnelseFritekst());
    }

    @Test
    public void testYrkesgruppeOrdinær() throws TekniskException {
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setFakta("YRKESAKTIV");
        Optional<Avklartefakta> avklartefaktaSet = Optional.ofNullable(avklartefakta);
        when(avklarteFaktaRepository.findByBehandlingsresultatIdAndType(anyLong(), any())).thenReturn(avklartefaktaSet);

        YrkesgruppeType yrkesgruppeType = avklartefaktaService.hentYrkesGruppe(1L);
        assertThat(yrkesgruppeType).isEqualTo(YrkesgruppeType.ORDINAER);
    }

    @Test
    public void testYrkesgruppeFlyvende() throws TekniskException {
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setFakta("YRKESAKTIV_FLYVENDE");
        Optional<Avklartefakta> avklartefaktaSet = Optional.ofNullable(avklartefakta);
        when(avklarteFaktaRepository.findByBehandlingsresultatIdAndType(anyLong(), any())).thenReturn(avklartefaktaSet);

        YrkesgruppeType yrkesgruppeType = avklartefaktaService.hentYrkesGruppe(1L);
        assertThat(yrkesgruppeType).isEqualTo(YrkesgruppeType.FLYENDE_PERSONELL);
    }
}
