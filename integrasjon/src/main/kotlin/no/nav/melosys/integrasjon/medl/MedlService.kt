package no.nav.melosys.integrasjon.medl

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import no.nav.melosys.domain.*
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode
import no.nav.melosys.domain.dokument.medlemskap.Periode
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Overgangsregelbestemmelser
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004
import no.nav.melosys.domain.mottatteopplysninger.SedGrunnlag
import no.nav.melosys.domain.util.IsoLandkodeKonverterer
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.integrasjon.medl.api.v1.MedlemskapsunntakForGet
import no.nav.melosys.integrasjon.medl.api.v1.MedlemskapsunntakForPost
import no.nav.melosys.integrasjon.medl.api.v1.MedlemskapsunntakForPut
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class MedlService(
    private val medlemskapRestConsumer: MedlemskapRestConsumer,
    private val objectMapper: ObjectMapper
) {

    init {
        objectMapper.registerModule(JavaTimeModule())
    }

    fun hentPeriodeListe(fnr: String, fom: LocalDate?, tom: LocalDate?): Saksopplysning {
        val periodeListeResponse = medlemskapRestConsumer.hentPeriodeListe(fnr, fom, tom)

        return Saksopplysning().apply {
            type = SaksopplysningType.MEDL
            versjon = MEDLEMSKAP_VERSJON
            dokument = MedlemskapDokument().apply {
                medlemsperiode = periodeListeResponse.map {
                    Medlemsperiode().apply {
                        id = it.unntakId
                        periode = Periode(it.fraOgMed, it.tilOgMed)
                        type = if (it.medlem!!) "PMMEDSKP" else "PUMEDSKP"
                        status = it.status
                        grunnlagstype = it.grunnlag
                        land = it.lovvalgsland
                        lovvalg = it.lovvalg
                        trygdedekning = it.dekning
                        kildedokumenttype = it.sporingsinformasjon!!.kildedokument
                        kilde = it.sporingsinformasjon!!.kilde
                    }
                }
            }
            try {
                leggTilKildesystemOgMottattDokument(
                    SaksopplysningKildesystem.MEDL,
                    objectMapper.writeValueAsString(periodeListeResponse)
                )
            } catch (e: JsonProcessingException) {
                throw TekniskException("Kunne ikke lagre kildedokument fra MEDL")
            }
        }
    }

    fun opprettPeriodeEndelig(
        fnr: String, periodeMedBestemmelse: HarBestemmelse<*>, kildedokumenttypeMedl: KildedokumenttypeMedl
    ): Long? {
        return opprettPeriode(
            fnr,
            periodeMedBestemmelse,
            PeriodestatusMedl.GYLD,
            LovvalgMedl.ENDL,
            kildedokumenttypeMedl
        )
    }

    fun opprettPeriodeUnderAvklaring(
        fnr: String?, periodeOmLovvalg: PeriodeOmLovvalg?, kildedokumenttypeMedl: KildedokumenttypeMedl?
    ): Long? {
        return opprettPeriode(
            fnr!!, periodeOmLovvalg!!, PeriodestatusMedl.UAVK, LovvalgMedl.UAVK, kildedokumenttypeMedl!!
        )
    }

    fun oppdaterPeriodeUnderAvklaring(
        periodeOmLovvalg: PeriodeOmLovvalg?,
        kildedokumenttypeMedl: KildedokumenttypeMedl?
    ) {
        oppdaterPeriode(periodeOmLovvalg!!, PeriodestatusMedl.UAVK, LovvalgMedl.UAVK, kildedokumenttypeMedl!!)
    }


    fun opprettPeriodeForeløpig(
        fnr: String?, periodeOmLovvalg: PeriodeOmLovvalg?, kildedokumenttypeMedl: KildedokumenttypeMedl?
    ): Long? {
        return opprettPeriode(
            fnr!!, periodeOmLovvalg!!, PeriodestatusMedl.UAVK, LovvalgMedl.FORL, kildedokumenttypeMedl!!
        )
    }

    fun oppdaterPeriodeEndelig(
        periodeMedBestemmelse: HarBestemmelse<*>?,
        kildedokumenttypeMedl: KildedokumenttypeMedl?
    ) {
        oppdaterPeriode(periodeMedBestemmelse!!, PeriodestatusMedl.GYLD, LovvalgMedl.ENDL, kildedokumenttypeMedl!!)
    }

    fun oppdaterPeriodeForeløpig(lovvalgsperiode: Lovvalgsperiode?, kildedokumenttypeMedl: KildedokumenttypeMedl?) {
        oppdaterPeriode(lovvalgsperiode!!, PeriodestatusMedl.UAVK, LovvalgMedl.FORL, kildedokumenttypeMedl!!)
    }

    fun avvisPeriode(medlPeriodeID: Long, årsak: StatusaarsakMedl) {
        val eksisterendePeriode = hentEksisterendePeriode(medlPeriodeID)
        val request = MedlemskapsunntakForPut(
            unntakId = eksisterendePeriode.unntakId,
            fraOgMed = eksisterendePeriode.fraOgMed,
            tilOgMed = eksisterendePeriode.tilOgMed,
            status = PeriodestatusMedl.AVST.kode,
            statusaarsak = årsak.kode,
            dekning = eksisterendePeriode.dekning,
            lovvalgsland = eksisterendePeriode.lovvalgsland,
            lovvalg = LovvalgMedl.ENDL.kode,
            grunnlag = eksisterendePeriode.grunnlag,
            sporingsinformasjon = MedlemskapsunntakForPut.SporingsinformasjonForPut(
                kildedokument = eksisterendePeriode.sporingsinformasjon!!.kildedokument,
                versjon = eksisterendePeriode.sporingsinformasjon!!.versjon
            )
        )

        medlemskapRestConsumer.oppdaterPeriode(request)
    }

    private fun opprettPeriode(
        fnr: String, periodeMedBestemmelse: HarBestemmelse<*>, periodestatusMedl: PeriodestatusMedl,
        lovvalgMedl: LovvalgMedl, kildedokumenttypeMedl: KildedokumenttypeMedl
    ): Long? {
        val request: MedlemskapsunntakForPost = when (periodeMedBestemmelse) {
            is PeriodeOmLovvalg -> lovvalgRequestForPost(periodeMedBestemmelse)
            is Medlemskapsperiode -> medlemskapsperiodeRequestForPost(periodeMedBestemmelse)
            else -> throw TekniskException("Oppretting av periode i MEDL feilet")
        }.apply {
            sporingsinformasjon = MedlemskapsunntakForPost.SporingsinformasjonForPost(
                kildedokument = kildedokumenttypeMedl.getKode()
            )
            ident = fnr
            status = periodestatusMedl.kode
            lovvalg = lovvalgMedl.kode
        }
        return medlemskapRestConsumer.opprettPeriode(request).unntakId
    }

    private fun lovvalgRequestForPost(periodeOmLovvalg: PeriodeOmLovvalg): MedlemskapsunntakForPost {
        val overgangsregelbestemmelser =
            (periodeOmLovvalg.behandlingsresultat?.behandling?.mottatteOpplysninger?.mottatteOpplysningerData
                as? SedGrunnlag)?.overgangsregelbestemmelser ?: listOf<Overgangsregelbestemmelser>()

        val lovvalgBestemmelse = MedlPeriodeKonverter.hentLovvalgBestemmelse(
            periodeOmLovvalg
        )

        if (harOvergangsregler(lovvalgBestemmelse) && overgangsregelbestemmelser.isEmpty()) {
            throw FunksjonellException("Grunnlaget ${lovvalgBestemmelse.kode} og overgangsregler skal benyttes, men er tom.")
        }

        val grunnlag =
            if (harOvergangsregler(lovvalgBestemmelse)) MedlPeriodeKonverter.tilGrunnlagMedltypeFraOvergangsregler(
                overgangsregelbestemmelser[0]
            ).kode else MedlPeriodeKonverter.tilGrunnlagMedltype(
                lovvalgBestemmelse
            ).kode

        return MedlemskapsunntakForPost(
            fraOgMed = periodeOmLovvalg.fom,
            tilOgMed = periodeOmLovvalg.tom,
            dekning = MedlPeriodeKonverter.tilMedlTrygdeDekning(periodeOmLovvalg.dekning).kode,
            lovvalgsland = IsoLandkodeKonverterer.tilIso3(periodeOmLovvalg.lovvalgsland.kode),
            grunnlag = grunnlag
        )
    }

    private fun medlemskapsperiodeRequestForPost(medlemskapsperiode: Medlemskapsperiode): MedlemskapsunntakForPost =
        MedlemskapsunntakForPost(
            fraOgMed = medlemskapsperiode.fom,
            tilOgMed = medlemskapsperiode.tom,
            dekning = MedlPeriodeKonverter.tilMedlTrygdeDekning(
                medlemskapsperiode.trygdedekning,
                medlemskapsperiode.bestemmelse
            ).kode,
            lovvalgsland = IsoLandkodeKonverterer.tilIso3(Land_iso2.NO.kode),
            grunnlag = MedlPeriodeKonverter.tilGrunnlagMedltype(
                medlemskapsperiode.bestemmelse,
            ).kode
        )

    private fun oppdaterPeriode(
        periodeMedBestemmelse: HarBestemmelse<*>,
        periodestatusMedl: PeriodestatusMedl,
        lovvalgMedl: LovvalgMedl,
        kildedokumenttypeMedl: KildedokumenttypeMedl
    ) {
        val request: MedlemskapsunntakForPut = when (periodeMedBestemmelse) {
            is PeriodeOmLovvalg -> lovvalgRequestForPut(periodeMedBestemmelse, kildedokumenttypeMedl)
            is Medlemskapsperiode -> medlemskapsperiodeRequestForPut(periodeMedBestemmelse, kildedokumenttypeMedl)
            else -> throw TekniskException("Oppretting av periode i MEDL feilet")
        }.apply {
            status = periodestatusMedl.kode
            lovvalg = lovvalgMedl.kode
        }

        medlemskapRestConsumer.oppdaterPeriode(request)
    }

    private fun lovvalgRequestForPut(
        periodeOmLovvalg: PeriodeOmLovvalg,
        kildedokumenttypeMedl: KildedokumenttypeMedl
    ): MedlemskapsunntakForPut {
        val medlPeriodeID = periodeOmLovvalg.medlPeriodeID
            ?: throw TekniskException("Det er ikke lagret noen medlPeriodeID på lovvalgsperiode som skal oppdateres i MEDL")
        val eksisterendePeriode = hentEksisterendePeriode(medlPeriodeID)

        return MedlemskapsunntakForPut(
            unntakId = medlPeriodeID,
            fraOgMed = periodeOmLovvalg.fom,
            tilOgMed = periodeOmLovvalg.tom,
            dekning = MedlPeriodeKonverter.tilMedlTrygdeDekning(periodeOmLovvalg.dekning).kode,
            lovvalgsland = IsoLandkodeKonverterer.tilIso3(periodeOmLovvalg.lovvalgsland.kode),
            grunnlag = MedlPeriodeKonverter.tilGrunnlagMedltype(
                MedlPeriodeKonverter.hentLovvalgBestemmelse(
                    periodeOmLovvalg
                )
            ).kode,
            sporingsinformasjon = MedlemskapsunntakForPut.SporingsinformasjonForPut(
                kildedokument = kildedokumenttypeMedl.getKode(),
                versjon = eksisterendePeriode.sporingsinformasjon!!.versjon
            )
        )
    }

    private fun medlemskapsperiodeRequestForPut(
        medlemskapsperiode: Medlemskapsperiode,
        kildedokumenttypeMedl: KildedokumenttypeMedl
    ): MedlemskapsunntakForPut {
        val medlPeriodeID = medlemskapsperiode.medlPeriodeID
            ?: throw TekniskException("Det er ikke lagret noen medlPeriodeID på medlemskapsperiode som skal oppdateres i MEDL")
        val eksisterendePeriode = hentEksisterendePeriode(medlPeriodeID)

        return MedlemskapsunntakForPut(
            unntakId = medlPeriodeID,
            fraOgMed = medlemskapsperiode.fom,
            tilOgMed = medlemskapsperiode.tom,
            dekning = MedlPeriodeKonverter.tilMedlTrygdeDekning(
                medlemskapsperiode.trygdedekning,
                medlemskapsperiode.bestemmelse
            ).kode,
            lovvalgsland = IsoLandkodeKonverterer.tilIso3(Land_iso2.NO.kode),
            grunnlag = MedlPeriodeKonverter.tilGrunnlagMedltype(
                medlemskapsperiode.bestemmelse,
            ).kode,
            sporingsinformasjon = MedlemskapsunntakForPut.SporingsinformasjonForPut(
                kildedokument = kildedokumenttypeMedl.getKode(),
                versjon = eksisterendePeriode.sporingsinformasjon!!.versjon
            )
        )
    }


    private fun harOvergangsregler(bestemmelse: LovvalgBestemmelse?): Boolean {
        return bestemmelse == Tilleggsbestemmelser_883_2004.FO_883_2004_ART87_8 || bestemmelse == Tilleggsbestemmelser_883_2004.FO_883_2004_ART87A
    }

    private fun hentEksisterendePeriode(medlPeriodeID: Long): MedlemskapsunntakForGet {
        return medlemskapRestConsumer.hentPeriode(medlPeriodeID.toString())
    }

    companion object {
        const val MEDLEMSKAP_VERSJON = "2.0"
    }
}


