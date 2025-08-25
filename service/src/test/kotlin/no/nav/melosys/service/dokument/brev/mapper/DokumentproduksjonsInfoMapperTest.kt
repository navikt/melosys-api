package no.nav.melosys.service.dokument.brev.mapper

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*
import no.nav.melosys.exception.FunksjonellException
import org.junit.jupiter.api.Test

class DokumentproduksjonsInfoMapperTest {

    private val dokumentproduksjonsInfoMapper = DokumentproduksjonsInfoMapper()

    @Test
    fun `feiler når mal ikke finnes i dokgen`() {
        val exception = shouldThrow<FunksjonellException> {
            dokumentproduksjonsInfoMapper.hentDokumentproduksjonsInfo(ATTEST_A1)
        }
        exception.message shouldContain "ProduserbartDokument ATTEST_A1 er ikke støttet"
    }

    @Test
    fun `skal hente dokumentinfo`() {
        val dokumentproduksjonsInfo = dokumentproduksjonsInfoMapper.hentDokumentproduksjonsInfo(MANGELBREV_BRUKER)


        dokumentproduksjonsInfo.run {
            dokgenMalnavn shouldBe "mangelbrev_bruker"
            dokumentKategoriKode shouldBe "IB"
            journalføringsTittel shouldBe "Melding om manglende opplysninger"
        }
    }

    @Test
    fun `skal hente malnavn`() {
        val malnavn = dokumentproduksjonsInfoMapper.hentMalnavn(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)


        malnavn shouldBe "saksbehandlingstid_soknad"
    }
}
