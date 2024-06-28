package no.nav.melosys.itest

import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.RegistreringsInfo
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.service.aktoer.AktoerDto
import no.nav.melosys.service.aktoer.AktoerHistorikkService
import no.nav.melosys.service.aktoer.AktoerService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

import java.time.Instant
import kotlin.test.assertTrue

class AktoerHistorikkServiceIT(
    @Autowired val aktoerHistorikkService: AktoerHistorikkService,
    @Autowired val aktoerService: AktoerService,
    @Autowired val fagsakRepository: FagsakRepository,
    @Autowired val transactionHelper: TestWithTransactionHelper,
) : ComponentTestBase() {


    @Test
    fun testAktoerHistorikk() {
        val fagSak = lagFagsak()
        val aktoerDto = lagAktoerDto()
        val revisionCount = aktoerHistorikkService.hentAktørHistorikk(fagSak, Aktoersroller.BRUKER).size

        aktoerService.lagEllerOppdaterAktoer(fagSak, aktoerDto).also {
            val revisionCountAfterAktoerCreation = aktoerHistorikkService.hentAktørHistorikk(fagSak, Aktoersroller.BRUKER).size
            assertTrue { revisionCount + 1 == revisionCountAfterAktoerCreation }

            addCleanUpAction {
                ryddDbEtterTest(it, fagSak.saksnummer)
            }
        }
    }

    private fun ryddDbEtterTest(id: Long, saksnummer: String) {
        val deleteFullmaktQuery = TestWithTransactionHelper.Q(
            "DELETE FROM FULLMAKT WHERE AKTOER_ID = :aktoerId",
            arrayOf(Pair("aktoerId", id))
        )

        val deleteAktoerQuery = TestWithTransactionHelper.Q(
            "DELETE FROM AKTOER WHERE ID = :id",
            arrayOf(Pair("id", id))
        )

        val deleteFagsakQUery =
            TestWithTransactionHelper.Q(
                "DELETE FROM FAGSAK WHERE SAKSNUMMER = :saksnummer",
                arrayOf(Pair("saksnummer", saksnummer))
            )

        transactionHelper.execute(deleteFullmaktQuery, deleteAktoerQuery, deleteFagsakQUery)
    }

    private fun lagAktoerDto(): AktoerDto {
        val aktoerDto = AktoerDto()
        aktoerDto.rolleKode = "BRUKER"
        aktoerDto.institusjonsID = "institusjonsID"
        aktoerDto.utenlandskPersonID = "utenlandskPersonID"
        aktoerDto.orgnr = "orgnr"
        aktoerDto.personIdent = "21075114491"
        aktoerDto.fullmakter = setOf(Fullmaktstype.FULLMEKTIG_SØKNAD)
        return aktoerDto
    }

    private fun lagFagsak(): Fagsak {
        return Fagsak(
            "MEL-aktoerhistorikk", null, Sakstyper.EU_EOS, Sakstemaer.MEDLEMSKAP_LOVVALG, Saksstatuser.OPPRETTET
        ).apply { leggTilRegisteringInfo() }.also { fagsakRepository.save(it) }
    }

    private fun RegistreringsInfo.leggTilRegisteringInfo() {
        registrertDato = Instant.now()
        endretDato = Instant.now()
        endretAv = "bla"
    }
}
