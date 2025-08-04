package no.nav.melosys.service.eessi

import com.fasterxml.jackson.databind.ObjectMapper
import io.getunleash.FakeUnleash
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.melosys.domain.eessi.sed.SedGrunnlagDto
import no.nav.melosys.domain.mottatteopplysninger.SedGrunnlag
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

class SedGrunnlagMapperKtTest {
    private val fakeUnleash = FakeUnleash()

    @Test
    fun mapSedGrunnlag() {
        val sedGrunnlag = SedGrunnlagMapper.tilSedGrunnlag(lagSedGrunnlag("eessi/sedGrunnlag.json"))

        sedGrunnlag.shouldNotBeNull()
        sedGrunnlag.shouldBeInstanceOf<SedGrunnlag>()

        // Test utenlandsk ident
        sedGrunnlag.personOpplysninger.utenlandskIdent
            .map { it.ident to it.landkode }
            .shouldContainExactly(listOf("15225345345" to "BG"))

        // Test fysiske arbeidssteder
        sedGrunnlag.arbeidPaaLand.fysiskeArbeidssteder
            .map { it.virksomhetNavn }
            .shouldContainExactlyInAnyOrder(
                "Testarbeidsstednavn",
                "Testarbeidsstednavn2"
            )

        // Test ekstra arbeidsgivere
        sedGrunnlag.juridiskArbeidsgiverNorge.ekstraArbeidsgivere shouldContainExactlyInAnyOrder listOf(
            "115511",
            "226622",
            "finner ikke orgnummer så vi sender uten"
        )

        // Test foretak utland
        sedGrunnlag.foretakUtland
            .map { it.orgnr }
            .shouldContainExactly(
                "923609016",
                "123321",
                "123",
                "Testselvstendignummer"
            )
    }

    @Test
    fun lagSedGrunnlagA001() {
        val sedGrunnlag = SedGrunnlagMapper.tilSedGrunnlag(lagSedGrunnlag("eessi/sedGrunnlagA001.json"))

        sedGrunnlag.shouldNotBeNull()
        sedGrunnlag.shouldBeInstanceOf<SedGrunnlag>()

        // Test utenlandsk ident
        sedGrunnlag.personOpplysninger.utenlandskIdent
            .map { it.ident to it.landkode }
            .shouldContainExactly(listOf("15225345345" to "BG"))

        // Test fysiske arbeidssteder
        sedGrunnlag.arbeidPaaLand.fysiskeArbeidssteder
            .map { it.virksomhetNavn }
            .shouldContainExactlyInAnyOrder(
                "Testarbeidsstednavn",
                "Testarbeidsstednavn2"
            )

        // Test foretak utland
        sedGrunnlag.foretakUtland
            .map { it.orgnr }
            .shouldContainExactly(
                "TestOrgnummer",
                "Testselvstendignummer"
            )
    }

    private fun lagSedGrunnlag(filename: String): SedGrunnlagDto {
        val uri = Objects.requireNonNull(javaClass.classLoader.getResource(filename)).toURI()
        val json = String(Files.readAllBytes(Paths.get(uri)))
        return ObjectMapper().readValue(json, SedGrunnlagDto::class.java)
    }
}
