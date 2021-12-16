package no.nav.melosys.service.dokument;

import java.util.HashSet;
import java.util.Set;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import org.junit.jupiter.api.Test;

import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static no.nav.melosys.service.dokument.DokumentproduksjonsInfoMapper.DOKUMENTMALER_PRODSATT;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class DokumentproduksjonsInfoMapperTest {
    private final FakeUnleash fakeUnleash = new FakeUnleash();

    private final DokumentproduksjonsInfoMapper dokumentproduksjonsInfoMapper = new DokumentproduksjonsInfoMapper(fakeUnleash);

    @Test
    void feilerNårMalIkkeFinnesIDokgen() {
        assertThatThrownBy(() -> dokumentproduksjonsInfoMapper.hentDokumentproduksjonsInfo(ATTEST_A1))
            .isInstanceOf(FunksjonellException.class)
            .hasMessage("ProduserbartDokument ATTEST_A1 er ikke støttet");
    }

    @Test
    void skalUtledeTilgjengeligeMaler() {
        Set<Produserbaredokumenter> produserbaredokumenter = dokumentproduksjonsInfoMapper.utledTilgjengeligeMaler();

        Set<Produserbaredokumenter> forventetProduserbaredokumenter = new HashSet<>(DOKUMENTMALER_PRODSATT);
        assertThat(produserbaredokumenter).hasSameElementsAs(forventetProduserbaredokumenter);
    }

    @Test
    void skalHenteDokumentInfo() {
        DokumentproduksjonsInfo dokumentproduksjonsInfo = dokumentproduksjonsInfoMapper.hentDokumentproduksjonsInfo(MANGELBREV_BRUKER);

        assertThat(dokumentproduksjonsInfo.dokgenMalnavn()).isEqualTo("mangelbrev_bruker");
        assertThat(dokumentproduksjonsInfo.dokumentKategoriKode()).isEqualTo("IB");
        assertThat(dokumentproduksjonsInfo.journalføringsTittel()).isEqualTo("Melding om manglende opplysninger");
    }

    @Test
    void skalHenteMalnavn() {
        String malnavn = dokumentproduksjonsInfoMapper.hentMalnavn(MELDING_FORVENTET_SAKSBEHANDLINGSTID);
        assertThat(malnavn).isEqualTo("saksbehandlingstid_soknad");
    }
}
