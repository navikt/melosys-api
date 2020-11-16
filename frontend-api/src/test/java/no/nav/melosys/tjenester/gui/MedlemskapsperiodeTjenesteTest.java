package no.nav.melosys.tjenester.gui;

import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.medlemskapsperiode.MedlemskapsperiodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MedlemskapsperiodeTjenesteTest {

    @Mock
    private MedlemskapsperiodeService medlemskapsperiodeService;
    @Mock
    private TilgangService tilgangService;

    private MedlemskapsperiodeTjeneste medlemskapsperiodeTjeneste;

    @BeforeEach
    void setup() {
        medlemskapsperiodeTjeneste = new MedlemskapsperiodeTjeneste(medlemskapsperiodeService, tilgangService);
    }

    @Test
    void hentMedlemskapsperioder_validerSchema() {
        //TODO
    }

    @Test
    void opprettMedlemskapsperioder_validerSchema() {
        //TODO
    }

    @Test
    void oppdaterMedlemskapsperioder_validerSchema() {
        //TODO
    }

}