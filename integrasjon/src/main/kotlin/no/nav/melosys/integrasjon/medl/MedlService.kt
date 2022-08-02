package no.nav.melosys.integrasjon.medl

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import no.nav.melosys.domain.*
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode
import no.nav.melosys.domain.dokument.medlemskap.Periode
import no.nav.melosys.domain.util.LandkoderUtils
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

    fun hentPeriodeListe(fnr: String, fom: LocalDate, tom: LocalDate): Saksopplysning {
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
        fnr: String?, bestemmelse: HarBestemmelse<*>?, kildedokumenttypeMedl: KildedokumenttypeMedl?
    ): Long? {
        return opprettPeriode(fnr!!, bestemmelse!!, PeriodestatusMedl.GYLD, LovvalgMedl.ENDL, kildedokumenttypeMedl!!)
    }

    fun opprettPeriodeUnderAvklaring(
        fnr: String?, periodeOmLovvalg: PeriodeOmLovvalg?, kildedokumenttypeMedl: KildedokumenttypeMedl?
    ): Long? {
        return opprettPeriode(
            fnr!!, periodeOmLovvalg!!, PeriodestatusMedl.UAVK, LovvalgMedl.UAVK, kildedokumenttypeMedl!!
        )
    }

    fun opprettPeriodeForeløpig(
        fnr: String?, periodeOmLovvalg: PeriodeOmLovvalg?, kildedokumenttypeMedl: KildedokumenttypeMedl?
    ): Long? {
        return opprettPeriode(
            fnr!!, periodeOmLovvalg!!, PeriodestatusMedl.UAVK, LovvalgMedl.FORL, kildedokumenttypeMedl!!
        )
    }

    fun oppdaterPeriodeEndelig(lovvalgsperiode: Lovvalgsperiode?, kildedokumenttypeMedl: KildedokumenttypeMedl?) {
        oppdaterPeriode(lovvalgsperiode!!, PeriodestatusMedl.GYLD, LovvalgMedl.ENDL, kildedokumenttypeMedl!!)
    }

    fun oppdaterPeriodeForeløpig(lovvalgsperiode: Lovvalgsperiode?, kildedokumenttypeMedl: KildedokumenttypeMedl?) {
        oppdaterPeriode(lovvalgsperiode!!, PeriodestatusMedl.UAVK, LovvalgMedl.FORL, kildedokumenttypeMedl!!)
    }

    fun avvisPeriode(medlPeriodeID: Long?, årsak: StatusaarsakMedl) {
        val eksisterendePeriode = hentEksisterendePeriode(medlPeriodeID!!)
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
        fnr: String, bestemmelse: HarBestemmelse<*>, periodestatusMedl: PeriodestatusMedl,
        lovvalgMedl: LovvalgMedl, kildedokumenttypeMedl: KildedokumenttypeMedl
    ): Long? {
        val medlemskapsunntakForPost = when (bestemmelse) {
            is PeriodeOmLovvalg -> lovvalgRequest(bestemmelse)
            is Medlemskapsperiode -> medlemskapsperiodeRequest(bestemmelse)
            else -> throw TekniskException("Oppretting av periode i MEDL feilet")
        }.apply {
            sporingsinformasjon = MedlemskapsunntakForPost.SporingsinformasjonForPost(
                kildedokument = kildedokumenttypeMedl.getKode()
            )
            ident = fnr
            status = periodestatusMedl.kode
            lovvalg = lovvalgMedl.kode
        }
        return medlemskapRestConsumer.opprettPeriode(medlemskapsunntakForPost).unntakId
    }

    fun oppdaterDatoForPeriode(medlPeriodeID: Long?, fom: LocalDate?, tom: LocalDate?) {
        val request = MedlemskapsunntakForPut(
            unntakId = medlPeriodeID,
            fraOgMed = fom,
            tilOgMed = tom // TODO: Antar flere av feltene her er obligatoriske. Kopiere fra forrige periode? Bruke standardverdier?
        )
        medlemskapRestConsumer.oppdaterPeriode(request)
    }

    private fun lovvalgRequest(periodeOmLovvalg: PeriodeOmLovvalg) =
        MedlemskapsunntakForPost(
            fraOgMed = periodeOmLovvalg.fom,
            tilOgMed = periodeOmLovvalg.tom,
            dekning = MedlPeriodeKonverter.tilMedlTrygdeDekningEos(periodeOmLovvalg.dekning).kode,
            lovvalgsland = LandkoderUtils.tilIso3(periodeOmLovvalg.lovvalgsland.kode),
            grunnlag = MedlPeriodeKonverter.tilGrunnlagMedltype(
                MedlPeriodeKonverter.hentLovvalgBestemmelse(
                    periodeOmLovvalg
                )
            ).kode
        )

    private fun medlemskapsperiodeRequest(medlemskapsperiode: Medlemskapsperiode) =
        MedlemskapsunntakForPost(
            fraOgMed = medlemskapsperiode.fom,
            tilOgMed = medlemskapsperiode.tom,
            dekning = MedlPeriodeKonverter.tilMedlTrygdeDekningFtrl(
                medlemskapsperiode.dekning,
                medlemskapsperiode.bestemmelse
            ).kode,
            lovvalgsland = LandkoderUtils.tilIso3(medlemskapsperiode.arbeidsland),
            grunnlag = MedlPeriodeKonverter.tilGrunnlagMedltype(medlemskapsperiode.bestemmelse).kode
        )

    private fun oppdaterPeriode(
        lovvalgsperiode: Lovvalgsperiode, periodestatusMedl: PeriodestatusMedl,
        lovvalgMedl: LovvalgMedl, kildedokumenttypeMedl: KildedokumenttypeMedl
    ) {
        val medlPeriodeID = lovvalgsperiode.medlPeriodeID
            ?: throw TekniskException("Det er ikke lagret noen medlPeriodeID på lovvalgsperiode som skal oppdateres i MEDL")
        val eksisterendePeriode = hentEksisterendePeriode(medlPeriodeID)

        val request = MedlemskapsunntakForPut(
            unntakId = medlPeriodeID,
            fraOgMed = lovvalgsperiode.fom,
            tilOgMed = lovvalgsperiode.tom,
            status = periodestatusMedl.kode,
            dekning = MedlPeriodeKonverter.tilMedlTrygdeDekningEos(lovvalgsperiode.dekning).kode,
            lovvalgsland = LandkoderUtils.tilIso3(lovvalgsperiode.lovvalgsland.kode),
            lovvalg = lovvalgMedl.kode,
            grunnlag = MedlPeriodeKonverter.tilGrunnlagMedltype(
                MedlPeriodeKonverter.hentLovvalgBestemmelse(
                    lovvalgsperiode
                )
            ).kode,
            sporingsinformasjon = MedlemskapsunntakForPut.SporingsinformasjonForPut(
                kildedokument = kildedokumenttypeMedl.getKode(),
                versjon = eksisterendePeriode.sporingsinformasjon!!.versjon
            )
        )
        medlemskapRestConsumer.oppdaterPeriode(request)
    }

    private fun hentEksisterendePeriode(medlPeriodeID: Long): MedlemskapsunntakForGet {
        return medlemskapRestConsumer.hentPeriode(medlPeriodeID.toString())
    }

    companion object {
        const val MEDLEMSKAP_VERSJON = "2.0"
    }
}


