package no.nav.melosys.service.kontroll.feature.godkjennunntak

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.Saksopplysning
import no.nav.melosys.domain.dokument.medlemskap.Periode
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.eessi.SedType
import no.nav.melosys.exception.ValideringException
import no.nav.melosys.service.kontroll.feature.unntaksperiode.UnntaksperiodeKontrollService
import no.nav.melosys.service.saksopplysninger.SaksopplysningerService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UnntaksperiodeKontrollKtTest {

    @MockK
    private lateinit var saksopplysningerService: SaksopplysningerService
    private lateinit var unntaksperiodeKontrollService: UnntaksperiodeKontrollService

    private lateinit var behandling: Behandling
    private lateinit var saksopplysning: Saksopplysning
    private lateinit var sedDokument: SedDokument

    @BeforeEach
    fun setupMedA009SedDokument() {
        MockKAnnotations.init(this)
        unntaksperiodeKontrollService = UnntaksperiodeKontrollService(saksopplysningerService)

        this.saksopplysning = Saksopplysning()

        this.behandling = Behandling.forTest {
            saksopplysninger = mutableSetOf(saksopplysning)
        }

        this.sedDokument = SedDokument().apply {
            sedType = SedType.A009
        }

        this.saksopplysning.dokument = sedDokument

        every { saksopplysningerService.finnSedOpplysninger(1L) } returns java.util.Optional.of(sedDokument)
    }

    @Test
    fun `utførKontroll A009 enTestbarGodkjennUnntakKontroll med periode på 2 år og en dag forventer ingen feil`() {
        val gyldigPeriode = Periode(
            LocalDate.of(2070, 1, 1),
            LocalDate.of(2072, 1, 1)
        )


        shouldNotThrowAny {
            unntaksperiodeKontrollService.kontrollPeriode(1L, gyldigPeriode)
        }
    }

    @Test
    fun `utførKontroll A009 enTestbarGodkjennUnntakKontroll med periode på 2 år og fem dager forventer feil`() {
        val gyldigPeriode = Periode(
            LocalDate.of(2070, 1, 1),
            LocalDate.of(2072, 1, 5)
        )


        shouldThrow<ValideringException> {
            unntaksperiodeKontrollService.kontrollPeriode(1L, gyldigPeriode)
        }.shouldBeInstanceOf<ValideringException>()
    }

    @Test
    fun `kontrollPeriode A009 med periode på 2 år og en dag forventer ingen feil`() {
        val gyldigPeriode = Periode(
            LocalDate.of(2050, 1, 1),
            LocalDate.of(2052, 1, 1)
        )


        shouldNotThrowAny {
            unntaksperiodeKontrollService.kontrollPeriode(1L, gyldigPeriode)
        }
    }

    @Test
    fun `kontrollPeriode A009 enTestbarGodkjennUnntakKontroll med periode på 2 år og fem dager forventer feil`() {
        val ugyldigPeriode = Periode(
            LocalDate.of(2050, 1, 1),
            LocalDate.of(2052, 1, 5)
        )


        shouldThrow<ValideringException> {
            unntaksperiodeKontrollService.kontrollPeriode(1L, ugyldigPeriode)
        }.shouldBeInstanceOf<ValideringException>()
    }

    @Test
    fun `kontrollPeriode A003 med periode langt over 2 år ikke relevant for A003 forventer ingen feil`() {
        val gyldigPeriode = Periode(
            LocalDate.of(2050, 1, 1),
            LocalDate.of(2055, 12, 26)
        )
        sedDokument.sedType = SedType.A003


        shouldNotThrowAny {
            unntaksperiodeKontrollService.kontrollPeriode(1L, gyldigPeriode)
        }
    }
}
