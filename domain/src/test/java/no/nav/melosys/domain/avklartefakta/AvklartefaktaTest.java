package no.nav.melosys.domain.avklartefakta;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;
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
        String opphold1 = "Opphold";
        String opphold2 = "Opphold";
        String familie = "Familie";

        Avklartefakta avklartefakta = new Avklartefakta();

        AvklartefaktaRegistrering førsteRegistrering = lagRegistrering(opphold1, avklartefakta);
        avklartefakta.setRegistreringer(new HashSet<>(Collections.singletonList(førsteRegistrering)));

        Set<AvklartefaktaRegistrering> nyeRegistreringer = new HashSet<>();
        nyeRegistreringer.add(lagRegistrering(opphold2, avklartefakta));
        nyeRegistreringer.add(lagRegistrering(familie, avklartefakta));

        avklartefakta.oppdaterRegistreringer(nyeRegistreringer);

        assertEquals(2, avklartefakta.getRegistreringer().size());
        assertTrue(avklartefakta.getRegistreringer().contains(førsteRegistrering));
    }
}
