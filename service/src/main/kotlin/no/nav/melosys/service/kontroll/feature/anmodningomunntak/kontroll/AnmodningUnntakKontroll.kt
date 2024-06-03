package no.nav.melosys.service.kontroll.feature.anmodningomunntak.kontroll

import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser
import no.nav.melosys.service.kontroll.feature.anmodningomunntak.data.AnmodningUnntakKontrollData
import no.nav.melosys.service.kontroll.regler.PersonRegler.harRegistrertAdresse
import no.nav.melosys.service.validering.Kontrollfeil


object AnmodningUnntakKontroll {
    fun harRegistrertAdresse(kontrollData: AnmodningUnntakKontrollData): Kontrollfeil? {
        if (!harRegistrertAdresse(kontrollData.persondata, kontrollData.mottatteOpplysningerData)) {
            return Kontrollfeil(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE)
        }

        return null
    }

    fun anmodningsperiodeManglerSluttdato(kontrollData: AnmodningUnntakKontrollData): Kontrollfeil? {
        if (kontrollData.anmodningsperiode.tom == null) {
            return Kontrollfeil(Kontroll_begrunnelser.INGEN_SLUTTDATO)
        }

        return null
    }

    fun kunEnArbeidsgiver(kontrollData: AnmodningUnntakKontrollData): Kontrollfeil? {
        if (kontrollData.antallArbeidsgivere != 1) {
            return Kontrollfeil(Kontroll_begrunnelser.IKKE_KUN_EN_VIRKSOMHET)
        }

        return null
    }
}
