package no.nav.melosys.integrasjon.dokgen;

import java.util.Set;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import org.junit.jupiter.api.Test;

import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.ATTEST_A1;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DokgenMalResolverTest {

    private final FakeUnleash fakeUnleash = new FakeUnleash();

    private final DokgenMalResolver dokgenMalResolver = new DokgenMalResolver(fakeUnleash);

    @Test
    void skalUtledeTilgjengeligeMaler() {
        fakeUnleash.enable("melosys.brev.MELDING_FORVENTET_SAKSBEHANDLINGSTID");
        Set<Produserbaredokumenter> produserbaredokumenter = dokgenMalResolver.utledTilgjengeligeMaler();

        assertEquals(1, produserbaredokumenter.size());
        assertEquals(MELDING_FORVENTET_SAKSBEHANDLINGSTID, produserbaredokumenter.iterator().next());
    }

    @Test
    void feilerNårMalIkkeFinnesIDokgen() {
        assertThrows(FunksjonellException.class, () ->
            dokgenMalResolver.hentMalnavn(ATTEST_A1)
        );
    }

    @Test
    void skalHenteMalnavn() throws Exception {
        String malnavn = dokgenMalResolver.hentMalnavn(MELDING_FORVENTET_SAKSBEHANDLINGSTID);
        assertEquals("saksbehandlingstid_soknad", malnavn);
    }
}