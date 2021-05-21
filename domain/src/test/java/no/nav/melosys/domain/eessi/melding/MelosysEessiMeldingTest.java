package no.nav.melosys.domain.eessi.melding;

import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MelosysEessiMeldingTest {
    @Test
    public void testSerialisering() {
        EasyRandomParameters easyRandomParameters = new EasyRandomParameters().collectionSizeRange(1, 2).stringLengthRange(1,4);
        MelosysEessiMelding eessiMelding = new EasyRandom(easyRandomParameters).nextObject(MelosysEessiMelding.class);
        Prosessinstans p = new Prosessinstans();
        p.setData(ProsessDataKey.EESSI_MELDING, eessiMelding);
        final MelosysEessiMelding deserialisering = p.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);
        assertThat(deserialisering).isEqualToComparingFieldByField(eessiMelding);
    }
}
