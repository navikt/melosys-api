package no.nav.melosys.integrasjon.dokgen;

import java.util.Set;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import org.junit.jupiter.api.Test;

import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class DokumentInfoMapperTest {
    private final FakeUnleash fakeUnleash = new FakeUnleash();

    private final DokumentInfoMapper dokumentInfoMapper = new DokumentInfoMapper(fakeUnleash);

    @Test
    void feilerNårMalIkkeFinnesIDokgen() {
        assertThatThrownBy(() -> dokumentInfoMapper.hentDokumentInfo(ATTEST_A1))
            .isInstanceOf(FunksjonellException.class)
            .hasMessage("ProduserbartDokument ATTEST_A1 er ikke støttet");
    }

    @Test
    void skalUtledeTilgjengeligeMaler() {
        fakeUnleash.enable("melosys.brev.MELDING_FORVENTET_SAKSBEHANDLINGSTID");
        Set<Produserbaredokumenter> produserbaredokumenter = dokumentInfoMapper.utledTilgjengeligeMaler();

        assertThat(produserbaredokumenter).hasSize(1);
        assertThat(produserbaredokumenter.iterator().next()).isEqualTo(MELDING_FORVENTET_SAKSBEHANDLINGSTID);
    }

    @Test
    void skalHenteDokumentInfo() throws Exception {
        DokumentInfo dokumentInfo = dokumentInfoMapper.hentDokumentInfo(MANGELBREV_BRUKER);

        assertThat(dokumentInfo.getDokgenMalnavn()).isEqualTo("mangelbrev_bruker");
        assertThat(dokumentInfo.getDokumentKategoriKode()).isEqualTo("IB");
        assertThat(dokumentInfo.getJournalføringsTittel()).isEqualTo("Melding om manglende opplysninger");
    }

    @Test
    void skalHenteMalnavn() throws Exception {
        String malnavn = dokumentInfoMapper.hentMalnavn(MELDING_FORVENTET_SAKSBEHANDLINGSTID);
        assertThat(malnavn).isEqualTo("saksbehandlingstid_soknad");
    }
}