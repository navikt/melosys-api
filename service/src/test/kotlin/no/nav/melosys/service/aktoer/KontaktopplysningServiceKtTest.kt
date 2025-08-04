package no.nav.melosys.service.aktoer

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Kontaktopplysning
import no.nav.melosys.domain.KontaktopplysningID
import no.nav.melosys.repository.KontaktopplysningRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(MockKExtension::class)
class KontaktopplysningServiceKtTest {
    @RelaxedMockK
    lateinit var kontaktopplysningRepository: KontaktopplysningRepository

    private lateinit var kontaktopplysningService: KontaktopplysningService
    private lateinit var eksisterendeKontaktopplysning: Kontaktopplysning

    @BeforeEach
    fun setUp() {
        kontaktopplysningService = KontaktopplysningService(kontaktopplysningRepository)
        eksisterendeKontaktopplysning = Kontaktopplysning().apply {
            kontaktopplysningID = KontaktopplysningID(SAK_NUMMER, ORG_NUMMER)
            kontaktNavn = "eksisterendenavn"
            kontaktOrgnr = "eksisterendeorgnr"
        }
    }

    @Test
    fun hentKontaktopplysning_kallerRepoFindById() {
        kontaktopplysningService.hentKontaktopplysning(SAK_NUMMER, ORG_NUMMER)

        verify { kontaktopplysningRepository.findById(KontaktopplysningID(SAK_NUMMER, ORG_NUMMER)) }
    }

    @Test
    fun lagEllerOppdaterKontaktopplysning_nyttObject_lagerNyttObjekt() {
        every { kontaktopplysningRepository.findById(KontaktopplysningID(SAK_NUMMER, ORG_NUMMER)) } returns Optional.empty()
        every { kontaktopplysningRepository.save(any<Kontaktopplysning>()) } answers { firstArg<Kontaktopplysning>() }

        val kontaktorgnr = "nyttkontaktorgnr"
        val kontaktnavn = "nyttkontaktnavn"
        val kontakttelefon = "nyttkontakttelefonnummer"
        val kontaktopplysning = kontaktopplysningService
            .lagEllerOppdaterKontaktopplysning(SAK_NUMMER, ORG_NUMMER, kontaktorgnr, kontaktnavn, kontakttelefon)

        kontaktopplysning shouldNotBeSameInstanceAs eksisterendeKontaktopplysning
        verify { kontaktopplysningRepository.save(kontaktopplysning) }
        kontaktopplysning.kontaktopplysningID.saksnummer shouldBe SAK_NUMMER
        kontaktopplysning.kontaktopplysningID.orgnr shouldBe ORG_NUMMER
        kontaktopplysning.kontaktNavn shouldBe kontaktnavn
        kontaktopplysning.kontaktOrgnr shouldBe kontaktorgnr
        kontaktopplysning.kontaktTelefon shouldBe kontakttelefon
    }

    @Test
    fun lagEllerOppdaterKontaktopplysning_ekisterendeObject_oppdatererObjekt() {
        every { kontaktopplysningRepository.findById(KontaktopplysningID(SAK_NUMMER, ORG_NUMMER)) } returns Optional.of(eksisterendeKontaktopplysning)
        every { kontaktopplysningRepository.save(any<Kontaktopplysning>()) } answers { firstArg<Kontaktopplysning>() }
        val kontaktorgnr = "nyttkontaktorgnr"
        val kontaktnavn = "nyttkontaktnavn"
        val kontakttelefon = "nyttkontakttelefonnummer"
        val kontaktopplysning = kontaktopplysningService
            .lagEllerOppdaterKontaktopplysning(SAK_NUMMER, ORG_NUMMER, kontaktorgnr, kontaktnavn, kontakttelefon)

        kontaktopplysning shouldBeSameInstanceAs eksisterendeKontaktopplysning
        verify { kontaktopplysningRepository.save(kontaktopplysning) }
        kontaktopplysning.kontaktopplysningID.saksnummer shouldBe SAK_NUMMER
        kontaktopplysning.kontaktopplysningID.orgnr shouldBe ORG_NUMMER
        kontaktopplysning.kontaktNavn shouldBe kontaktnavn
        kontaktopplysning.kontaktOrgnr shouldBe kontaktorgnr
        kontaktopplysning.kontaktTelefon shouldBe kontakttelefon
    }

    @Test
    fun slettKontaktopplysning_kallerDeleteByIdMedGittSaksnummerOgOrgNummer() {
        kontaktopplysningService.slettKontaktopplysning(SAK_NUMMER, ORG_NUMMER)

        verify { kontaktopplysningRepository.deleteById(KontaktopplysningID(SAK_NUMMER, ORG_NUMMER)) }
    }

    companion object {
        private const val SAK_NUMMER = "MEL-1"
        private const val ORG_NUMMER = "999"
    }
}
