package no.nav.melosys.domain;

import java.time.LocalDate;

import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PeriodeOmLovvalgTest {

    private static final long MOCK_MEDL_PERIODE_ID = 1L;

    @Test
    void harForskjelligMedlID_medLikMedlID_girFalse() {
        PeriodeOmLovvalg periodeOmLovvalg = new PeriodeOmLovvalgMock();
        assertFalse(periodeOmLovvalg.harForskjelligMedlID(MOCK_MEDL_PERIODE_ID));
    }

    @Test
    void harForskjelligMedlID_medForskjelligMedlID_girTrue() {
        PeriodeOmLovvalg periodeOmLovvalg = new PeriodeOmLovvalgMock();
        assertTrue(periodeOmLovvalg.harForskjelligMedlID(1234L));
    }

    class PeriodeOmLovvalgMock implements PeriodeOmLovvalg {

        @Override
        public Long getMedlPeriodeID() {
            return MOCK_MEDL_PERIODE_ID;
        }

        @Override
        public LocalDate getFom() {
            return null;
        }

        @Override
        public LocalDate getTom() {
            return null;
        }

        @Override
        public LovvalgBestemmelse getBestemmelse() {
            return null;
        }

        @Override
        public Landkoder getLovvalgsland() {
            return null;
        }

        @Override
        public LovvalgBestemmelse getTilleggsbestemmelse() {
            return null;
        }

        @Override
        public Trygdedekninger getDekning() {
            return null;
        }
    }
}
