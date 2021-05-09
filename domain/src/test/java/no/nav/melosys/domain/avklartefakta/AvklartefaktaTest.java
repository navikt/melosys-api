package no.nav.melosys.domain.avklartefakta;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AvklartefaktaTest {

    private AvklartefaktaRegistrering lagRegistrering(String begrunnelse, Avklartefakta avklartefakta) {
        AvklartefaktaRegistrering registrering = new AvklartefaktaRegistrering();
        registrering.setBegrunnelseKode(begrunnelse);
        registrering.setAvklartefakta(avklartefakta);

        return registrering;
    }

    @Test
    void testOppdaterMedEkstraRegistrering() {
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

        assertThat(avklartefakta.getRegistreringer()).hasSize(2);
        assertThat(avklartefakta.getRegistreringer()).contains(førsteRegistrering);
    }
}
