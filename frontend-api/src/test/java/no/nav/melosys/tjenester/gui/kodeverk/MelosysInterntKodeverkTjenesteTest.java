package no.nav.melosys.tjenester.gui.kodeverk;

import java.io.IOException;

import no.nav.melosys.service.medlemskapsperiode.MedlemskapsperiodeService;
import no.nav.melosys.tjenester.gui.JsonSchemaTestParent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MelosysInterntKodeverkTjenesteTest extends JsonSchemaTestParent {

    private static final String INTERNT_KODEVERK_SCHEMA = "kodeverk-melosys-internt-folketrygden-schema.json";

    private MelosysInterntKodeverkTjeneste melosysInterntKodeverkTjeneste;

    @BeforeEach
    public void setup() {
        MedlemskapsperiodeService medlemskapsperiodeService = mock(MedlemskapsperiodeService.class);
        when(medlemskapsperiodeService.hentGyldigeTrygdedekninger()).thenCallRealMethod();
        melosysInterntKodeverkTjeneste = new MelosysInterntKodeverkTjeneste(medlemskapsperiodeService);
    }

    @Test
    void hentKoderTilFolketrygden_validerSchema() throws IOException {
        valider(melosysInterntKodeverkTjeneste.hentKoderTilFolketrygden().getBody(), INTERNT_KODEVERK_SCHEMA);
    }
}