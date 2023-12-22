package no.nav.melosys.service.kontroll.regler

import no.nav.melosys.domain.dokument.person.adresse.BostedsadressePeriode
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData
import no.nav.melosys.domain.person.Persondata
import no.nav.melosys.domain.person.adresse.Bostedsadresse
import java.time.LocalDate
import java.util.*


object PersonRegler {
    private const val NORGE_ISO2_LANDKODE = "NO"

    @JvmStatic
    fun erPersonDød(persondata: Persondata): Boolean {
        return persondata.erPersonDød()
    }

    @JvmStatic
    fun personBosattINorge(persondata: Persondata): Boolean {
        val bostedsadresseOptional = persondata.finnBostedsadresse()

        return bostedsadresseOptional.isPresent && bostedsadresseOptional.get().strukturertAdresse.landkode != null && NORGE_ISO2_LANDKODE == bostedsadresseOptional.get().strukturertAdresse.landkode
    }

    @JvmStatic
    fun personBosattINorgeIPeriode(
        bostedsadressePerioder: List<BostedsadressePeriode>,
        bostedsadresseOptional: Optional<Bostedsadresse>,
        periodeFra: LocalDate,
        periodeTil: LocalDate
    ): Boolean {
        val erGyldigBostedsadresse = bostedsadresseOptional.filter { bostedsadresse: Bostedsadresse ->
            bostedsadresse.strukturertAdresse.landkode != null && NORGE_ISO2_LANDKODE == bostedsadresse.strukturertAdresse.landkode &&
                filtrerAdressePeriode(bostedsadresse.gyldigFraOgMed, bostedsadresse.gyldigTilOgMed, periodeFra, periodeTil)
        }.isPresent

        return erGyldigBostedsadresse || bostedsadressePerioder
            .stream()
            .anyMatch { a: BostedsadressePeriode ->
                a.bostedsadresse.tilStrukturertAdresse().landkode == NORGE_ISO2_LANDKODE &&
                    filtrerAdressePeriode(a.periode.fom, a.periode.tom, periodeFra, periodeTil)
            }
    }

    private fun filtrerAdressePeriode(
        adresseGyldigFom: LocalDate?,
        adresseGyldigTom: LocalDate?,
        periodeFra: LocalDate,
        periodeTil: LocalDate
    ): Boolean {
        val fom = adresseGyldigFom
        val tom = adresseGyldigTom ?: LocalDate.now().plusYears(10)

        if (fom == null) {
            return false
        }

        val fomErFørEllerLikPeriodeFra = fom.isBefore(periodeFra) || fom.isEqual(periodeFra)
        val tomErEtterEllerLikPeriodeFil = tom.isAfter(periodeFra) || tom.isEqual(periodeFra)
        val fomErFørEllerLikPeriodeTil = fom.isBefore(periodeTil) || fom.isEqual(periodeTil)
        val tomErEtterEllerLikPeriodeTil = tom.isAfter(periodeTil) || tom.isEqual(periodeTil)

        return (fomErFørEllerLikPeriodeFra && tomErEtterEllerLikPeriodeFil) ||
            (fomErFørEllerLikPeriodeTil && tomErEtterEllerLikPeriodeTil)
    }

    @JvmStatic
    fun harRegistrertAdresse(persondata: Persondata, mottatteOpplysningerData: MottatteOpplysningerData?): Boolean {
        return !persondata.manglerGyldigRegistrertAdresse() || mottatteOpplysningerData?.bosted?.oppgittAdresse?.erGyldig() == true
    }

    // TODO Bør vi fortsatt ta hensyn til MottatteOpplysningerData her?
    fun harRegistrertAdresse(persondata: Persondata): Boolean {
        return !persondata.manglerGyldigRegistrertAdresse()
    }
}
