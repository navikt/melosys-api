package no.nav.melosys.service.avklartefakta;

import java.util.*;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Avklartefakta;
import no.nav.melosys.domain.AvklartefaktaRegistrering;
import no.nav.melosys.domain.AvklartefaktaType;
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
        assertEquals(avklartefakta.getAvklartefaktaType(), avklartefaktaDto.getAvklartefakta());
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
        avklartefakta.setAvklartefaktakode(type);
        avklartefakta.setBegrunnelseFritekst(begrunnelsefritekst);

        AvklartefaktaRegistrering registrering = new AvklartefaktaRegistrering();
        registrering.setAvklartefakta(avklartefakta);
        registrering.setBegrunnelseKode(begrunnelsekode);
        avklartefakta.setRegistreringer(new HashSet<>(Arrays.asList(registrering)));

        Set<Avklartefakta> avklartefaktaSet = new HashSet<>(Arrays.asList(avklartefakta));
        Set<AvklartefaktaDto> avklartefaktaDtoSet = avklartefaktaDtoKonverterer.lagDtoFraAvklartefakta(avklartefaktaSet);

        AvklartefaktaDto dto = avklartefaktaDtoSet.stream().findFirst().get();
        assertEquals(referanse, dto.getReferanse());
        assertEquals(subjektID, dto.getSubjektID());
        assertEquals(Arrays.asList(fakta), dto.getFakta());
        assertEquals(type, dto.getAvklartefakta());
        assertEquals(Arrays.asList(begrunnelsekode), dto.getBegrunnelsekoder());
        assertEquals(begrunnelsefritekst, dto.getBegrunnelsefritekst());
    }
}
