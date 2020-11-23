package no.nav.melosys.integrasjon.dokgen;

import java.time.Instant;
import java.util.Set;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.dokgen.dto.DokgenDto;
import no.nav.melosys.integrasjon.dokgen.dto.SaksbehandlingstidSoknad;
import org.junit.jupiter.api.Test;

import static java.util.Collections.singleton;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.ATTEST_A1;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void feilerNårProduserbartDokumentIkkeErStøttet() {
        assertThrows(FunksjonellException.class, () ->
            dokgenMalResolver.mapBehandling(ATTEST_A1, new Behandling())
        );
    }

    @Test
    void skalMappeTilDto() throws Exception {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setSaksopplysninger(singleton(lagPersonopplysning()));
        behandling.setFagsak(lagFagsak());
        DokgenDto dokgenDto = dokgenMalResolver.mapBehandling(MELDING_FORVENTET_SAKSBEHANDLINGSTID, behandling);

        assertTrue(dokgenDto instanceof SaksbehandlingstidSoknad);
    }

    private Fagsak lagFagsak() {
        Fagsak fagsak = new Fagsak();
        fagsak.setRegistrertDato(Instant.now());
        return fagsak;
    }

    private Saksopplysning lagPersonopplysning() {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.PERSOPL);
        PersonDokument personDokument = new PersonDokument();
        personDokument.fnr = "99887766554";
        saksopplysning.setDokument(personDokument);
        return saksopplysning;
    }
}