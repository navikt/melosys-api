package no.nav.melosys.domain.util;

import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class LovvalgBestemmelseUtilsTest {

    @Test
    void dbDataTilLovvalgBestemmelseIkkeFunnet() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> LovvalgBestemmelseUtils.dbDataTilLovvalgBestemmelse("test"))
            .withMessage("Lovvalgbestemmelse kode:test ikke funnet");
    }

    @Test
    void dbDataTilLovvalgBestemmelse() {
        LovvalgBestemmelse lovvalgBestemmelse = LovvalgBestemmelseUtils.dbDataTilLovvalgBestemmelse("UK_ART7");
        assertThat(lovvalgBestemmelse.getKode()).isEqualTo("UK_ART7");
        assertThat(lovvalgBestemmelse).isInstanceOf(Lovvalgbestemmelser_trygdeavtale_uk.class);
    }
}
