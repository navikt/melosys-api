package no.nav.melosys.domain.util;

import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LandkoderUtilsTest {

    @Test
    public void valiateLandkoderTest() throws TekniskException, IkkeFunnetException {
        assertThat(LandkoderUtils.tilIso3(Landkoder.NO.getKode())).isEqualTo(Land.NORGE);
    }
}