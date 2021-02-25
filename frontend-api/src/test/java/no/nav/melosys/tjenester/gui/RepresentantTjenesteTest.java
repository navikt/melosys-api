package no.nav.melosys.tjenester.gui;

import no.nav.melosys.domain.folketrygden.ValgtRepresentant;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.avgiftoverforing.dto.AvgiftOverforingRepresentantDataDto;
import no.nav.melosys.integrasjon.avgiftoverforing.dto.AvgiftOverforingRepresentantDto;
import no.nav.melosys.service.representant.RepresentantService;
import no.nav.melosys.tjenester.gui.dto.RepresentantDto;
import no.nav.melosys.tjenester.gui.dto.ValgtRepresentantDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RepresentantTjenesteTest extends JsonSchemaTestParent {
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
                new AvgiftOverforingRepresentantDto("id1", "navn1"),
                new AvgiftOverforingRepresentantDto("id2", "navn2")));

        var response = representantTjeneste.hentRepresentantListe().getBody();

        assertThat(response)
            .hasSize(2)
            .flatExtracting(RepresentantDto::getNummer, RepresentantDto::getNavn)
            .containsExactly("id1", "navn1", "id2", "navn2");
        validerArray(response, REPRESENTANT_LISTE_SCHEMA, log);
    }

    @Test
    void hentRepresentant_validerSchema() throws IOException, TekniskException {
        var now = LocalDate.now();

        when(representantService.hentRepresentant("id")).thenReturn(
            new AvgiftOverforingRepresentantDataDto("id", "navn", List.of("adresselinje1", "adresselinje2"), "postnummer", "telefon", "123456789", "endretAv", now));

        var response = representantTjeneste.hentRepresentant("id").getBody();

        assertThat(response).isNotNull();
        assertThat(response.getNummer()).isEqualTo("id");
        assertThat(response.getNavn()).isEqualTo("navn");
        assertThat(response.getAdresselinjer()).hasSize(2).containsExactly("adresselinje1", "adresselinje2");
        assertThat(response.getPostnummer()).isEqualTo("postnummer");
        assertThat(response.getOrgnr()).isEqualTo("123456789");
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