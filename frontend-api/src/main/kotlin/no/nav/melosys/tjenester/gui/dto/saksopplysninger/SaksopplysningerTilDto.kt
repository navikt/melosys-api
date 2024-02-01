package no.nav.melosys.tjenester.gui.dto.saksopplysninger

import no.nav.melosys.domain.Saksopplysning
import no.nav.melosys.domain.SaksopplysningType
import no.nav.melosys.domain.dokument.arbeidsforhold.Arbeidsforhold
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument
import no.nav.melosys.domain.dokument.inntekt.InntektDokument
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.tjenester.gui.dto.SaksopplysningerDto
import no.nav.melosys.tjenester.gui.dto.eessi.SedDokumentDto
import no.nav.melosys.tjenester.gui.dto.inntekt.InntektDto
import org.springframework.stereotype.Component

@Component
class SaksopplysningerTilDto {
    fun getSaksopplysningerDto(saksopplysningSet: Set<Saksopplysning>): SaksopplysningerDto {
        val dto = SaksopplysningerDto()

        for (saksopplysning in saksopplysningSet) {
            val type = saksopplysning.type
            val dokument = saksopplysning.dokument

            when (type) {
                SaksopplysningType.ARBFORH -> {
                    val arbeidsforholdDokument = dokument as ArbeidsforholdDokument
                    if (arbeidsforholdDokument?.getArbeidsforhold() != null) {
                        arbeidsforholdDokument.getArbeidsforhold().stream().sorted(ArbeidsforholdComparator())
                    }
                    dto.arbeidsforhold = arbeidsforholdDokument
                }

                SaksopplysningType.ORG -> dto.organisasjoner.apply { dokument as OrganisasjonDokument }
                SaksopplysningType.MEDL -> {
                    val medlemskapDokument = dokument as MedlemskapDokument
                    if (medlemskapDokument?.getMedlemsperiode() != null) {
                        medlemskapDokument.getMedlemsperiode().stream()
                            .sorted(Comparator.comparing { obj: Medlemsperiode -> obj.getType() }
                                .thenComparing(medlemsperiodeKomparator))
                    }
                    dto.medlemskap = medlemskapDokument
                }

                SaksopplysningType.INNTK -> dto.inntekt = InntektDto(dokument as InntektDokument)
                SaksopplysningType.SEDOPPL -> dto.sed = SedDokumentDto.fra(dokument as SedDokument)
                SaksopplysningType.PDL_PERSOPL -> TODO()
                SaksopplysningType.PDL_PERS_SAKS -> TODO()
                SaksopplysningType.PERSHIST -> TODO()
                SaksopplysningType.PERSOPL -> TODO()
                SaksopplysningType.UTBETAL -> TODO()
            }
        }
        return dto
    }

    /**
     * - Åpent arbeidsforhold uten sluttdato sorteres foran/over arbeidsforhold med sluttdato.
     * - Arbeidsforhold må ellers sorteres med nyeste fra-og-med-dato øverst.
     */
    internal class ArbeidsforholdComparator : Comparator<Arbeidsforhold> {
        override fun compare(arbeidsforholdA: Arbeidsforhold, arbeidsforholdB: Arbeidsforhold): Int {
            return if (arbeidsforholdA.getAnsettelsesPeriode().tom == null) {
                if (arbeidsforholdB.getAnsettelsesPeriode().tom == null) {
                    arbeidsforholdB.getAnsettelsesPeriode().fom.compareTo(arbeidsforholdA.getAnsettelsesPeriode().fom)
                } else {
                    -1
                }
            } else if (arbeidsforholdB.getAnsettelsesPeriode().tom == null) {
                1
            } else {
                arbeidsforholdB.getAnsettelsesPeriode().fom.compareTo(arbeidsforholdA.getAnsettelsesPeriode().fom)
            }
        }
    }

    companion object {
        @JvmField
        val medlemsperiodeKomparator: Comparator<Medlemsperiode> =
            Comparator { o1: Medlemsperiode, o2: Medlemsperiode -> o2.getPeriode().fom.compareTo(o1.getPeriode().fom) }
    }
}
