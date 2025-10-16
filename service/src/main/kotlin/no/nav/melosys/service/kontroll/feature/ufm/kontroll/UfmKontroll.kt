package no.nav.melosys.service.kontroll.feature.ufm.kontroll

import mu.KotlinLogging
import java.time.LocalDate
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData
import no.nav.melosys.domain.mottatteopplysninger.SedGrunnlag
import no.nav.melosys.service.kontroll.feature.anmodningomunntak.kontroll.AnmodningUnntakKontroll.harOverlappendeMedlemsperiodeFraSed
import no.nav.melosys.service.kontroll.feature.ufm.data.UfmKontrollData
import no.nav.melosys.service.kontroll.regler.*
import no.nav.melosys.service.kontroll.regler.OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiodeMerEnn1DagFraSed
import no.nav.melosys.service.kontroll.regler.OverlappendeMedlemskapsperioderRegler.harOverlappendePerioderMedUlikSedLovvalgslandOgMedlLovvalgsland
import org.apache.cxf.common.util.StringUtils.isEmpty
import kotlin.jvm.optionals.getOrNull

private val log = KotlinLogging.logger { }

object UfmKontroll {

    fun feilIPeriode(data: UfmKontrollData) =
        if (PeriodeRegler.feilIPeriode(data.sedDokument.hentLovvalgsperiode().fom, data.sedDokument.hentLovvalgsperiode().tom))
            Kontroll_begrunnelser.FEIL_I_PERIODEN else null

    fun periodeErÅpen(data: UfmKontrollData) =
        if (PeriodeRegler.periodeErÅpen(data.sedDokument.hentLovvalgsperiode().fom, data.sedDokument.hentLovvalgsperiode().tom))
            Kontroll_begrunnelser.INGEN_SLUTTDATO else null

    fun periodeOver24MånederOgEnDag(data: UfmKontrollData): Kontroll_begrunnelser? {
        val fom = data.sedDokument.hentLovvalgsperiode().fom
        val tom = data.sedDokument.hentLovvalgsperiode().tom
        return if (PeriodeRegler.periodeOver2ÅrOgEnDag(fom, tom)) Kontroll_begrunnelser.PERIODEN_OVER_24_MD else null
    }

    fun periodeOver5År(data: UfmKontrollData) =
        if (PeriodeRegler.periodeOver5År(data.sedDokument.hentLovvalgsperiode().fom, data.sedDokument.hentLovvalgsperiode().tom))
            Kontroll_begrunnelser.PERIODEN_OVER_5_AR else null

    fun periodeStarterFørFørsteJuni2012(data: UfmKontrollData) =
        if (PeriodeRegler.datoErFørFørsteJuni2012(data.sedDokument.hentLovvalgsperiode().fom))
            Kontroll_begrunnelser.PERIODE_FOR_GAMMEL else null

    fun periodeOver1ÅrFremITid(data: UfmKontrollData) =
        if (PeriodeRegler.datoOver1ÅrFremITid(data.sedDokument.hentLovvalgsperiode().fom))
            Kontroll_begrunnelser.PERIODE_LANGT_FREM_I_TID else null

    fun utbetaltYtelserFraOffentligIPeriode(data: UfmKontrollData): Kontroll_begrunnelser? {
        val fom = data.sedDokument.hentLovvalgsperiode().fom
        val tom = data.sedDokument.hentLovvalgsperiode().tom
        return if (YtelseRegler.utbetaltYtelserFraOffentligIPeriode(data.inntektDokument, fom, tom))
            Kontroll_begrunnelser.MOTTAR_YTELSER else null
    }

    fun lovvalgslandErNorge(data: UfmKontrollData) =
        if (UfmRegler.lovvalgslandErNorge(data.sedDokument.lovvalgslandKode))
            Kontroll_begrunnelser.LOVVALGSLAND_NORGE else null

    fun overlappendeMedlemsperiode(data: UfmKontrollData) =
        if (harOverlappendeMedlemsperiodeFraSed(data.medlemskapDokument, data.sedDokument.lovvalgsperiode))
            Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER else null

    fun overlappendeMedlemsperiodeMerEnn1Dag(data: UfmKontrollData) =
        if (harOverlappendeMedlemsperiodeMerEnn1DagFraSed(data.medlemskapDokument, data.sedDokument.lovvalgsperiode))
            Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER else null

