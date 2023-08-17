package no.nav.melosys.service.dokument.brev.mapper;

import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.dokument.DokumentproduksjonsInfo;
import org.junit.jupiter.api.Test;

import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class DokumentproduksjonsInfoMapperTest {
    private final DokumentproduksjonsInfoMapper dokumentproduksjonsInfoMapper = new DokumentproduksjonsInfoMapper();

    @Test
    void feilerNårMalIkkeFinnesIDokgen() {
        assertThatThrownBy(() -> dokumentproduksjonsInfoMapper.hentDokumentproduksjonsInfo(ATTEST_A1))
            .isInstanceOf(FunksjonellException.class)
            .hasMessage("ProduserbartDokument ATTEST_A1 er ikke støttet");
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
        String malnavn = dokumentproduksjonsInfoMapper.hentMalnavn(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD);
        assertThat(malnavn).isEqualTo("saksbehandlingstid_soknad");
    }
}
