package no.nav.melosys.saksflytapi.domain;

import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import org.assertj.core.api.Assertions;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.Test;

public class MelosysEessiMeldingTest {
    @Test
    public void testSerialisering() {
        EasyRandomParameters easyRandomParameters = new EasyRandomParameters().collectionSizeRange(1, 2).stringLengthRange(1, 4);
        MelosysEessiMelding eessiMelding = new EasyRandom(easyRandomParameters).nextObject(MelosysEessiMelding.class);
        Prosessinstans p = new Prosessinstans();
        p.setData(ProsessDataKey.EESSI_MELDING, eessiMelding);
        final MelosysEessiMelding deserialisering = p.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);
        Assertions.assertThat(deserialisering).isEqualToComparingFieldByField(eessiMelding);
    }
}
