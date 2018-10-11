package no.nav.melosys.service.avklartefakta;

import java.util.*;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Avklartefakta;
import no.nav.melosys.domain.AvklartefaktaRegistrering;
import no.nav.melosys.repository.AvklarteFaktaRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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

        avklartefaktaDto = new AvklartefaktaDto(new ArrayList<>(Arrays.asList("Bosted")),"yrkestypevalgliste");
        avklartefaktaDto.setSubjektID("123456789");
    }

    @Test
    public void testOppdaterAvklartefaktaInnhold() {
        avklartefaktaDtoKonverterer.oppdaterAvklartefaktaFraDto(avklartefakta, avklartefaktaDto);

        assertEquals(avklartefakta.getSubjekt(), avklartefaktaDto.getSubjektID());
        assertEquals(avklartefakta.getAvklartefaktakode(), avklartefaktaDto.getAvklartefaktaKode());
        assertEquals(avklartefakta.getFakta(), avklartefaktaDto.getFakta().stream().collect(Collectors.joining(" ")));
        assertEquals(avklartefakta.getBegrunnelseFritekst(), avklartefaktaDto.getBegrunnelsefritekst());
    }

    @Test
    public void testOppdaterAvklartefaktaUtenBegrunnelse() {
        avklartefaktaDtoKonverterer.oppdaterAvklartefaktaFraDto(avklartefakta, avklartefaktaDto);

        assertTrue(avklartefakta.getRegistreringer().size() == 0);
    }

    @Test
    public void testOppdaterAvklarteFaktaBegrunnelser() {
        avklartefaktaDto.setBegrunnelsekoder(new ArrayList<>(Arrays.asList("Opphold", "Familie")));
        avklartefaktaDtoKonverterer.oppdaterAvklartefaktaFraDto(avklartefakta, avklartefaktaDto);

        assertTrue(avklartefakta.getRegistreringer().size() == 2);
        avklartefakta.getRegistreringer().forEach(r -> assertFalse(r.getBegrunnelseKode().isEmpty()));
    }

    @Test
    public void testOppdaterAvklartefaktaBegrunnelseFritekst() {
        String fritekst = "Fritekst som beskriver begrunnelse";
        avklartefaktaDto.setBegrunnelsefritekst(fritekst);
        avklartefaktaDtoKonverterer.oppdaterAvklartefaktaFraDto(avklartefakta, avklartefaktaDto);

        avklartefaktaDto.setBegrunnelsefritekst("Begrunnelsefritekst");
        assertTrue(avklartefakta.getRegistreringer().size() == 0);
        avklartefakta.getRegistreringer().forEach(r -> assertNull(r.getBegrunnelseKode()));
    }

    @Test
    public void testOppdaterMedEkstraRegistrering() throws NoSuchFieldException, IllegalAccessException {
        String opphold1 = new String("Opphold");
        String opphold2 = new String("Opphold");
        String familie = new String("Familie");

        Avklartefakta avklartefakta = new Avklartefakta();
        AvklartefaktaRegistrering registrering = new AvklartefaktaRegistrering();
        registrering.setBegrunnelseKode(opphold1);
        registrering.setAvklartefakta(avklartefakta);

        avklartefakta.setRegistreringer(new HashSet<>(Arrays.asList(registrering)));

        avklartefaktaDto.setBegrunnelsekoder(new ArrayList<>(Arrays.asList(opphold2, familie)));
        avklartefaktaDtoKonverterer.oppdaterAvklartefaktaFraDto(avklartefakta, avklartefaktaDto);

        assertTrue(avklartefakta.getRegistreringer().size() == 2);
        assertTrue(avklartefakta.getRegistreringer().contains(registrering));
    }

    @Test
    public void lagDtoFraAvklartefakta() {

    }
}
