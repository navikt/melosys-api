package no.nav.melosys.service.registeropplysninger

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.melosys.domain.SaksopplysningType
import no.nav.melosys.exception.TekniskException
import org.junit.jupiter.api.Test

class RegisteropplysningerRequestKtTest {

    @Test
    fun valider_ingenBehandlingID_forventException() {
        val exception = shouldThrow<TekniskException> {
            RegisteropplysningerRequest.builder().build()
        }
        exception.message shouldContain "BehandlingID er påkrevd for å hente registeropplysninger"
    }

    @Test
    fun valider_ingenFnrMenPåkrevd_forventException() {
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
        exception.message shouldContain "Krever at fnr er satt ved henting av "
        exception.message shouldContain SaksopplysningType.MEDL.beskrivelse
    }

    @Test
    fun valider_ingenFnrMenIkkePåkrevd_forventFnrLikNull() {
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
    fun getOpplysningstyper_aktivPDL_dataFraTPSHentesIkke() {
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
