package no.nav.melosys.tjenester.gui;

import no.nav.melosys.domain.folketrygden.ValgtRepresentant;
import no.nav.melosys.service.representant.RepresentantService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.ValgtRepresentantDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepresentantTjenesteTest {
    private static final Logger log = LoggerFactory.getLogger(RepresentantTjenesteTest.class);

    @Mock
    private RepresentantService representantService;
    @Mock
    private Aksesskontroll aksesskontroll;

    private RepresentantTjeneste representantTjeneste;

    @BeforeEach
    public void setup() {
        representantTjeneste = new RepresentantTjeneste(representantService, aksesskontroll);
    }

    @Test
    void oppdaterValgtRepresentant_validerResponse() {
        var request = new ValgtRepresentantDto("repnr", true, "123456789", "kontaktperson");
        when(representantService.oppdaterValgtRepresentant(anyLong(), any(ValgtRepresentant.class))).thenReturn(request.til());
        var response = representantTjeneste.lagreValgtRepresentant(1L, request).getBody();

        assertThat(response).isNotNull();
        assertThat(response.representantnummer()).isEqualTo(request.representantnummer());
        assertThat(response.selvbetalende()).isEqualTo(request.selvbetalende());
        assertThat(response.organisasjonsnummer()).isEqualTo(request.organisasjonsnummer());
        assertThat(response.kontaktperson()).isEqualTo(request.kontaktperson());
    }

    @Test
    void hentValgtRepresentant_validerResponse() {
        var forventetResponse = new ValgtRepresentantDto("repnr", true, "123456789", "kontaktperson");

        when(representantService.hentValgtRepresentant(anyLong())).thenReturn(forventetResponse.til());

        var response = representantTjeneste.hentValgtRepresentant(1L).getBody();

        assertThat(response).isNotNull();
        assertThat(response.representantnummer()).isEqualTo(forventetResponse.representantnummer());
        assertThat(response.selvbetalende()).isEqualTo(forventetResponse.selvbetalende());
        assertThat(response.organisasjonsnummer()).isEqualTo(forventetResponse.organisasjonsnummer());
        assertThat(response.kontaktperson()).isEqualTo(forventetResponse.kontaktperson());
    }
}
