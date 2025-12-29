package no.nav.melosys.domain

import no.nav.melosys.domain.dokument.PersonDokumentTestFactory
import no.nav.melosys.domain.dokument.SaksopplysningDokument
import no.nav.melosys.domain.dokument.arbeidsforhold.Aktoertype
import no.nav.melosys.domain.dokument.arbeidsforhold.Arbeidsforhold
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument
import no.nav.melosys.domain.dokument.felles.Periode
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

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

fun SaksopplysningTestFactory.Builder.arbeidsforholdDokument(init: ArbeidsforholdDokumentBuilder.() -> Unit) = apply {
    this.dokument = ArbeidsforholdDokumentBuilder().apply(init).build()
}

@MelosysTestDsl
class ArbeidsforholdDokumentBuilder {
    // For single arbeidsforhold (simple pattern)
    var arbeidsforholdID: String? = null
    var arbeidsforholdIDnav: Long = 0
    var ansettelsesPeriode: Periode? = null
    var arbeidsforholdstype: String? = null
    var arbeidsgivertype: Aktoertype? = null
    var arbeidsgiverID: String? = null
    var arbeidstakerID: String? = null
    var opplysningspliktigtype: Aktoertype? = null
    var opplysningspliktigID: String? = null
    var arbeidsforholdInnrapportertEtterAOrdningen: Boolean? = null
    var opprettelsestidspunkt: OffsetDateTime? = null
    var sistBekreftet: OffsetDateTime? = null

    // For multiple arbeidsforhold
    private val arbeidsforholdListe = mutableListOf<Arbeidsforhold>()

    fun ansettelsesPeriode(fom: LocalDate, tom: LocalDate? = null) {
        ansettelsesPeriode = Periode(fom, tom)
    }

    fun arbeidsforhold(init: ArbeidsforholdBuilder.() -> Unit) {
        arbeidsforholdListe.add(ArbeidsforholdBuilder().apply(init).build())
    }

    fun build(): ArbeidsforholdDokument = ArbeidsforholdDokument(
        if (arbeidsforholdListe.isNotEmpty()) {
            arbeidsforholdListe
        } else {
            listOf(
                Arbeidsforhold().apply {
                    this.arbeidsforholdID = this@ArbeidsforholdDokumentBuilder.arbeidsforholdID
                    this.arbeidsforholdIDnav = this@ArbeidsforholdDokumentBuilder.arbeidsforholdIDnav
                    this.ansettelsesPeriode = this@ArbeidsforholdDokumentBuilder.ansettelsesPeriode
                    this.arbeidsforholdstype = this@ArbeidsforholdDokumentBuilder.arbeidsforholdstype
                    this.arbeidsgivertype = this@ArbeidsforholdDokumentBuilder.arbeidsgivertype
                    this.arbeidsgiverID = this@ArbeidsforholdDokumentBuilder.arbeidsgiverID
                    this.arbeidstakerID = this@ArbeidsforholdDokumentBuilder.arbeidstakerID
                    this.opplysningspliktigtype = this@ArbeidsforholdDokumentBuilder.opplysningspliktigtype
                    this.opplysningspliktigID = this@ArbeidsforholdDokumentBuilder.opplysningspliktigID
                    this.arbeidsforholdInnrapportertEtterAOrdningen = this@ArbeidsforholdDokumentBuilder.arbeidsforholdInnrapportertEtterAOrdningen
                    this.opprettelsestidspunkt = this@ArbeidsforholdDokumentBuilder.opprettelsestidspunkt
                    this.sistBekreftet = this@ArbeidsforholdDokumentBuilder.sistBekreftet
                }
            )
        }
    )
}

@MelosysTestDsl
class ArbeidsforholdBuilder {
    var arbeidsforholdID: String? = null
    var arbeidsforholdIDnav: Long = 0
    var ansettelsesPeriode: Periode? = null
    var arbeidsforholdstype: String? = null
    var arbeidsgivertype: Aktoertype? = null
    var arbeidsgiverID: String? = null
    var arbeidstakerID: String? = null
    var opplysningspliktigtype: Aktoertype? = null
    var opplysningspliktigID: String? = null
    var arbeidsforholdInnrapportertEtterAOrdningen: Boolean? = null
    var opprettelsestidspunkt: OffsetDateTime? = null
    var sistBekreftet: OffsetDateTime? = null

    fun ansettelsesPeriode(fom: LocalDate, tom: LocalDate? = null) {
        ansettelsesPeriode = Periode(fom, tom)
    }

    fun build(): Arbeidsforhold = Arbeidsforhold().apply {
        this.arbeidsforholdID = this@ArbeidsforholdBuilder.arbeidsforholdID
        this.arbeidsforholdIDnav = this@ArbeidsforholdBuilder.arbeidsforholdIDnav
        this.ansettelsesPeriode = this@ArbeidsforholdBuilder.ansettelsesPeriode
        this.arbeidsforholdstype = this@ArbeidsforholdBuilder.arbeidsforholdstype
        this.arbeidsgivertype = this@ArbeidsforholdBuilder.arbeidsgivertype
        this.arbeidsgiverID = this@ArbeidsforholdBuilder.arbeidsgiverID
        this.arbeidstakerID = this@ArbeidsforholdBuilder.arbeidstakerID
        this.opplysningspliktigtype = this@ArbeidsforholdBuilder.opplysningspliktigtype
        this.opplysningspliktigID = this@ArbeidsforholdBuilder.opplysningspliktigID
        this.arbeidsforholdInnrapportertEtterAOrdningen = this@ArbeidsforholdBuilder.arbeidsforholdInnrapportertEtterAOrdningen
        this.opprettelsestidspunkt = this@ArbeidsforholdBuilder.opprettelsestidspunkt
        this.sistBekreftet = this@ArbeidsforholdBuilder.sistBekreftet
    }
}

@MelosysTestDsl
class SedDokumentBuilder {
    var avsenderLandkode: Landkoder? = null
    var rinaSaksnummer: String? = null
    var rinaDokumentID: String? = null
    var fnr: String? = null
    var sedType: no.nav.melosys.domain.eessi.SedType? = null
    var lovvalgsperiode: no.nav.melosys.domain.dokument.medlemskap.Periode? = null
    var lovvalgBestemmelse: LovvalgBestemmelse? = null
    var lovvalgslandKode: Landkoder? = null
    var unntakFraLovvalgBestemmelse: LovvalgBestemmelse? = null
    var unntakFraLovvalgslandKode: Landkoder? = null

    fun lovvalgsperiode(fom: LocalDate, tom: LocalDate? = null) {
        lovvalgsperiode = no.nav.melosys.domain.dokument.medlemskap.Periode(fom, tom)
    }

    fun build(): SedDokument = SedDokument().apply {
        this.avsenderLandkode = this@SedDokumentBuilder.avsenderLandkode
        this.rinaSaksnummer = this@SedDokumentBuilder.rinaSaksnummer
        this.rinaDokumentID = this@SedDokumentBuilder.rinaDokumentID
        this.fnr = this@SedDokumentBuilder.fnr
        this.sedType = this@SedDokumentBuilder.sedType
        this.lovvalgsperiode = this@SedDokumentBuilder.lovvalgsperiode
        this.lovvalgBestemmelse = this@SedDokumentBuilder.lovvalgBestemmelse
        this.lovvalgslandKode = this@SedDokumentBuilder.lovvalgslandKode
        this.unntakFraLovvalgBestemmelse = this@SedDokumentBuilder.unntakFraLovvalgBestemmelse
        this.unntakFraLovvalgslandKode = this@SedDokumentBuilder.unntakFraLovvalgslandKode
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
