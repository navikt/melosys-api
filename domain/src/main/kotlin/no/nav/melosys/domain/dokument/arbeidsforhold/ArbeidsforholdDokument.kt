package no.nav.melosys.domain.dokument.arbeidsforhold

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import no.nav.melosys.domain.dokument.SaksopplysningDokument
import no.nav.melosys.domain.dokument.felles.Periode
import org.apache.commons.lang3.StringUtils

class ArbeidsforholdDokument @JsonCreator constructor(
    @get:JsonValue var arbeidsforhold: List<Arbeidsforhold> = listOf()
) : SaksopplysningDokument {

    fun hentOrgnumre(): Set<String> =
        arbeidsforhold
            .flatMap { it.hentOrgnumre() }
            .filter { StringUtils.isNotEmpty(it) }
            .toSet()

    fun hentArbeidsgiverIDer(): Set<String> =
        arbeidsforhold
            .mapNotNull { it.arbeidsgiverID }
            .filter { StringUtils.isNotEmpty(it) }
            .toSet()

    fun hentAnsettelsesperioder(orgnummere: Collection<String>): Set<Periode> =
        arbeidsforhold
            .filter { arbeidsforhold -> arbeidsforhold.hentOrgnumre().any { it in orgnummere } }
            .mapNotNull { it.ansettelsesPeriode }
            .toSet()
}
