package no.nav.melosys.service.avklartefakta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@ExtendWith(MockitoExtension.class)
public class AvklartefaktaDtoKonvertererTest {

    private AvklartefaktaDtoKonverterer avklartefaktaDtoKonverterer;

    private AvklartefaktaDto avklartefaktaDto;

    @BeforeEach
    public void setup() {
        avklartefaktaDtoKonverterer = new AvklartefaktaDtoKonverterer();

        Avklartefakta avklartefakta = new Avklartefakta();

        avklartefaktaDto = new AvklartefaktaDto(new ArrayList<>(Arrays.asList("Bosted")),"yrkestypevalgliste");
        avklartefaktaDto.setSubjektID("123456789");
    }

    @Test
    public void testOppdaterAvklartefaktaInnhold() {
        Avklartefakta avklartefakta = avklartefaktaDtoKonverterer.opprettAvklartefaktaFraDto(avklartefaktaDto, null);

        assertEquals(avklartefakta.getSubjekt(), avklartefaktaDto.getSubjektID());
        assertEquals(avklartefakta.getType(), avklartefaktaDto.getAvklartefaktaType());
        assertEquals(avklartefakta.getFakta(), avklartefaktaDto.getFakta().stream().collect(Collectors.joining(" ")));
        assertEquals(avklartefakta.getBegrunnelseFritekst(), avklartefaktaDto.getBegrunnelseFritekst());
    }

    @Test
    public void testOppdaterAvklartefaktaUtenBegrunnelse() {
        Avklartefakta avklartefakta = avklartefaktaDtoKonverterer.opprettAvklartefaktaFraDto(avklartefaktaDto, null);

        assertEquals(0, avklartefakta.getRegistreringer().size());
    }

    @Test
    public void testOppdaterAvklarteFaktaBegrunnelser() {
        avklartefaktaDto.setBegrunnelseKoder(new ArrayList<>(Arrays.asList("Opphold", "Familie")));
        Avklartefakta avklartefakta = avklartefaktaDtoKonverterer.opprettAvklartefaktaFraDto(avklartefaktaDto, null);

        assertEquals(2, avklartefakta.getRegistreringer().size());
        avklartefakta.getRegistreringer().forEach(r -> assertFalse(r.getBegrunnelseKode().isEmpty()));
    }

    @Test
    public void testOppdaterAvklartefaktaBegrunnelseFritekst() {
        String fritekst = "Fritekst som beskriver begrunnelse";
        avklartefaktaDto.setBegrunnelseFritekst(fritekst);
        Avklartefakta avklartefakta = avklartefaktaDtoKonverterer.opprettAvklartefaktaFraDto(avklartefaktaDto, null);

        assertEquals(0, avklartefakta.getRegistreringer().size());
    }
}
