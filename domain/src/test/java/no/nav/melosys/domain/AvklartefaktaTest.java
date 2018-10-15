package no.nav.melosys.domain;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class AvklartefaktaTest {

    private AvklartefaktaRegistrering lagRegistrering(String begrunnelse, Avklartefakta avklartefakta) {
        AvklartefaktaRegistrering registrering = new AvklartefaktaRegistrering();
        registrering.setBegrunnelseKode(begrunnelse);
        registrering.setAvklartefakta(avklartefakta);

        return registrering;
    }

    @Test
    public void testOppdaterMedEkstraRegistrering() {
        String opphold1 = new String("Opphold");
        String opphold2 = new String("Opphold");
        String familie = new String("Familie");

        Avklartefakta avklartefakta = new Avklartefakta();

        AvklartefaktaRegistrering førsteRegistrering = lagRegistrering(opphold1, avklartefakta);
        avklartefakta.setRegistreringer(new HashSet<>(Arrays.asList(førsteRegistrering)));

        Set<AvklartefaktaRegistrering> nyeRegistreringer = new HashSet<>();
        nyeRegistreringer.add(lagRegistrering(opphold2, avklartefakta));
        nyeRegistreringer.add(lagRegistrering(familie, avklartefakta));

        avklartefakta.oppdaterRegistreringer(nyeRegistreringer);

        assertEquals(2, avklartefakta.getRegistreringer().size());
        assertTrue(avklartefakta.getRegistreringer().contains(førsteRegistrering));
    }

    @Test
    public void equalSammeReferanse() {
        String fellesReferanse = "YRKESTYPE";

        Avklartefakta valg1 = new Avklartefakta();
        valg1.setReferanse(fellesReferanse);
        valg1.setFakta("MOTTAR_KONTANTYTELSE");

        Avklartefakta valg2 = new Avklartefakta();
        valg2.setReferanse(fellesReferanse);
        valg2.setFakta("FLYVENDE_PERSONELL");

        Set<Avklartefakta> avklartefaktas = new HashSet<>();
        avklartefaktas.add(valg1);
        avklartefaktas.add(valg2);

        assertEquals(1, avklartefaktas.size());
    }

    @Test
    public void equalSammeAvklartefaktaKodeOgReferanse() {
        AvklartefaktaType fellesType = AvklartefaktaType.BOSTEDSLAND;
        String fellesReferanse = "BOSTEDSLAND";

        Avklartefakta valg1 = new Avklartefakta();
        valg1.setReferanse(fellesReferanse);
        valg1.setType(fellesType);
        valg1.setFakta("NO");

        Avklartefakta valg2 = new Avklartefakta();
        valg2.setReferanse(fellesReferanse);
        valg2.setType(fellesType);
        valg2.setFakta("SE");

        Set<Avklartefakta> avklartefaktas = new HashSet<>();
        avklartefaktas.add(valg1);
        avklartefaktas.add(valg2);

        assertEquals(1, avklartefaktas.size());
    }

    @Test
    public void equalSammeAvklartefaktaKodeUlikReferanse() {
        AvklartefaktaType fellesType = AvklartefaktaType.BOSTEDSLAND;

        Avklartefakta valg1 = new Avklartefakta();
        valg1.setReferanse("ValgSteg1");
        valg1.setType(fellesType);
        valg1.setFakta("NO");

        Avklartefakta valg2 = new Avklartefakta();
        valg2.setReferanse("ValgSted2");
        valg2.setType(fellesType);
        valg2.setFakta("SE");

        Set<Avklartefakta> avklartefaktas = new HashSet<>();
        avklartefaktas.add(valg1);
        avklartefaktas.add(valg2);

        assertEquals(2, avklartefaktas.size());
    }

    @Test
    public void equalSammeReferanserMedUlikSubjekt() {
        String fellesReferanse = "nyeOpplysninger_inngangsVilkaar";

        Avklartefakta valg1 = new Avklartefakta();
        valg1.setReferanse(fellesReferanse);
        valg1.setSubjekt("NO");
        valg1.setFakta("Utelat");

        Avklartefakta valg2 = new Avklartefakta();
        valg2.setReferanse(fellesReferanse);
        valg2.setSubjekt("SE");
        valg2.setFakta("Inkluder");

        Set<Avklartefakta> avklartefaktas = new HashSet<>();
        avklartefaktas.add(valg1);
        avklartefaktas.add(valg2);

        assertEquals(2, avklartefaktas.size());
    }
}
