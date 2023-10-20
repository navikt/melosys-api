package no.nav.melosys.integrasjon.ereg

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.optional.shouldBePresent
import io.kotest.matchers.optional.shouldNotBePresent
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.domain.Saksopplysning
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.integrasjon.ereg.organisasjon.OrganisasjonResponse.*
import no.nav.melosys.integrasjon.ereg.organisasjon.OrganisasjonRestConsumer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class EregRestServiceTest {

    private val organisasjonRestConsumer = mockk<OrganisasjonRestConsumer>()
    private lateinit var eregService: EregRestService

    @BeforeEach
    fun before() {
        every { organisasjonRestConsumer.hentOrganisasjon("873102322") } returns Organisasjon(
            organisasjonsnummer = "873102322",
            organisasjonDetaljer = OrganisasjonDetaljer(
                navn = listOf(
                    Navn(
                        bruksperiode = Bruksperiode(LocalDateTime.now()),
                        gyldighetsperiode = Gyldighetsperiode(LocalDate.now()),
                        sammensattnavn = "MULTICONSULT ASA"
                    )
                )
            )
        )
        every { organisasjonRestConsumer.hentOrganisasjon("111111111") } throws IkkeFunnetException("111111111 Ikke funnet")
        eregService = EregRestService(organisasjonRestConsumer)
    }

    @Test
    fun getOrganisasjon() {
        val saksopplysning: Saksopplysning = eregService.hentOrganisasjon("873102322")

        val organisasjonDokument = saksopplysning.dokument as OrganisasjonDokument?
        organisasjonDokument.shouldNotBeNull()
            .organisasjonDetaljer.shouldNotBeNull()
            .navn.shouldNotBeNull().shouldHaveSize(1).first()
            .apply {
                navn.shouldHaveSize(1).first().shouldBe("MULTICONSULT ASA")
                redigertNavn.shouldBe("MULTICONSULT ASA")
            }
    }

    @Test
    fun finnOrganisasjon_finnerOrganisasjon_returnererMedVerdi() {
        eregService.finnOrganisasjon("873102322").shouldBePresent()
    }

    @Test
    fun finnOrganisasjon_finnerIkkeOrganisasjon_returnererTomVerdi() {
        eregService.finnOrganisasjon("111111111").shouldNotBePresent()
    }
}
