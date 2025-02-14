package no.nav.melosys.service.ftrl.medlemskapsperiode

import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.repository.MedlemskapsperiodeRepository
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.ftrl.GyldigeTrygdedekningerService
import no.nav.melosys.service.kontroll.regler.PeriodeRegler
import no.nav.melosys.service.medl.MedlPeriodeService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class MedlemskapsperiodeService(
    private val medlemskapsperiodeRepository: MedlemskapsperiodeRepository,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val medlPeriodeService: MedlPeriodeService,
    private val gyldigeTrygdedekningerService: GyldigeTrygdedekningerService
) {
    @Transactional(readOnly = true)
    fun hentMedlemskapsperioder(behandlingsresultatID: Long): List<Medlemskapsperiode> {
        return behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID).medlemskapsperioder.toList()
    }

    @Transactional
    fun opprettMedlemskapsperiode(
        behandlingsresultatID: Long,
        fom: LocalDate?,
        tom: LocalDate?,
        innvilgelsesResultat: InnvilgelsesResultat?,
        trygdedekning: Trygdedekninger?,
        bestemmelse: Bestemmelse?
    ): Medlemskapsperiode {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID)
        val søknad = behandlingsresultat.behandling.mottatteOpplysninger.mottatteOpplysningerData as SøknadNorgeEllerUtenforEØS

        validerFelt(
            behandlingsresultat,
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
        behandlingsresultat.addMedlemskapsperiode(nyMedlemskapsperiode)

        behandlingsresultat.clearTrygdeavgiftsperioder()
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
        bestemmelse: Bestemmelse?
    ): Medlemskapsperiode {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID)
        val søknad = behandlingsresultat.behandling.mottatteOpplysninger.mottatteOpplysningerData as SøknadNorgeEllerUtenforEØS

        validerFelt(
            behandlingsresultat,
            fom,
            tom,
            innvilgelsesResultat,
            trygdedekning,
            bestemmelse,
            søknad.soeknadsland.landkoder
        )

        val medlemskapsperiode = behandlingsresultat.medlemskapsperioder.firstOrNull { it.id == medlemskapsperiodeID }
            ?: throw IkkeFunnetException("Behandling $behandlingsresultatID har ingen medlemskapsperiode med id $medlemskapsperiodeID")

        medlemskapsperiode.apply {
            this.tom = tom
            this.fom = fom
            this.innvilgelsesresultat = innvilgelsesResultat
            this.trygdedekning = trygdedekning
            this.bestemmelse = bestemmelse
            medlemskapstype = UtledMedlemskapstype.av(bestemmelse!!)
        }
        medlemskapsperiode.clearTrygdeavgiftsperioder()

        return medlemskapsperiodeRepository.saveAndFlush(medlemskapsperiode)
    }

    private fun datoErInnenforÅr(dato: LocalDate, år: Int): Boolean {
        val førsteDagIÅr = LocalDate.of(år, 1, 1)
        val sisteDagIÅr = LocalDate.of(år, 12, 31)

        return !dato.isBefore(førsteDagIÅr) && !dato.isAfter(sisteDagIÅr)
    }

    private fun validerFelt(
        behandlingsresultat: Behandlingsresultat,
        fom: LocalDate?,
        tom: LocalDate?,
        innvilgelsesResultat: InnvilgelsesResultat?,
        trygdedekning: Trygdedekninger?,
        bestemmelse: Bestemmelse?,
        land: List<String>
    ) {
        val nullTilOgMedDatoErTillatt =
            Land_iso2.NO.kode in land && land.size == 1 && bestemmelse in listOf(
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
            )

        if (tom == null && !nullTilOgMedDatoErTillatt) {
            throw FunksjonellException("Tom-dato er påkrevd")
        } else if (fom == null || innvilgelsesResultat == null || bestemmelse == null || trygdedekning == null) {
            throw FunksjonellException("Fom-dato, innvilgelsesresultat, bestemmelse og trygdedekning er påkrevd")
        }

        if (behandlingsresultat.årsavregning != null) {
            val år = behandlingsresultat.årsavregning.aar
            if (tom != null) {
                if (!datoErInnenforÅr(tom, år) || !datoErInnenforÅr(fom, år))
                    throw FunksjonellException("Utenfor valgt år")
            } else {
                throw FunksjonellException("Til-og-med dato er påkrevd for årsavregning")
            }
        }

        val behandlingstema = behandlingsresultat.behandling.tema
        val gyldigeTrygdedekninger = gyldigeTrygdedekningerService.hentTrygdedekninger(
            behandlingstema,
            if (bestemmelse == Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_15_ANDRE_LEDD) null else bestemmelse
        )
        if (trygdedekning !in gyldigeTrygdedekninger) {
            throw FunksjonellException("Trygedekning $trygdedekning støttes ikke for behandlingstema $behandlingstema og bestemmelse $bestemmelse")
        } else if (PeriodeRegler.feilIPeriode(fom, tom)) {
            throw FunksjonellException("Tom-dato kan ikke være før fom-dato")
        }
    }

    @Transactional
    fun erstattMedlemskapsperioder(behandlingID: Long, opprinneligBehandlingID: Long, nyeMedlemskapsperioder: List<Medlemskapsperiode>) {
        val opprinneligeMedlemskapsperioder = hentMedlemskapsperioder(opprinneligBehandlingID)
            .filter { it.erInnvilget() || it.erOpphørt() }
        val perioderSomVidereføres = nyeMedlemskapsperioder
            .filter { it.erInnvilget() || it.erOpphørt() }

        opphørOpprinneligeInnvilgedePerioderSomIkkeVidereføres(opprinneligeMedlemskapsperioder, perioderSomVidereføres)
        opprettEllerOppdaterInnvilgedePerioder(behandlingID, nyeMedlemskapsperioder)

        feilregistrerOpprinneligeOpphørtePerioderSomIkkeVidereføres(opprinneligeMedlemskapsperioder, perioderSomVidereføres)
        opprettEllerOppdaterOpphørtePerioder(behandlingID, nyeMedlemskapsperioder)
    }

    private fun opphørOpprinneligeInnvilgedePerioderSomIkkeVidereføres(
        opprinneligeGjeldendeMedlemskapsperioder: List<Medlemskapsperiode>,
        perioderSomVidereføres: List<Medlemskapsperiode>
    ) =
        opprinneligeGjeldendeMedlemskapsperioder
            .filter { it.erInnvilget() }
            .filterNot { eksistererMedlemskapsperiodeMedID(perioderSomVidereføres, it.medlPeriodeID) }
            .forEach { medlPeriodeService.avvisPeriodeOpphørt(it.medlPeriodeID) }

    private fun feilregistrerOpprinneligeOpphørtePerioderSomIkkeVidereføres(
        opprinneligeGjeldendeMedlemskapsperioder: List<Medlemskapsperiode>,
        perioderSomVidereføres: List<Medlemskapsperiode>
    ) =
        opprinneligeGjeldendeMedlemskapsperioder
            .filter { it.erOpphørt() }
            .filterNot { eksistererMedlemskapsperiodeMedID(perioderSomVidereføres, it.medlPeriodeID) }
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
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID)
        val medlemskapsperiode = behandlingsresultat.medlemskapsperioder
            .firstOrNull { it.id == medlemskapsperiodeID }
            ?: throw IkkeFunnetException("Finner ingen medlemskapsperiode med id $medlemskapsperiodeID for behandling $behandlingsresultatID")

        behandlingsresultat.removeMedlemskapsperiode(medlemskapsperiode)
    }

    @Transactional
    fun slettMedlemskapsperioder(behandlingsresultatID: Long) {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID)
        behandlingsresultat.clearMedlemskapsperioder()
    }
}
