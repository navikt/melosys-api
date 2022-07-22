package no.nav.melosys.integrasjon.medl

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import no.nav.melosys.domain.*
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode
import no.nav.melosys.domain.dokument.medlemskap.Periode
import no.nav.melosys.domain.util.LandkoderUtils
import no.nav.melosys.ekstern.tjenester.medlemskapsunntak.api.v1.MedlemskapsunntakForGet
import no.nav.melosys.ekstern.tjenester.medlemskapsunntak.api.v1.MedlemskapsunntakForPost
import no.nav.melosys.ekstern.tjenester.medlemskapsunntak.api.v1.MedlemskapsunntakForPost.MedlemskapsunntakForPostBuilder
import no.nav.melosys.ekstern.tjenester.medlemskapsunntak.api.v1.MedlemskapsunntakForPut
import no.nav.melosys.exception.TekniskException
import java.time.LocalDate

class MedlService(
    private val medlemskapRestConsumer: MedlemskapRestConsumer,
    private val objectMapper: ObjectMapper
) {

    init {
        objectMapper.registerModule(JavaTimeModule())
    }

    fun hentPeriodeListe(fnr: String, fom: LocalDate, tom: LocalDate): Saksopplysning {
        val periodeListeResponse = medlemskapRestConsumer.hentPeriodeListe(fnr, fom, tom)
        val medlemskapDokument = MedlemskapDokument()
        val medlemsperioder: MutableList<Medlemsperiode> = ArrayList()
        for (m in periodeListeResponse) {
            val medlemsperiode = Medlemsperiode()
            medlemsperiode.id = m.unntakId
            medlemsperiode.periode = Periode(m.fraOgMed, m.tilOgMed)
            medlemsperiode.type = if (m.medlem) "PMMEDSKP" else "PUMEDSKP"
            medlemsperiode.status = m.status
            medlemsperiode.grunnlagstype = m.grunnlag
            medlemsperiode.land = m.lovvalgsland
            medlemsperiode.lovvalg = m.lovvalg
            medlemsperiode.trygdedekning = m.dekning
            val sporingsinformasjon = m.sporingsinformasjon
            medlemsperiode.kildedokumenttype = sporingsinformasjon.kildedokument
            medlemsperiode.kilde = sporingsinformasjon.kilde
            medlemsperioder.add(medlemsperiode)
        }
        medlemskapDokument.medlemsperiode = medlemsperioder
        val saksopplysning = Saksopplysning()
        saksopplysning.type = SaksopplysningType.MEDL
        saksopplysning.versjon = MEDLEMSKAP_VERSJON
        saksopplysning.dokument = medlemskapDokument
        try {
            saksopplysning.leggTilKildesystemOgMottattDokument(
                SaksopplysningKildesystem.MEDL,
                objectMapper.writeValueAsString(periodeListeResponse)
            )
        } catch (e: JsonProcessingException) {
            throw TekniskException("Kunne ikke lagre kildedokument fra MEDL")
        }
        return saksopplysning
    }

    fun opprettPeriodeEndelig(
        fnr: String?,
        bestemmelse: HarBestemmelse<*>?,
        kildedokumenttypeMedl: KildedokumenttypeMedl?
    ): Long? {
        return opprettPeriode(fnr!!, bestemmelse!!, PeriodestatusMedl.GYLD, LovvalgMedl.ENDL, kildedokumenttypeMedl!!)
    }

    fun opprettPeriodeUnderAvklaring(
        fnr: String?,
        periodeOmLovvalg: PeriodeOmLovvalg?,
        kildedokumenttypeMedl: KildedokumenttypeMedl?
    ): Long? {
        return opprettPeriode(
            fnr!!, periodeOmLovvalg!!, PeriodestatusMedl.UAVK, LovvalgMedl.UAVK,
            kildedokumenttypeMedl!!
        )
    }

    fun opprettPeriodeForeløpig(
        fnr: String?,
        periodeOmLovvalg: PeriodeOmLovvalg?,
        kildedokumenttypeMedl: KildedokumenttypeMedl?
    ): Long? {
        return opprettPeriode(
            fnr!!, periodeOmLovvalg!!, PeriodestatusMedl.UAVK, LovvalgMedl.FORL,
            kildedokumenttypeMedl!!
        )
    }

    fun oppdaterPeriodeEndelig(lovvalgsperiode: Lovvalgsperiode?, kildedokumenttypeMedl: KildedokumenttypeMedl?) {
        oppdaterPeriode(lovvalgsperiode!!, PeriodestatusMedl.GYLD, LovvalgMedl.ENDL, kildedokumenttypeMedl!!)
    }

    fun oppdaterPeriodeForeløpig(lovvalgsperiode: Lovvalgsperiode?, kildedokumenttypeMedl: KildedokumenttypeMedl?) {
        oppdaterPeriode(lovvalgsperiode!!, PeriodestatusMedl.UAVK, LovvalgMedl.FORL, kildedokumenttypeMedl!!)
    }

    fun avvisPeriode(medlPeriodeID: Long?, årsak: StatusaarsakMedl) {
        val eksisterendePeriode = hentEksisterendePeriode(
            medlPeriodeID!!
        )
        val sporingsinformasjon = MedlemskapsunntakForPut.SporingsinformasjonForPut.builder()
            .kildedokument(eksisterendePeriode!!.sporingsinformasjon.kildedokument)
            .versjon(eksisterendePeriode.sporingsinformasjon.versjon)
            .build()
        val request = MedlemskapsunntakForPut.builder()
            .unntakId(eksisterendePeriode.unntakId)
            .fraOgMed(eksisterendePeriode.fraOgMed)
            .tilOgMed(eksisterendePeriode.tilOgMed)
            .status(PeriodestatusMedl.AVST.kode)
            .statusaarsak(årsak.kode)
            .dekning(eksisterendePeriode.dekning)
            .lovvalgsland(eksisterendePeriode.lovvalgsland)
            .lovvalg(LovvalgMedl.ENDL.kode)
            .grunnlag(eksisterendePeriode.grunnlag)
            .sporingsinformasjon(sporingsinformasjon)
            .build()
        medlemskapRestConsumer.oppdaterPeriode(request)
    }
    private fun opprettPeriode(
        fnr: String, bestemmelse: HarBestemmelse<*>, periodestatusMedl: PeriodestatusMedl,
        lovvalgMedl: LovvalgMedl, kildedokumenttypeMedl: KildedokumenttypeMedl
    ): Long? {

        var request: MedlemskapsunntakForPostBuilder? = null
        if (bestemmelse is PeriodeOmLovvalg) {
            request = lovvalgRequest(bestemmelse)
        } else if (bestemmelse is Medlemskapsperiode) {
            request = medlemskapsperiodeRequest(bestemmelse)
        }

        if (request == null) {
            throw TekniskException("Oppretting av periode i MEDL feilet")
        }

        val sporingsinformasjon = MedlemskapsunntakForPost.SporingsinformasjonForPost.builder()
            .kildedokument(kildedokumenttypeMedl.getKode())
            .build()

        request
            .sporingsinformasjon(sporingsinformasjon)
            .ident(fnr)
            .status(periodestatusMedl.kode)
            .lovvalg(lovvalgMedl.kode)

        return medlemskapRestConsumer.opprettPeriode(request.build())!!.unntakId
    }

    private fun lovvalgRequest(periodeOmLovvalg: PeriodeOmLovvalg): MedlemskapsunntakForPostBuilder? {
        return MedlemskapsunntakForPost.builder()
            .fraOgMed(periodeOmLovvalg.fom)
            .tilOgMed(periodeOmLovvalg.tom)
            .dekning(MedlPeriodeKonverter.tilMedlTrygdeDekningEos(periodeOmLovvalg.dekning).kode)
            .lovvalgsland(LandkoderUtils.tilIso3(periodeOmLovvalg.lovvalgsland.kode))
            .grunnlag(
                MedlPeriodeKonverter.tilGrunnlagMedltype(
                    MedlPeriodeKonverter.hentLovvalgBestemmelse(
                        periodeOmLovvalg
                    )
                ).kode
            )
    }
    private fun medlemskapsperiodeRequest(medlemskapsperiode: Medlemskapsperiode): MedlemskapsunntakForPostBuilder? {
        return MedlemskapsunntakForPost.builder()
            .fraOgMed(medlemskapsperiode.fom)
            .tilOgMed(medlemskapsperiode.tom)
            .dekning(
                MedlPeriodeKonverter.tilMedlTrygdeDekningFtrl(
                    medlemskapsperiode.dekning,
                    medlemskapsperiode.bestemmelse
                ).kode
            )
            .lovvalgsland(LandkoderUtils.tilIso3(medlemskapsperiode.arbeidsland))
            .grunnlag(MedlPeriodeKonverter.tilGrunnlagMedltype(medlemskapsperiode.bestemmelse).kode)
    }

    private fun oppdaterPeriode(
        lovvalgsperiode: Lovvalgsperiode, periodestatusMedl: PeriodestatusMedl,
        lovvalgMedl: LovvalgMedl, kildedokumenttypeMedl: KildedokumenttypeMedl
    ) {
        val medlPeriodeID = lovvalgsperiode.medlPeriodeID
            ?: throw TekniskException("Det er ikke lagret noen medlPeriodeID på lovvalgsperiode som skal oppdateres i MEDL")
        val eksisterendePeriode = hentEksisterendePeriode(medlPeriodeID)
        val sporingsinformasjon = MedlemskapsunntakForPut.SporingsinformasjonForPut.builder()
            .kildedokument(kildedokumenttypeMedl.getKode())
            .versjon(eksisterendePeriode!!.sporingsinformasjon.versjon)
            .build()
        val bestemmelse = MedlPeriodeKonverter.hentLovvalgBestemmelse(lovvalgsperiode)
        val request = MedlemskapsunntakForPut.builder()
            .unntakId(medlPeriodeID)
            .fraOgMed(lovvalgsperiode.fom)
            .tilOgMed(lovvalgsperiode.tom)
            .status(periodestatusMedl.kode)
            .dekning(MedlPeriodeKonverter.tilMedlTrygdeDekningEos(lovvalgsperiode.dekning).kode)
            .lovvalgsland(LandkoderUtils.tilIso3(lovvalgsperiode.lovvalgsland.kode))
            .lovvalg(lovvalgMedl.kode)
            .grunnlag(MedlPeriodeKonverter.tilGrunnlagMedltype(bestemmelse).kode)
            .sporingsinformasjon(sporingsinformasjon)
            .build()
        medlemskapRestConsumer.oppdaterPeriode(request)
    }
    private fun hentEksisterendePeriode(medlPeriodeID: Long): MedlemskapsunntakForGet? {
        return medlemskapRestConsumer.hentPeriode(medlPeriodeID.toString())
    }

    companion object {
        const val MEDLEMSKAP_VERSJON = "2.0"
    }
}


