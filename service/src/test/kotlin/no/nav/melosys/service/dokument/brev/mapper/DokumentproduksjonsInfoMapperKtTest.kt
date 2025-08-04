package no.nav.melosys.service.dokument.brev.mapper

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*
import no.nav.melosys.exception.FunksjonellException
import org.junit.jupiter.api.Test

class DokumentproduksjonsInfoMapperKtTest {
    private val dokumentproduksjonsInfoMapper = DokumentproduksjonsInfoMapper()

    @Test
    fun `feilerNårMalIkkeFinnesIDokgen`() {
        val exception = shouldThrow<FunksjonellException> {
            dokumentproduksjonsInfoMapper.hentDokumentproduksjonsInfo(ATTEST_A1)
        }
        exception.message shouldBe "ProduserbartDokument ATTEST_A1 er ikke støttet"
    }

    @Test
    fun `skalHenteDokumentInfo`() {
        val dokumentproduksjonsInfo = dokumentproduksjonsInfoMapper.hentDokumentproduksjonsInfo(MANGELBREV_BRUKER)

        dokumentproduksjonsInfo.dokgenMalnavn() shouldBe "mangelbrev_bruker"
        dokumentproduksjonsInfo.dokumentKategoriKode() shouldBe "IB"
        dokumentproduksjonsInfo.journalføringsTittel() shouldBe "Melding om manglende opplysninger"
    }

    @Test
    fun `skalHenteMalnavn`() {
        val malnavn = dokumentproduksjonsInfoMapper.hentMalnavn(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)
        malnavn shouldBe "saksbehandlingstid_soknad"
    }
}
