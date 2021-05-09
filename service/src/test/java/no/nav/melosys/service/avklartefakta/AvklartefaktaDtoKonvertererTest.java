package no.nav.melosys.service.avklartefakta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AvklartefaktaDtoKonvertererTest {

    private AvklartefaktaDtoKonverterer avklartefaktaDtoKonverterer;

    private AvklartefaktaDto avklartefaktaDto;

    @BeforeEach
    public void setup() {
        avklartefaktaDtoKonverterer = new AvklartefaktaDtoKonverterer();
        avklartefaktaDto = new AvklartefaktaDto(new ArrayList<>(Collections.singletonList("Bosted")),"yrkestypevalgliste");
        avklartefaktaDto.setSubjektID("123456789");
    }

    @Test
    void testOppdaterAvklartefaktaInnhold() {
        Avklartefakta avklartefakta = avklartefaktaDtoKonverterer.opprettAvklartefaktaFraDto(avklartefaktaDto, null);

        assertThat(avklartefakta.getSubjekt()).isEqualTo(avklartefaktaDto.getSubjektID());
        assertThat(avklartefakta.getType()).isEqualTo(avklartefaktaDto.getAvklartefaktaType());
        assertThat(avklartefakta.getFakta()).isEqualTo(String.join(" ", avklartefaktaDto.getFakta()));
        assertThat(avklartefakta.getBegrunnelseFritekst()).isEqualTo(avklartefaktaDto.getBegrunnelseFritekst());
    }

    @Test
    void testOppdaterAvklartefaktaUtenBegrunnelse() {
        Avklartefakta avklartefakta = avklartefaktaDtoKonverterer.opprettAvklartefaktaFraDto(avklartefaktaDto, null);

        assertThat(avklartefakta.getRegistreringer()).isEmpty();
    }

    @Test
    void testOppdaterAvklarteFaktaBegrunnelser() {
        avklartefaktaDto.setBegrunnelseKoder(new ArrayList<>(Arrays.asList("Opphold", "Familie")));
        Avklartefakta avklartefakta = avklartefaktaDtoKonverterer.opprettAvklartefaktaFraDto(avklartefaktaDto, null);

        assertThat(avklartefakta.getRegistreringer()).hasSize(2);
        assertThat(avklartefakta.getRegistreringer()).noneMatch(r -> r.getBegrunnelseKode() == null);
    }

    @Test
    void testOppdaterAvklartefaktaBegrunnelseFritekst() {
        String fritekst = "Fritekst som beskriver begrunnelse";
        avklartefaktaDto.setBegrunnelseFritekst(fritekst);
        Avklartefakta avklartefakta = avklartefaktaDtoKonverterer.opprettAvklartefaktaFraDto(avklartefaktaDto, null);

        assertThat(avklartefakta.getRegistreringer()).isEmpty();
    }
}
