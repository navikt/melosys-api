package no.nav.melosys.domain

import no.nav.melosys.domain.dokument.PersonDokumentTestFactory
import no.nav.melosys.domain.dokument.SaksopplysningDokument
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.kodeverk.Landkoder
import java.time.Instant

fun saksopplysningForTest(init: SaksopplysningTestFactory.Builder.() -> Unit = {}): Saksopplysning =
    SaksopplysningTestFactory.Builder().apply(init).build()

fun SaksopplysningTestFactory.Builder.personDokument(init: PersonDokumentTestFactory.Builder.() -> Unit) = apply {
    this.dokument = PersonDokumentTestFactory.Builder().apply(init).build()
}

fun SaksopplysningTestFactory.Builder.organisasjonDokument(init: OrganisasjonDokumentTestFactory.Builder.() -> Unit) = apply {
    this.dokument = OrganisasjonDokumentTestFactory.builder().apply(init).build()
}

fun SaksopplysningTestFactory.Builder.sedDokument(init: SedDokumentBuilder.() -> Unit) = apply {
    this.dokument = SedDokumentBuilder().apply(init).build()
}

@MelosysTestDsl
class SedDokumentBuilder {
    var avsenderLandkode: Landkoder? = null
    var rinaSaksnummer: String? = null
    var rinaDokumentID: String? = null
    var fnr: String? = null

    fun build(): SedDokument = SedDokument().apply {
        this.avsenderLandkode = this@SedDokumentBuilder.avsenderLandkode
        this.rinaSaksnummer = this@SedDokumentBuilder.rinaSaksnummer
        this.rinaDokumentID = this@SedDokumentBuilder.rinaDokumentID
        this.fnr = this@SedDokumentBuilder.fnr
    }
}

object SaksopplysningTestFactory {
    const val DEFAULT_VERSJON = "1.0"

    @MelosysTestDsl
    class Builder {
        var behandling: Behandling? = null
        var type: SaksopplysningType = SaksopplysningType.PDL_PERSOPL
        var versjon: String = DEFAULT_VERSJON
        var dokument: SaksopplysningDokument = PersonDokument()
        var registrertDato: Instant = Instant.now()
        var endretDato: Instant = Instant.now()

        fun build(): Saksopplysning = Saksopplysning().apply {
            this.behandling = this@Builder.behandling ?: Behandling.forTest {}
            this.type = this@Builder.type
            this.versjon = this@Builder.versjon
            this.dokument = this@Builder.dokument
            this.registrertDato = this@Builder.registrertDato
            this.endretDato = this@Builder.endretDato
        }
    }
}
