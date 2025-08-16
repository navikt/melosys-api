package no.nav.melosys.service.registeropplysninger

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.melosys.domain.SaksopplysningType
import no.nav.melosys.exception.TekniskException
import org.junit.jupiter.api.Test

class RegisteropplysningerRequestTest {

    @Test
    fun `valider ingen behandlingID skal kaste exception`() {
        val exception = shouldThrow<TekniskException> {
            RegisteropplysningerRequest.builder().build()
        }


        exception.message shouldContain "BehandlingID er påkrevd for å hente registeropplysninger"
    }

    @Test
    fun `valider ingen fnr men påkrevd skal kaste exception`() {
        val exception = shouldThrow<TekniskException> {
            RegisteropplysningerRequest.builder()
                .behandlingID(1L)
                .saksopplysningTyper(
                    RegisteropplysningerRequest.SaksopplysningTyper.builder()
                        .medlemskapsopplysninger()
                        .organisasjonsopplysninger()
                        .build()
                )
                .build()
        }


        exception.message.shouldNotBeNull().run {
            this shouldContain "Krever at fnr er satt ved henting av "
            this shouldContain SaksopplysningType.MEDL.beskrivelse
        }
    }

    @Test
    fun `valider ingen fnr men ikke påkrevd skal ha fnr lik null`() {
        val registeropplysningerRequest = RegisteropplysningerRequest.builder()
            .behandlingID(1L)
            .saksopplysningTyper(
                RegisteropplysningerRequest.SaksopplysningTyper.builder()
                    .organisasjonsopplysninger()
                    .build()
            )
            .build()


        registeropplysningerRequest.fnr shouldBe null
    }

    @Test
    fun `getOpplysningstyper med aktiv PDL skal ikke hente data fra TPS`() {
        val registeropplysningerRequest = RegisteropplysningerRequest.builder()
            .behandlingID(1L)
            .fnr("12345678911")
            .saksopplysningTyper(
                RegisteropplysningerRequest.SaksopplysningTyper.builder()
                    .organisasjonsopplysninger()
                    .build()
            )
            .build()


        val opplysningstyper = registeropplysningerRequest.opplysningstyper


        opplysningstyper shouldBe setOf(SaksopplysningType.ORG)
    }
}
