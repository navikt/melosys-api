package no.nav.melosys.service.avklartefakta;

import java.util.*;
import java.util.stream.Collectors;

import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class AvklartefaktaDtoKonvertererTest {

    private AvklartefaktaDtoKonverterer avklartefaktaDtoKonverterer;

    private Avklartefakta avklartefakta;

    private AvklartefaktaDto avklartefaktaDto;

    @Before
    public void setup() {
        avklartefaktaDtoKonverterer = new AvklartefaktaDtoKonverterer();

        avklartefakta = new Avklartefakta();

        avklartefaktaDto = new AvklartefaktaDto(new ArrayList<>(Arrays.asList("Bosted")),"yrkestypevalgliste");
        avklartefaktaDto.setSubjektID("123456789");
    }

    @Test
    public void testOppdaterAvklartefaktaInnhold() {
        avklartefaktaDtoKonverterer.oppdaterAvklartefaktaFraDto(avklartefakta, avklartefaktaDto);

        assertEquals(avklartefakta.getSubjekt(), avklartefaktaDto.getSubjektID());
        assertEquals(avklartefakta.getType(), avklartefaktaDto.getAvklartefaktaType());
        assertEquals(avklartefakta.getFakta(), avklartefaktaDto.getFakta().stream().collect(Collectors.joining(" ")));
        assertEquals(avklartefakta.getBegrunnelseFritekst(), avklartefaktaDto.getBegrunnelseFritekst());
    }

    @Test
    public void testOppdaterAvklartefaktaUtenBegrunnelse() {
        avklartefaktaDtoKonverterer.oppdaterAvklartefaktaFraDto(avklartefakta, avklartefaktaDto);

        assertEquals(0, avklartefakta.getRegistreringer().size());
    }

    @Test
    public void testOppdaterAvklarteFaktaBegrunnelser() {
        avklartefaktaDto.setBegrunnelseKoder(new ArrayList<>(Arrays.asList("Opphold", "Familie")));
        avklartefaktaDtoKonverterer.oppdaterAvklartefaktaFraDto(avklartefakta, avklartefaktaDto);

        assertEquals(2, avklartefakta.getRegistreringer().size());
        avklartefakta.getRegistreringer().forEach(r -> assertFalse(r.getBegrunnelseKode().isEmpty()));
    }

    @Test
    public void testOppdaterAvklartefaktaBegrunnelseFritekst() {
        String fritekst = "Fritekst som beskriver begrunnelse";
        avklartefaktaDto.setBegrunnelseFritekst(fritekst);
        avklartefaktaDtoKonverterer.oppdaterAvklartefaktaFraDto(avklartefakta, avklartefaktaDto);

        assertEquals(0, avklartefakta.getRegistreringer().size());
    }
}
