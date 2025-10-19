package no.nav.melosys.domain.mottatteopplysninger

import no.nav.melosys.domain.MelosysTestDsl
import no.nav.melosys.domain.kodeverk.Mottatteopplysningertyper
import java.time.Instant

fun mottatteOpplysningerForTest(init: MottatteOpplysningerTestFactory.Builder.() -> Unit = {}): MottatteOpplysninger =
    MottatteOpplysningerTestFactory.Builder().apply(init).build()

fun MottatteOpplysningerTestFactory.Builder.soeknad(init: SoeknadTestFactory.Builder.() -> Unit) = apply {
    this.mottatteOpplysningerData = soeknadForTest(init)
}

fun MottatteOpplysningerTestFactory.Builder.anmodningEllerAttest(init: AnmodningEllerAttestTestFactory.Builder.() -> Unit) = apply {
    this.mottatteOpplysningerData = anmodningEllerAttestForTest(init)
}

object MottatteOpplysningerTestFactory {
    const val DEFAULT_VERSION = "1.0"
    const val DEFAULT_JSON_DATA = "{}"
    val DEFAULT_TYPE = Mottatteopplysningertyper.SØKNAD_A1_YRKESAKTIVE_EØS

    @JvmStatic
    fun builder() = Builder()

    @MelosysTestDsl
    class Builder {
        var id: Long? = null
        var versjon: String = DEFAULT_VERSION
        var registrertDato: Instant = Instant.now()
        var endretDato: Instant = Instant.now()
        var type: Mottatteopplysningertyper = DEFAULT_TYPE
        var originalData: String? = null
        var jsonData: String = DEFAULT_JSON_DATA
        var eksternReferanseID: String? = null
        var mottatteOpplysningerData: MottatteOpplysningerData? = null

        fun build(): MottatteOpplysninger = MottatteOpplysninger().apply {
            this@apply.id = this@Builder.id
            this@apply.versjon = this@Builder.versjon
            this@apply.registrertDato = this@Builder.registrertDato
            this@apply.endretDato = this@Builder.endretDato
            this@apply.type = this@Builder.type
            this@apply.originalData = this@Builder.originalData
            this@apply.jsonData = this@Builder.jsonData
            this@apply.eksternReferanseID = this@Builder.eksternReferanseID
            this@apply.mottatteOpplysningerData = this@Builder.mottatteOpplysningerData
        }
    }
}
