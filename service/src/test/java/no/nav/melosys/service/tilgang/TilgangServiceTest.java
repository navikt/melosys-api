package no.nav.melosys.service.tilgang;

import no.nav.melosys.sikkerhet.abac.Pep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TilgangServiceTest {

    @Mock
    private Pep pep;

    private TilgangService tilgangService;

    @BeforeEach
    void setup() {
        tilgangService = new TilgangService(pep);
    }

    @Test
    void validerTilgangTilAktørID() {
        final var aktørID = "11111";
        tilgangService.validerTilgangTilAktørID(aktørID);
        verify(pep).sjekkTilgangTilAktoerId(aktørID);
    }

    @Test
    void validerTilgangTilFolkeregisterIdent() {
        final var folkeregisterIdent = "123321";
        tilgangService.validerTilgangTilFolkeregisterIdent(folkeregisterIdent);
        verify(pep).sjekkTilgangTilFnr(folkeregisterIdent);
    }

}
