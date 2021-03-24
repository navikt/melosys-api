package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.util.List;

import no.nav.melosys.domain.folketrygden.ValgtRepresentant;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.representant.RepresentantService;
import no.nav.melosys.service.representant.dto.RepresentantDataDto;
import no.nav.melosys.service.representant.dto.RepresentantDto;
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
class RepresentantTjenesteTest extends JsonSchemaTestParent {
    private static final Logger log = LoggerFactory.getLogger(RepresentantTjenesteTest.class);

    private static final String REPRESENTANT_LISTE_SCHEMA="representant-liste-schema.json";
    private static final String REPRESENTANT_SCHEMA="representant-representant-schema.json";
    private static final String VALGTREPRESENTANT_SCHEMA="representant-valgt-schema.json";
    private static final String VALGTREPRESENTANT_POST_SCHEMA="representant-valgt-post-schema.json";

    @Mock
    private RepresentantService representantService;

    private RepresentantTjeneste representantTjeneste;

    @BeforeEach
    public void setup() {
        representantTjeneste = new RepresentantTjeneste(representantService);
    }

    @Test
    void hentRepresentantListe_validerSchema() throws IOException, TekniskException {
        when(representantService.hentRepresentantListe())
            .thenReturn(List.of(
                new RepresentantDto("id1", "navn1"),
                new RepresentantDto("id2", "navn2")));

        var response = representantTjeneste.hentRepresentantListe().getBody();

        validerArray(response, REPRESENTANT_LISTE_SCHEMA, log);
    }

    @Test
    void hentRepresentant_validerSchema() throws IOException, TekniskException {
        when(representantService.hentRepresentant("id")).thenReturn(
            new RepresentantDataDto("id", "navn", List.of("adresselinje1", "adresselinje2"), "postnummer", "123456789"));

        var response = representantTjeneste.hentRepresentant("id").getBody();

        valider(response, REPRESENTANT_SCHEMA, log);
    }

    @Test
    void oppdaterValgtRepresentant_validerResponse() throws FunksjonellException, IOException, TekniskException {
        var request = new ValgtRepresentantDto("repnr", true, "123456789", "kontaktperson");

        valider(request, VALGTREPRESENTANT_POST_SCHEMA, log);

        when(representantService.oppdaterValgtRepresentant(anyLong(), any(ValgtRepresentant.class))).thenReturn(request.til());

        var response = representantTjeneste.lagreValgtRepresentant(1L, request).getBody();

        assertThat(response).isNotNull();
        assertThat(response.getRepresentantnummer()).isEqualTo(request.getRepresentantnummer());
        assertThat(response.isSelvbetalende()).isEqualTo(request.isSelvbetalende());
        assertThat(response.getOrganisasjonsnummer()).isEqualTo(request.getOrganisasjonsnummer());
        assertThat(response.getKontaktperson()).isEqualTo(request.getKontaktperson());
        valider(response, VALGTREPRESENTANT_SCHEMA, log);
    }

    @Test
    void hentValgtRepresentant_validerResponse() throws FunksjonellException, IOException, TekniskException {
        var forventetResponse = new ValgtRepresentantDto("repnr", true, "123456789", "kontaktperson");

        when(representantService.hentValgtRepresentant(anyLong())).thenReturn(forventetResponse.til());

        var response = representantTjeneste.hentValgtRepresentant(1L).getBody();

        assertThat(response).isNotNull();
        assertThat(response.getRepresentantnummer()).isEqualTo(forventetResponse.getRepresentantnummer());
        assertThat(response.isSelvbetalende()).isEqualTo(forventetResponse.isSelvbetalende());
        assertThat(response.getOrganisasjonsnummer()).isEqualTo(forventetResponse.getOrganisasjonsnummer());
        assertThat(response.getKontaktperson()).isEqualTo(forventetResponse.getKontaktperson());
        valider(response, VALGTREPRESENTANT_SCHEMA, log);
    }
}