    fun overlappendeMedlemsperiodeForA003(data: UfmKontrollData): Kontroll_begrunnelser? {
        val sed = data.sedDokument
        val lovvalgsperiode = sed.lovvalgsperiode
        val medlemskap = data.medlemskapDokument

        if (!harOverlappendeMedlemsperiodeMerEnn1DagFraSed(medlemskap, lovvalgsperiode)) return null

        return when {
            sed.erMedlemskapsperiode() -> {
                log.info("Mottatt overlappende medlemsperiode med medlemskap for A003")
                Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER
            }

            sed.erUnntaksperiode() -> {
                log.info("Mottatt overlappende unntaksperiode uten medlemskap for A003")
                when {
                    sed.erEndring -> {
                        log.info("Mottatt overlappende unntaksperiode for A003 med en endring")
                        Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER
                    }

                    harMottatteOpplysningerMedYtterligereInformasjon(data.mottatteOpplysningerData?.getOrNull()) -> {
                        log.info("Mottatt overlappende unntaksperiode for A003 med ytterligere informasjon")
                        Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER
                    }

                    harOverlappendePerioderMedUlikSedLovvalgslandOgMedlLovvalgsland(sed, medlemskap) -> {
                        log.info("Mottatt overlappende unntaksperiode for A003 med ulike lovvalgsland i SED og MEDL")
                        Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER
                    }

                    else -> null
                }
            }

            else -> null
        }
    }

    fun statsborgerskapIkkeMedlemsland(data: UfmKontrollData) =
        if (UfmRegler.avsenderErNordiskEllerAvtaleland(data.sedDokument.avsenderLandkode) ||
            UfmRegler.erStatsløs(data.sedDokument.statsborgerskapKoder) ||
            UfmRegler.statsborgerskapErMedlemsland(data.sedDokument.statsborgerskapKoder)
        ) null else Kontroll_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND

    fun personDød(data: UfmKontrollData) =
        if (PersonRegler.erPersonDød(data.persondata)) Kontroll_begrunnelser.PERSON_DOD else null

    fun personBosattINorge(data: UfmKontrollData) =
        if (PersonRegler.personBosattINorge(data.persondata)) Kontroll_begrunnelser.BOSATT_I_NORGE else null

    fun personBosattINorgeIPerioden(data: UfmKontrollData): Kontroll_begrunnelser? {
        val fra = data.sedDokument.hentLovvalgsperiode().fom ?: error("fom er påkrevd for å kunne sjekke om person er bosatt i Norge i perioden")
        val til = data.sedDokument.hentLovvalgsperiode().tom
        val tilDato = til ?: LocalDate.now()

        val historiskeBosted = data.persondataMedHistorikk?.getOrNull()?.bostedsadresser ?: emptyList()
        val historiskeOpphold = data.persondataMedHistorikk?.getOrNull()?.oppholdsadresser ?: emptyList()

        val bostedsperioder = data.personhistorikkDokumenter
            .filterNotNull()
            .flatMap { it.bostedsadressePeriodeListe }

        val nåværende = data.persondata.finnBostedsadresse()

        return if (PersonRegler.personBosattINorgeIPeriode(
                bostedsperioder,
                nåværende,
                historiskeBosted,
                historiskeOpphold,
                fra,
                tilDato
            )
        )
            Kontroll_begrunnelser.BOSATT_I_NORGE_I_PERIODEN else null
    }

    fun arbeidsland(data: UfmKontrollData) =
        if (ArbeidsstedRegler.erArbeidslandFraSvalbardOgJanMayen(data.sedDokument))
            Kontroll_begrunnelser.ARBEIDSSTED_UTENFOR_EOS else null

    fun unntakForA003(data: UfmKontrollData): Kontroll_begrunnelser? =
        if (!UfmRegler.lovvalgslandErNorge(data.sedDokument.lovvalgslandKode) &&
            (harTransitiveRegler(data.mottatteOpplysningerData?.getOrNull()) || harOvergangsregler(data.sedDokument))
        ) Kontroll_begrunnelser.OVERGANGSREGEL_VALGT else null

    private fun harTransitiveRegler(data: MottatteOpplysningerData?) =
        (data as? SedGrunnlag)?.overgangsregelbestemmelser?.isNotEmpty() == true

    private fun harOvergangsregler(sed: SedDokument) =
        sed.lovvalgBestemmelse == Tilleggsbestemmelser_883_2004.FO_883_2004_ART87_8 ||
                sed.lovvalgBestemmelse == Tilleggsbestemmelser_883_2004.FO_883_2004_ART87A

    private fun harMottatteOpplysningerMedYtterligereInformasjon(data: MottatteOpplysningerData?) =
        !isEmpty((data as? SedGrunnlag)?.ytterligereInformasjon)
}
