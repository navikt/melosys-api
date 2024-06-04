package no.nav.melosys.service.kontroll.feature.anmodningomunntak.kontroll

import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_konv_efta_storbritannia
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.service.kontroll.feature.anmodningomunntak.data.AnmodningUnntakKontrollData
import no.nav.melosys.service.kontroll.regler.PersonRegler.harRegistrertAdresse
import no.nav.melosys.service.validering.Kontrollfeil
import java.time.LocalDate


object AnmodningUnntakKontroll {
    fun brukerManglerAdresse(kontrollData: AnmodningUnntakKontrollData): Kontrollfeil? {
        if (!harRegistrertAdresse(kontrollData.persondata, kontrollData.mottatteOpplysningerData)) {
            return Kontrollfeil(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE)
        }

        return null
    }

    fun fullmektigManglerAdresse(kontrollData: AnmodningUnntakKontrollData): Kontrollfeil? {
        val fullmektig = kontrollData.fullmektig ?: return null

        if (fullmektig.erOrganisasjon()) {
            val organisasjon = kontrollData.organisasjonDokument ?: throw TekniskException("OrganisasjonDokument kan ikke være null")

            if (!organisasjon.harRegistrertPostadresse() && !organisasjon.harRegistrertForretningsadresse()) {
                return Kontrollfeil(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE_REPRESENTANT)
            }
        } else {
            val persondata = kontrollData.persondataTilFullmektig ?: throw TekniskException("Persondata til fullmektig kan ikke være null")

            if (!harRegistrertAdresse(persondata, kontrollData.mottatteOpplysningerData)) {
                return Kontrollfeil(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE_REPRESENTANT)
            }
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

    fun storbritanniaKonvensjonBruktForTidlig(kontrollData: AnmodningUnntakKontrollData): Kontrollfeil? {
        val anmodningsperiode = kontrollData.anmodningsperiode
        val JANUAR_2024 = LocalDate.of(2024, 1, 1)
        val storbritanniaBestemmelser = Lovvalgbestemmelser_konv_efta_storbritannia.values()

        if (anmodningsperiode.bestemmelse in storbritanniaBestemmelser && anmodningsperiode.fom?.isBefore(JANUAR_2024) == true) {
            return Kontrollfeil(Kontroll_begrunnelser.STORBRITANNIA_KONV_BRUKT_FOR_TIDLIG)
        }

        return null
    }
}
