package no.nav.melosys.repository

import io.kotest.assertions.throwables.shouldThrow
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/**
 * Tester låse-protokollen i [DigitalSøknadSakLås] (MELOSYS-8151).
 */
@ExtendWith(MockKExtension::class)
internal class DigitalSøknadSakLåsTest {

    @MockK
    lateinit var lockRepository: DigitalSøknadSakLockRepository

    private lateinit var lås: DigitalSøknadSakLås

    private val aktørId = "1234567890123"

    @BeforeEach
    fun setup() {
        lås = DigitalSøknadSakLås(lockRepository)
    }

    @Test
    fun `sikrer lås-raden før radlåsen tas`() {
        // Rekkefølgen er vesentlig: taRadlås gjør SELECT ... FOR UPDATE på en rad som må finnes,
        // og feiler hardt hvis sikreLåsRad ikke har kjørt først.
        every { lockRepository.sikreLåsRad(aktørId) } just runs
        every { lockRepository.taRadlås(aktørId) } just runs

        lås.lås(aktørId)

        verifyOrder {
            lockRepository.sikreLåsRad(aktørId)
            lockRepository.taRadlås(aktørId)
        }
    }

    @Test
    fun `feil i sikreLåsRad svelges og radlåsen tas likevel`() {
        // To instanser kan opprette lås-raden samtidig; da feiler den ene med unik-feil i sin egen
        // transaksjon. Raden finnes uansett etterpå, så vi skal gå videre og ta radlåsen.
        every { lockRepository.sikreLåsRad(aktørId) } throws RuntimeException("ORA-00001: unique constraint violated")
        every { lockRepository.taRadlås(aktørId) } just runs

        lås.lås(aktørId)

        verify(exactly = 1) { lockRepository.taRadlås(aktørId) }
    }

    @Test
    fun `feil i taRadlås propagerer ut`() {
        // Uten lås må vi IKKE fortsette — da er hele poenget (atomisk sak-resolusjon) borte og vi
        // risikerer duplikate fagsaker. Feilen skal stoppe steget, ikke svelges.
        every { lockRepository.sikreLåsRad(aktørId) } just runs
        every { lockRepository.taRadlås(aktørId) } throws IllegalStateException("Lås-rad for aktørId mangler")

        shouldThrow<IllegalStateException> {
            lås.lås(aktørId)
        }
    }
}
