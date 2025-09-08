package no.nav.melosys.service.eessi

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.melosys.domain.eessi.sed.SedGrunnlagDto
import no.nav.melosys.domain.mottatteopplysninger.SedGrunnlag
import org.junit.jupiter.api.Test

class SedGrunnlagMapperTest {

    @Test
    fun `mapSedGrunnlag skal mappe SED grunnlag korrekt`() {
        val sedGrunnlag = SedGrunnlagMapper.tilSedGrunnlag(lagSedGrunnlag("eessi/sedGrunnlag.json"))


        sedGrunnlag.shouldBeInstanceOf<SedGrunnlag>().run {
            personOpplysninger.utenlandskIdent.first().run {
                listOf(ident to landkode) shouldContainExactly listOf("15225345345" to "BG")
            }

            arbeidPaaLand.fysiskeArbeidssteder
                .map { it.virksomhetNavn } shouldContainExactlyInAnyOrder listOf(
                "Testarbeidsstednavn",
                "Testarbeidsstednavn2"
            )

            juridiskArbeidsgiverNorge.ekstraArbeidsgivere shouldContainExactlyInAnyOrder listOf(
                "115511",
                "226622",
                "finner ikke orgnummer så vi sender uten"
            )

            foretakUtland
                .map { it.orgnr } shouldContainExactly listOf(
                "923609016",
                "123321",
                "123",
                "Testselvstendignummer"
            )
        }

    }

    @Test
    fun `lagSedGrunnlagA001 skal mappe A001 SED grunnlag korrekt`() {
        val sedGrunnlag = SedGrunnlagMapper.tilSedGrunnlag(lagSedGrunnlag("eessi/sedGrunnlagA001.json"))


        sedGrunnlag.shouldBeInstanceOf<SedGrunnlag>().run {
            personOpplysninger.utenlandskIdent.first().run {
                listOf(ident to landkode) shouldContainExactly listOf("15225345345" to "BG")
            }

            arbeidPaaLand.fysiskeArbeidssteder
                .map { it.virksomhetNavn } shouldContainExactlyInAnyOrder listOf(
                "Testarbeidsstednavn",
                "Testarbeidsstednavn2"
            )

            foretakUtland
                .map { it.orgnr } shouldContainExactly listOf(
                "TestOrgnummer",
                "Testselvstendignummer"
            )
        }
    }

    private fun lagSedGrunnlag(filename: String): SedGrunnlagDto =
        javaClass.classLoader.getResourceAsStream(filename)?.use { stream ->
            jacksonObjectMapper().readValue<SedGrunnlagDto>(stream)
        } ?: throw IllegalArgumentException("Resource not found: $filename")
}
