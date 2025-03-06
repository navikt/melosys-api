package no.nav.melosys.domain.avgift;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ÅrsavregningTest {

    @Test
    void beregnTilFaktureringsBeloepUtenTidligereFakturertBenytterTotalBeloep() {
        Årsavregning årsavregning = new Årsavregning();
        årsavregning.setNyttTotalbeloep(new BigDecimal(1000));


        årsavregning.beregnTilFaktureringsBeloep();


        assertEquals(new BigDecimal(1000), årsavregning.getTilFaktureringBeloep());
    }

    @Test
    void beregnTilFaktureringsBeloepMedTidligereFakturertBeloepFraAvgiftssystemetTrekkerFraTidligereFakturertBeloepFraAvgiftsystemet() {
        Årsavregning årsavregning = new Årsavregning();
        årsavregning.setNyttTotalbeloep(new BigDecimal(1000));
        årsavregning.setHarDeltGrunnlag(true);
        årsavregning.setTidligereFakturertBeloepAvgiftssystem(new BigDecimal(200));


        årsavregning.beregnTilFaktureringsBeloep();


        assertEquals(new BigDecimal(800), årsavregning.getTilFaktureringBeloep());
    }

    @Test
    void beregnTilFaktureringsBeloepMedTidligereFakturertBeloepTrekkerFraTidligereFakturertBeloep() {
        Årsavregning årsavregning = new Årsavregning();
        årsavregning.setNyttTotalbeloep(new BigDecimal(1000));
        årsavregning.setTidligereFakturertBeloep(new BigDecimal(200));


        årsavregning.beregnTilFaktureringsBeloep();


        assertEquals(new BigDecimal(800), årsavregning.getTilFaktureringBeloep());
    }

    @Test
    void beregnTilFaktureringsBeloepMedTidligereFakturertBeloepOgTidligereFakturertBeloepFraAvgiftssystemetTrekkerFraTidligereFakturertBeloepFraBegge() {
        Årsavregning årsavregning = new Årsavregning();
        årsavregning.setNyttTotalbeloep(new BigDecimal(1000));
        årsavregning.setHarDeltGrunnlag(true);
        årsavregning.setTidligereFakturertBeloepAvgiftssystem(new BigDecimal(200));
        årsavregning.setTidligereFakturertBeloep(new BigDecimal(200));


        årsavregning.beregnTilFaktureringsBeloep();


        assertEquals(new BigDecimal(600), årsavregning.getTilFaktureringBeloep());
    }
}
