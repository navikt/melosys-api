package no.nav.melosys.service.medlemskapsperiode

import io.getunleash.Unleash
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.repository.MedlemskapsperiodeRepository
import no.nav.melosys.service.MedlemAvFolketrygdenService
import no.nav.melosys.service.avgift.TrygdeavgiftsgrunnlagService
import no.nav.melosys.service.ftrl.GyldigeTrygdedekningerService
import no.nav.melosys.service.kontroll.regler.PeriodeRegler
import no.nav.melosys.service.medl.MedlPeriodeService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class MedlemskapsperiodeService(
    private val medlemskapsperiodeRepository: MedlemskapsperiodeRepository,
    private val medlemAvFolketrygdenService: MedlemAvFolketrygdenService,
    private val trygdeavgiftsgrunnlagService: TrygdeavgiftsgrunnlagService,
    private val medlPeriodeService: MedlPeriodeService,
    private val gyldigeTrygdedekningerService: GyldigeTrygdedekningerService,
    private val unleash: Unleash
) {
    @Transactional(readOnly = true)
    fun hentMedlemskapsperioder(behandlingsresultatID: Long): List<Medlemskapsperiode> {
        return medlemAvFolketrygdenService.finnMedlemAvFolketrygden(behandlingsresultatID)
            .map { it.medlemskapsperioder.toList() }
            .orElse(emptyList())
    }

    @Transactional
    fun opprettMedlemskapsperiode(
        behandlingsresultatID: Long,
        fom: LocalDate?,
        tom: LocalDate?,
        innvilgelsesResultat: InnvilgelsesResultat?,
        trygdedekning: Trygdedekninger?,
        bestemmelse: Folketrygdloven_kap2_bestemmelser?
    ): Medlemskapsperiode {
        val medlemAvFolketrygden = medlemAvFolketrygdenService.hentMedlemAvFolketrygden(behandlingsresultatID)
        val søknad = medlemAvFolketrygden.behandlingsresultat.behandling.mottatteOpplysninger.mottatteOpplysningerData as SøknadNorgeEllerUtenforEØS

        validerFelt(
            medlemAvFolketrygden.behandlingsresultat.behandling.tema,
            fom,
            tom,
            innvilgelsesResultat,
            trygdedekning,
            bestemmelse,
            søknad.soeknadsland.landkoder
        )

        val nyMedlemskapsperiode = Medlemskapsperiode().apply {
            this.tom = tom
            this.fom = fom
            this.innvilgelsesresultat = innvilgelsesResultat
            this.trygdedekning = trygdedekning
            this.bestemmelse = bestemmelse
            medlemskapstype = UtledMedlemskapstype.av(bestemmelse!!)
        }
        medlemAvFolketrygden.addMedlemskapsperiode(nyMedlemskapsperiode)

        fjernTrygdeavgiftsperioderOmDeFinnes(medlemAvFolketrygden)
        return medlemskapsperiodeRepository.save(nyMedlemskapsperiode)
    }

    @Transactional
    fun oppdaterMedlemskapsperiode(
        behandlingsresultatID: Long,
        medlemskapsperiodeID: Long,
        fom: LocalDate?,
        tom: LocalDate?,
        innvilgelsesResultat: InnvilgelsesResultat?,
        trygdedekning: Trygdedekninger?,
        bestemmelse: Folketrygdloven_kap2_bestemmelser?
    ): Medlemskapsperiode {
        val medlemAvFolketrygden = medlemAvFolketrygdenService.hentMedlemAvFolketrygden(behandlingsresultatID)
        val søknad = medlemAvFolketrygden.behandlingsresultat.behandling.mottatteOpplysninger.mottatteOpplysningerData as SøknadNorgeEllerUtenforEØS

        validerFelt(
            medlemAvFolketrygden.behandlingsresultat.behandling.tema,
            fom,
            tom,
            innvilgelsesResultat,
            trygdedekning,
            bestemmelse,
            søknad.soeknadsland.landkoder
        )

        val medlemskapsperiode = medlemAvFolketrygden.medlemskapsperioder.firstOrNull { it.id == medlemskapsperiodeID }
            ?: throw IkkeFunnetException("Behandling $behandlingsresultatID har ingen medlemskapsperiode med id $medlemskapsperiodeID")

        medlemskapsperiode.apply {
            this.tom = tom
            this.fom = fom
            this.innvilgelsesresultat = innvilgelsesResultat
            this.trygdedekning = trygdedekning
            this.bestemmelse = bestemmelse
            medlemskapstype = UtledMedlemskapstype.av(bestemmelse!!)
        }

        fjernTrygdeavgiftsperioderOmDeFinnes(medlemAvFolketrygden)
        return medlemskapsperiodeRepository.save(medlemskapsperiode)
    }

    private fun fjernTrygdeavgiftsperioderOmDeFinnes(medlemAvFolketrygden: MedlemAvFolketrygden) =
        medlemAvFolketrygden.fastsattTrygdeavgift?.let { trygdeavgiftsgrunnlagService.fjernTrygdeavgiftsperioderOmDeFinnes(it) }

    private fun validerFelt(
        behandlingstema: Behandlingstema,
        fom: LocalDate?,
        tom: LocalDate?,
        innvilgelsesResultat: InnvilgelsesResultat?,
        trygdedekning: Trygdedekninger?,
        bestemmelse: Folketrygdloven_kap2_bestemmelser?,
        land: List<String>
    ) {
        val nullTilOgMedDatoErTillatt =
            Land_iso2.NO.kode in land && land.size == 1 && bestemmelse in listOf(
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1_FØRSTE_LEDD,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1_FJERDE_LEDD
            )

        if (unleash.isEnabled(ToggleName.MELOSYS_FTRL_IKKE_YRKESAKTIV) && tom == null && !nullTilOgMedDatoErTillatt) {
            throw FunksjonellException("Tom-dato er påkrevd")
        } else if (fom == null || innvilgelsesResultat == null || bestemmelse == null || trygdedekning == null) {
            throw FunksjonellException("Fom-dato, innvilgelsesresultat, bestemmelse og trygdedekning er påkrevd")
        } else if (trygdedekning !in gyldigeTrygdedekningerService.hentTrygdedekninger(behandlingstema, bestemmelse)) {
            throw FunksjonellException("Trygedekning $trygdedekning støttes ikke for behandlingstema $behandlingstema og bestemmelse $bestemmelse")
        } else if (PeriodeRegler.feilIPeriode(fom, tom)) {
            throw FunksjonellException("Tom-dato kan ikke være før fom-dato")
        }
    }

    @Transactional
    fun erstattMedlemskapsperioder(behandlingID: Long, opprinneligBehandlingID: Long, nyeMedlemskapsperioder: List<Medlemskapsperiode>) {
        val opprinneligeMedlemskapsperioder = hentMedlemskapsperioder(opprinneligBehandlingID)
            .filter { it.erInnvilget() || it.erOpphørt() }

        opphørOpprinneligeInnvilgedePerioderSomIkkeVidereføres(opprinneligeMedlemskapsperioder, nyeMedlemskapsperioder)
        opprettEllerOppdaterInnvilgedePerioder(behandlingID, nyeMedlemskapsperioder)

        feilregistrerOpprinneligeOpphørtePerioderSomIkkeVidereføres(opprinneligeMedlemskapsperioder, nyeMedlemskapsperioder)
        opprettEllerOppdaterOpphørtePerioder(behandlingID, nyeMedlemskapsperioder)
    }

    private fun opphørOpprinneligeInnvilgedePerioderSomIkkeVidereføres(
        opprinneligeGjeldendeMedlemskapsperioder: List<Medlemskapsperiode>,
        nyeMedlemskapsperioder: List<Medlemskapsperiode>
    ) =
        opprinneligeGjeldendeMedlemskapsperioder
            .filter { it.erInnvilget() }
            .filter { !eksistererMedlemskapsperiodeMedID(nyeMedlemskapsperioder, it.medlPeriodeID) }
            .forEach { medlPeriodeService.avvisPeriodeOpphørt(it.medlPeriodeID) }

    private fun feilregistrerOpprinneligeOpphørtePerioderSomIkkeVidereføres(
        opprinneligeGjeldendeMedlemskapsperioder: List<Medlemskapsperiode>,
        nyeMedlemskapsperioder: List<Medlemskapsperiode>
    ) =
        opprinneligeGjeldendeMedlemskapsperioder
            .filter { it.erOpphørt() }
            .filter { !eksistererMedlemskapsperiodeMedID(nyeMedlemskapsperioder, it.medlPeriodeID) }
            .forEach { medlPeriodeService.avvisPeriodeFeilregistrert(it.medlPeriodeID) }

    private fun opprettEllerOppdaterInnvilgedePerioder(behandlingID: Long, nyeMedlemskapsperioder: List<Medlemskapsperiode>) =
        nyeMedlemskapsperioder
            .filter { it.erInnvilget() }
            .forEach { opprettEllerOppdaterMedlPeriode(behandlingID, it) }

    private fun opprettEllerOppdaterOpphørtePerioder(behandlingID: Long, nyeMedlemskapsperioder: List<Medlemskapsperiode>) =
        nyeMedlemskapsperioder
            .filter { it.erOpphørt() }
            .forEach { opprettEllerOppdaterOpphørtMedlPeriode(behandlingID, it) }

    private fun eksistererMedlemskapsperiodeMedID(medlemskapsperioder: List<Medlemskapsperiode>, medlPeriodeID: Long): Boolean =
        medlemskapsperioder.any { it.medlPeriodeID == medlPeriodeID }

    fun opprettEllerOppdaterMedlPeriode(behandlingID: Long, medlemskapsperiode: Medlemskapsperiode) {
        if (medlemskapsperiode.medlPeriodeID == null) {
            medlPeriodeService.opprettPeriodeEndelig(behandlingID, medlemskapsperiode)
        } else {
            medlPeriodeService.oppdaterPeriodeEndelig(behandlingID, medlemskapsperiode)
        }
    }

    private fun opprettEllerOppdaterOpphørtMedlPeriode(behandlingID: Long, medlemskapsperiode: Medlemskapsperiode) {
        if (medlemskapsperiode.medlPeriodeID == null) {
            medlPeriodeService.opprettOpphørtPeriode(behandlingID, medlemskapsperiode)
        } else {
            medlPeriodeService.oppdaterOpphørtPeriode(behandlingID, medlemskapsperiode)
        }
    }

    @Transactional
    fun slettMedlemskapsperiode(behandlingsresultatID: Long, medlemskapsperiodeID: Long) {
        val medlemAvFolketrygden = medlemAvFolketrygdenService.hentMedlemAvFolketrygden(behandlingsresultatID)
        val medlemskapsperiode = medlemAvFolketrygden.medlemskapsperioder
            .firstOrNull { it.id == medlemskapsperiodeID }
            ?: throw IkkeFunnetException("Finner ingen medlemskapsperiode med id $medlemskapsperiodeID for behandling $behandlingsresultatID")

        medlemAvFolketrygden.removeMedlemskapsperioder(medlemskapsperiode)
        fjernTrygdeavgiftsperioderOmDeFinnes(medlemAvFolketrygden)
    }

    @Transactional
    fun slettMedlemskapsperioder(behandlingsresultatID: Long) {
        val medlemAvFolketrygden = medlemAvFolketrygdenService.hentMedlemAvFolketrygden(behandlingsresultatID)
        medlemAvFolketrygden.medlemskapsperioder.clear()
        fjernTrygdeavgiftsperioderOmDeFinnes(medlemAvFolketrygden)
    }
}
