package no.nav.melosys.tjenester.gui;

import no.nav.melosys.domain.folketrygden.ValgtRepresentant;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.avgiftoverforing.dto.AvgiftOverforingRepresentantDataDto;
import no.nav.melosys.integrasjon.avgiftoverforing.dto.AvgiftOverforingRepresentantDto;
import no.nav.melosys.service.representant.RepresentantService;
import no.nav.melosys.tjenester.gui.dto.ValgtRepresentantDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RepresentantTjenesteTest extends JsonSchemaTestParent {

    @Mock
    private RepresentantService representantService;

    private RepresentantTjeneste representantTjeneste;

    @BeforeEach
    public void setup() {
        representantTjeneste = new RepresentantTjeneste(representantService);
    }

    @Test
    void hentRepresentantListe_validerSchema() {
        when(representantService.hentRepresentantListe())
            .thenReturn(List.of(
                new AvgiftOverforingRepresentantDto("id1", "navn1"),
                new AvgiftOverforingRepresentantDto("id2", "navn2")));

        var response = representantTjeneste.hentRepresentantListe().getBody();

        assertThat(response).isNotNull();
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.get(0).getNummer()).isEqualTo("id1");
        assertThat(response.get(0).getNavn()).isEqualTo("navn1");
        assertThat(response.get(1).getNummer()).isEqualTo("id2");
        assertThat(response.get(1).getNavn()).isEqualTo("navn2");
        //TODO valider schema
    }

    @Test
    void hentRepresentant_validerSchema() {
        var now = LocalDate.now();

        when(representantService.hentRepresentant("id")).thenReturn(
            new AvgiftOverforingRepresentantDataDto("id", "navn", List.of("adresselinje1", "adresselinje2"), "postnummer", "telefon", "orgnr", "endretAv", now));

        var response = representantTjeneste.hentRepresentant("id").getBody();

        assertThat(response).isNotNull();
        assertThat(response.getNummer()).isEqualTo("id");
        assertThat(response.getNavn()).isEqualTo("navn");
        assertThat(response.getAdresselinjer().size()).isEqualTo(2);
        assertThat(response.getAdresselinjer().get(0)).isEqualTo("adresselinje1");
        assertThat(response.getAdresselinjer().get(1)).isEqualTo("adresselinje2");
        assertThat(response.getPostnummer()).isEqualTo("postnummer");
        assertThat(response.getTelefon()).isEqualTo("telefon");
        assertThat(response.getOrgnr()).isEqualTo("orgnr");
        assertThat(response.getEndretAv()).isEqualTo("endretAv");
        assertThat(response.getEndretDato()).isEqualTo(now);
        //TODO valider schema
    }

    @Test
    void oppdaterValgtRepresentant_validerResponse() throws FunksjonellException {
        var forventetResponse = new ValgtRepresentantDto("repnr", true, "orgnr", "kontaktperson");

        when(representantService.oppdaterValgtRepresentant(anyLong(), any(ValgtRepresentant.class))).thenReturn(forventetResponse.til());

        var response = representantTjeneste.lagreValgtRepresentant(1L, forventetResponse).getBody();

        assertThat(response).isNotNull();
        assertThat(response.getRepresentantnummer()).isEqualTo(forventetResponse.getRepresentantnummer());
        assertThat(response.isSelvbetalende()).isEqualTo(forventetResponse.isSelvbetalende());
        assertThat(response.getOrganisasjonsnummer()).isEqualTo(forventetResponse.getOrganisasjonsnummer());
        assertThat(response.getKontaktperson()).isEqualTo(forventetResponse.getKontaktperson());
        //TODO valider schema
    }

    @Test
    void hentValgtRepresentant_validerResponse() throws FunksjonellException {
        var forventetResponse = new ValgtRepresentantDto("repnr", true, "orgnr", "kontaktperson");

        when(representantService.hentValgtRepresentant(anyLong())).thenReturn(forventetResponse.til());

        var response = representantTjeneste.hentValgtRepresentant(1L).getBody();

        assertThat(response).isNotNull();
        assertThat(response.getRepresentantnummer()).isEqualTo(forventetResponse.getRepresentantnummer());
        assertThat(response.isSelvbetalende()).isEqualTo(forventetResponse.isSelvbetalende());
        assertThat(response.getOrganisasjonsnummer()).isEqualTo(forventetResponse.getOrganisasjonsnummer());
        assertThat(response.getKontaktperson()).isEqualTo(forventetResponse.getKontaktperson());
        //TODO valider schema
    }


}