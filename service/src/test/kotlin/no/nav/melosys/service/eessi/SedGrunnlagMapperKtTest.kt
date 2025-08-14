package no.nav.melosys.service.eessi

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.melosys.domain.eessi.sed.SedGrunnlagDto
import no.nav.melosys.domain.mottatteopplysninger.SedGrunnlag
import org.junit.jupiter.api.Test
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

class SedGrunnlagMapperKtTest {

    @Test
    fun `mapSedGrunnlag skal mappe SED grunnlag korrekt`() {
        val sedGrunnlag = SedGrunnlagMapper.tilSedGrunnlag(lagSedGrunnlag("eessi/sedGrunnlag.json"))


        sedGrunnlag.shouldBeInstanceOf<SedGrunnlag>()

        sedGrunnlag.personOpplysninger.utenlandskIdent.first().run {
            listOf(ident to landkode) shouldContainExactly listOf("15225345345" to "BG")
        }

        sedGrunnlag.arbeidPaaLand.fysiskeArbeidssteder
            .map { it.virksomhetNavn } shouldContainExactlyInAnyOrder listOf(
                "Testarbeidsstednavn",
                "Testarbeidsstednavn2"
            )

        sedGrunnlag.juridiskArbeidsgiverNorge.ekstraArbeidsgivere shouldContainExactlyInAnyOrder listOf(
            "115511",
            "226622",
            "finner ikke orgnummer så vi sender uten"
        )

        sedGrunnlag.foretakUtland
            .map { it.orgnr } shouldContainExactly listOf(
                "923609016",
                "123321",
                "123",
                "Testselvstendignummer"
            )
    }

    @Test
    fun `lagSedGrunnlagA001 skal mappe A001 SED grunnlag korrekt`() {
        val sedGrunnlag = SedGrunnlagMapper.tilSedGrunnlag(lagSedGrunnlag("eessi/sedGrunnlagA001.json"))


        sedGrunnlag.shouldBeInstanceOf<SedGrunnlag>()

        sedGrunnlag.personOpplysninger.utenlandskIdent.first().run {
            listOf(ident to landkode) shouldContainExactly listOf("15225345345" to "BG")
        }

        sedGrunnlag.arbeidPaaLand.fysiskeArbeidssteder
            .map { it.virksomhetNavn } shouldContainExactlyInAnyOrder listOf(
                "Testarbeidsstednavn",
                "Testarbeidsstednavn2"
            )

        sedGrunnlag.foretakUtland
            .map { it.orgnr } shouldContainExactly listOf(
                "TestOrgnummer",
                "Testselvstendignummer"
            )
    }

    private fun lagSedGrunnlag(filename: String): SedGrunnlagDto {
        val uri: URI = Objects.requireNonNull(javaClass.classLoader.getResource(filename)).toURI()
        val json = String(Files.readAllBytes(Paths.get(uri)))
        
        
        return ObjectMapper().readValue(json, SedGrunnlagDto::class.java)
    }
}
