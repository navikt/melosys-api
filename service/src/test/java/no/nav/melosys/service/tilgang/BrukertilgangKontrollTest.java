package no.nav.melosys.service.tilgang;

import no.nav.melosys.sikkerhet.abac.Pep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BrukertilgangKontrollTest {

    @Mock
    private Pep pep;

    private BrukertilgangKontroll brukertilgangKontroll;

    @BeforeEach
    void setup() {
        brukertilgangKontroll = new BrukertilgangKontroll(pep);
    }

    @Test
    void validerTilgangTilAktørID() {
        final var aktørID = "11111";
        brukertilgangKontroll.validerTilgangTilAktørID(aktørID);
        verify(pep).sjekkTilgangTilAktoerId(aktørID);
    }

    @Test
    void validerTilgangTilFolkeregisterIdent() {
        final var folkeregisterIdent = "123321";
        brukertilgangKontroll.validerTilgangTilFolkeregisterIdent(folkeregisterIdent);
        verify(pep).sjekkTilgangTilFnr(folkeregisterIdent);
    }

}
