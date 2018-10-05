package no.nav.melosys.service.avklartefakta;

import no.nav.melosys.domain.Avklartefakta;
import no.nav.melosys.repository.AvklarteFaktaRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class AvklartefaktaDtoKonvertererTest {

    private AvklartefaktaDtoKonverterer avklartefaktaDtoKonverterer;

    private Avklartefakta avklartefakta;

    private AvklartefaktaDto avklartefaktaDto;

    @Mock
    private AvklarteFaktaRepository avklarteFaktaRepository;

    @Before
    public void setup() {
        avklartefaktaDtoKonverterer = new AvklartefaktaDtoKonverterer(avklarteFaktaRepository);

        avklartefakta = new Avklartefakta();
        avklartefakta.setRegistreringer(new HashSet<>());

        avklartefaktaDto = new AvklartefaktaDto(new ArrayList<>(Arrays.asList("Bosted")),"yrkestypevalgliste");
        avklartefaktaDto.setSubjektID("123456789");
    }

    @Test
    public void testOppdaterAvklartefaktaInnhold() {
        avklartefaktaDtoKonverterer.oppdaterAvklartefaktaFraDto(avklartefakta, avklartefaktaDto);

        assertEquals(avklartefakta.getSubjekt(), avklartefaktaDto.getSubjektID());
        assertEquals(avklartefakta.getAvklartefaktakode(), avklartefaktaDto.getAvklartefaktaKode());
        assertEquals(avklartefakta.getFakta(), avklartefaktaDto.getFakta().stream().collect(Collectors.joining(" ")));
    }

    @Test
    public void testOppdaterAvklartefaktaUtenBegrunnelse() {
        avklartefaktaDtoKonverterer.oppdaterAvklartefaktaFraDto(avklartefakta, avklartefaktaDto);

      //  assertTrue(avklartefakta.getRegistreringer().size() == 0);
    }

    @Test
    public void testOppdaterAvklarteFaktaBegrunnelser() {
        avklartefaktaDto.setBegrunnelsekoder(new ArrayList<>(Arrays.asList("Opphold", "Familie")));
        avklartefaktaDtoKonverterer.oppdaterAvklartefaktaFraDto(avklartefakta, avklartefaktaDto);

//        assertTrue(avklartefakta.getRegistreringer().size() == 2);
//        avklartefakta.getRegistreringer().forEach(r -> assertFalse(r.getBegrunnelseKode().isEmpty()));
//        avklartefakta.getRegistreringer().forEach(r -> assertNull(r.getBegrunnelseFritekst()));
    }

    @Test
    public void testOppdaterAvklartefaktaBegrunnelseFritekst() {
        String fritekst = "Fritekst som beskriver begrunnelse";
        avklartefaktaDto.setBegrunnelsefritekst(fritekst);
        avklartefaktaDtoKonverterer.oppdaterAvklartefaktaFraDto(avklartefakta, avklartefaktaDto);

//        assertTrue(avklartefakta.getRegistreringer().size() == 1);
//        avklartefakta.getRegistreringer().forEach(r -> assertEquals(r.getBegrunnelseFritekst(), fritekst));
//        avklartefakta.getRegistreringer().forEach(r -> assertNull(r.getBegrunnelseKode()));
    }

    @Test
    public void lagDtoFraAvklartefakta() {

    }
}
