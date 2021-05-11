package no.nav.melosys.domain.saksflyt;


import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

class SedLåsReferanseTest {

    private final String rinaSaksnummer = "123";
    private final String sedID = "itoghreio";
    private final String versjon = "1";

    @Test
    void initierSedLåsReferanse_gyldigReferanseString_verifiserParsing() {
        assertThat(new SedLåsReferanse(rinaSaksnummer + "_" + sedID + "_" + versjon))
            .extracting(
                SedLåsReferanse::getReferanse,
                SedLåsReferanse::getIdentifikator)
            .containsExactly(rinaSaksnummer, sedID + "_" + versjon);
    }

    @Test
    void initierSedLåsReferanse_ugyldigReferanseString_kasterException() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> new SedLåsReferanse(rinaSaksnummer + sedID + versjon));
    }

}