package no.nav.melosys.integrasjon.medl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.integrasjon.medl.api.v1.MedlemskapsunntakForGet
import no.nav.melosys.integrasjon.medl.api.v1.MedlemskapsunntakForPost
import no.nav.melosys.integrasjon.medl.api.v1.MedlemskapsunntakForPut
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import java.io.IOException
import java.time.LocalDate
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MedlServiceTest {
    private var medlService: MedlService? = null
    private val FNR = "77777777773"
    private var mockRestConsumer = Mockito.mock(MedlemskapRestConsumer::class.java)
    private var objectMapper = ObjectMapper()

    @BeforeAll
    fun init() {
        objectMapper.registerModule(JavaTimeModule())
        medlService = MedlService(mockRestConsumer, objectMapper)
    }

    @BeforeEach
    fun setUp() {
        Mockito.reset(mockRestConsumer)
    }

    @Test
    @Throws(Exception::class)
    fun skalHentPeriodeliste() {
        Mockito.`when`(
            mockRestConsumer!!.hentPeriodeListe(
                ArgumentMatchers.eq(FNR),
                ArgumentMatchers.any(),
                ArgumentMatchers.any()
            )
        ).thenReturn(
            Arrays.asList(*hentJsonResponse("gyldigPeriodelisteResponse", Array<MedlemskapsunntakForGet>::class.java))
        )
        val saksopplysning = medlService!!.hentPeriodeListe(FNR, null, null)
        Assertions.assertThat(saksopplysning).isNotNull
        Assertions.assertThat(saksopplysning.kilder).isNotEmpty
        Assertions.assertThat(saksopplysning.kilder.iterator().next().mottattDokument).isNotNull
        val medlemskapDokument = saksopplysning.dokument as MedlemskapDokument
        Assertions.assertThat(medlemskapDokument).isNotNull
        Assertions.assertThat(medlemskapDokument.getMedlemsperiode()).hasSize(1)
        val medlemsperiode = medlemskapDokument.getMedlemsperiode()[0]
        Assertions.assertThat(medlemsperiode.getType()).isEqualTo("PUMEDSKP")
        Assertions.assertThat(medlemsperiode.getStatus()).isEqualTo(PeriodestatusMedl.GYLD.kode)
        Assertions.assertThat(medlemsperiode.getLovvalg()).isEqualTo(LovvalgMedl.ENDL.kode)
        Assertions.assertThat(medlemsperiode.getKilde()).isEqualTo("INFOTR")
        Assertions.assertThat(medlemsperiode.getGrunnlagstype()).isEqualTo("IMEDEOS")
    }

    @Test
    @Throws(Exception::class)
    fun skalOpprettPeriodeEndelig() {
        Mockito.`when`(mockRestConsumer!!.opprettPeriode(ArgumentMatchers.any())).thenReturn(
            hentJsonResponse("gyldigPeriodeResponse", MedlemskapsunntakForGet::class.java)
        )
        val lovvalgsperiode = lagLovvalgsPeriode()
        medlService!!.opprettPeriodeEndelig(FNR, lovvalgsperiode, KildedokumenttypeMedl.HENV_SOKNAD)
        val captor = ArgumentCaptor.forClass(
            MedlemskapsunntakForPost::class.java
        )
        Mockito.verify(mockRestConsumer).opprettPeriode(captor.capture())
        val request = captor.value
        Assertions.assertThat(request.ident).isEqualTo(FNR)
        Assertions.assertThat(request.fraOgMed).isEqualTo(lovvalgsperiode.fom)
        Assertions.assertThat(request.tilOgMed).isEqualTo(lovvalgsperiode.tom)
        Assertions.assertThat(request.status).isEqualTo(PeriodestatusMedl.GYLD.kode)
        Assertions.assertThat(request.dekning).isEqualTo(DekningMedl.FULL.kode)
        Assertions.assertThat(request.lovvalgsland).isEqualTo("BEL")
        Assertions.assertThat(request.lovvalg).isEqualTo(LovvalgMedl.ENDL.kode)
        Assertions.assertThat(request.grunnlag).isEqualTo("FO_11_4_1")
        Assertions.assertThat(request.sporingsinformasjon!!.kildedokument)
            .isEqualTo(KildedokumenttypeMedl.HENV_SOKNAD.getKode())
    }

    @Test
    @Throws(Exception::class)
    fun skalOpprettPeriodeUnderAvklaring() {
        Mockito.`when`(mockRestConsumer!!.opprettPeriode(ArgumentMatchers.any())).thenReturn(
            hentJsonResponse("gyldigPeriodeResponse", MedlemskapsunntakForGet::class.java)
        )
        val lovvalgsperiode = lagLovvalgsPeriode()
        medlService!!.opprettPeriodeUnderAvklaring(FNR, lovvalgsperiode, KildedokumenttypeMedl.HENV_SOKNAD)
        val captor = ArgumentCaptor.forClass(
            MedlemskapsunntakForPost::class.java
        )
        Mockito.verify(mockRestConsumer).opprettPeriode(captor.capture())
        val request = captor.value
        Assertions.assertThat(request.ident).isEqualTo(FNR)
        Assertions.assertThat(request.fraOgMed).isEqualTo(lovvalgsperiode.fom)
        Assertions.assertThat(request.tilOgMed).isEqualTo(lovvalgsperiode.tom)
        Assertions.assertThat(request.status).isEqualTo(LovvalgMedl.UAVK.kode)
        Assertions.assertThat(request.dekning).isEqualTo(DekningMedl.FULL.kode)
        Assertions.assertThat(request.lovvalgsland).isEqualTo("BEL")
        Assertions.assertThat(request.lovvalg).isEqualTo(LovvalgMedl.UAVK.kode)
        Assertions.assertThat(request.grunnlag).isEqualTo("FO_11_4_1")
        Assertions.assertThat(request.sporingsinformasjon!!.kildedokument)
            .isEqualTo(KildedokumenttypeMedl.HENV_SOKNAD.getKode())
    }

    @Test
    @Throws(Exception::class)
    fun skalOpprettPeriodeForeløpig() {
        Mockito.`when`(mockRestConsumer!!.opprettPeriode(ArgumentMatchers.any())).thenReturn(
            hentJsonResponse("gyldigPeriodeResponse", MedlemskapsunntakForGet::class.java)
        )
        val lovvalgsperiode = lagLovvalgsPeriode()
        medlService!!.opprettPeriodeForeløpig(FNR, lovvalgsperiode, KildedokumenttypeMedl.HENV_SOKNAD)
        val captor = ArgumentCaptor.forClass(
            MedlemskapsunntakForPost::class.java
        )
        Mockito.verify(mockRestConsumer).opprettPeriode(captor.capture())
        val request = captor.value
        Assertions.assertThat(request.ident).isEqualTo(FNR)
        Assertions.assertThat(request.fraOgMed).isEqualTo(lovvalgsperiode.fom)
        Assertions.assertThat(request.tilOgMed).isEqualTo(lovvalgsperiode.tom)
        Assertions.assertThat(request.status).isEqualTo(LovvalgMedl.UAVK.kode)
        Assertions.assertThat(request.dekning).isEqualTo(DekningMedl.FULL.kode)
        Assertions.assertThat(request.lovvalgsland).isEqualTo("BEL")
        Assertions.assertThat(request.lovvalg).isEqualTo(LovvalgMedl.FORL.kode)
        Assertions.assertThat(request.grunnlag).isEqualTo("FO_11_4_1")
        Assertions.assertThat(request.sporingsinformasjon!!.kildedokument)
            .isEqualTo(KildedokumenttypeMedl.HENV_SOKNAD.getKode())
    }

    @Test
    @Throws(Exception::class)
    fun skalOppdaterePeriodeEndelig() {
        Mockito.`when`(mockRestConsumer!!.hentPeriode(ArgumentMatchers.any())).thenReturn(
            hentJsonResponse("gyldigPeriodeResponse", MedlemskapsunntakForGet::class.java)
        )
        Mockito.`when`(mockRestConsumer!!.oppdaterPeriode(ArgumentMatchers.any())).thenReturn(
            hentJsonResponse("gyldigPeriodeResponse", MedlemskapsunntakForGet::class.java)
        )
        val lovvalgsperiode = lagLovvalgsPeriode()
        lovvalgsperiode.medlPeriodeID = 123456L
        medlService!!.oppdaterPeriodeEndelig(lovvalgsperiode, KildedokumenttypeMedl.HENV_SOKNAD)
        val captor = ArgumentCaptor.forClass(
            MedlemskapsunntakForPut::class.java
        )
        Mockito.verify(mockRestConsumer).oppdaterPeriode(captor.capture())
        val (unntakId, fraOgMed, tilOgMed, status, _, dekning, lovvalgsland, lovvalg, grunnlag, sporingsinformasjon) = captor.value
        Assertions.assertThat(unntakId).isEqualTo(123456L)
        Assertions.assertThat(fraOgMed).isEqualTo(lovvalgsperiode.fom)
        Assertions.assertThat(tilOgMed).isEqualTo(lovvalgsperiode.tom)
        Assertions.assertThat(status).isEqualTo(PeriodestatusMedl.GYLD.kode)
        Assertions.assertThat(dekning).isEqualTo(DekningMedl.FULL.kode)
        Assertions.assertThat(lovvalgsland).isEqualTo("BEL")
        Assertions.assertThat(lovvalg).isEqualTo(LovvalgMedl.ENDL.kode)
        Assertions.assertThat(grunnlag).isEqualTo("FO_11_4_1")
        Assertions.assertThat(sporingsinformasjon!!.kildedokument)
            .isEqualTo(KildedokumenttypeMedl.HENV_SOKNAD.getKode())
    }

    @Test
    @Throws(Exception::class)
    fun skalOpprettePeriodeEndeligFtrl() {
        Mockito.`when`(mockRestConsumer!!.opprettPeriode(ArgumentMatchers.any())).thenReturn(
            hentJsonResponse("gyldigPeriodeResponse", MedlemskapsunntakForGet::class.java)
        )
        val medlemskapsperiode = lagMedlemskapsPeriode()
        medlService!!.opprettPeriodeEndelig(FNR, medlemskapsperiode, KildedokumenttypeMedl.HENV_SOKNAD)
        val captor = ArgumentCaptor.forClass(
            MedlemskapsunntakForPost::class.java
        )
        Mockito.verify(mockRestConsumer).opprettPeriode(captor.capture())
        val request = captor.value
        Assertions.assertThat(request.fraOgMed).isEqualTo(medlemskapsperiode.fom)
        Assertions.assertThat(request.tilOgMed).isEqualTo(medlemskapsperiode.tom)
        Assertions.assertThat(request.status).isEqualTo(PeriodestatusMedl.GYLD.kode)
        Assertions.assertThat(request.dekning).isEqualTo(DekningMedl.FTRL_2_9_1_LEDD_A.kode)
        Assertions.assertThat(request.lovvalgsland).isEqualTo("BEL")
        Assertions.assertThat(request.lovvalg).isEqualTo(LovvalgMedl.ENDL.kode)
        Assertions.assertThat(request.grunnlag).isEqualTo(GrunnlagMedl.FTL_2_8.kode)
        Assertions.assertThat(request.sporingsinformasjon!!.kildedokument)
            .isEqualTo(KildedokumenttypeMedl.HENV_SOKNAD.getKode())
    }

    @Test
    @Throws(Exception::class)
    fun skalOppdaterePeriodeForeløpig() {
        Mockito.`when`(mockRestConsumer!!.hentPeriode(ArgumentMatchers.any())).thenReturn(
            hentJsonResponse("gyldigPeriodeResponse", MedlemskapsunntakForGet::class.java)
        )
        Mockito.`when`(mockRestConsumer!!.oppdaterPeriode(ArgumentMatchers.any())).thenReturn(
            hentJsonResponse("gyldigPeriodeResponse", MedlemskapsunntakForGet::class.java)
        )
        val lovvalgsperiode = lagLovvalgsPeriode()
        lovvalgsperiode.medlPeriodeID = 123456L
        medlService!!.oppdaterPeriodeForeløpig(lovvalgsperiode, KildedokumenttypeMedl.HENV_SOKNAD)
        val captor = ArgumentCaptor.forClass(
            MedlemskapsunntakForPut::class.java
        )
        Mockito.verify(mockRestConsumer).oppdaterPeriode(captor.capture())
        val (unntakId, fraOgMed, tilOgMed, status, _, dekning, lovvalgsland, lovvalg, grunnlag, sporingsinformasjon) = captor.value
        Assertions.assertThat(unntakId).isEqualTo(123456L)
        Assertions.assertThat(fraOgMed).isEqualTo(lovvalgsperiode.fom)
        Assertions.assertThat(tilOgMed).isEqualTo(lovvalgsperiode.tom)
        Assertions.assertThat(status).isEqualTo(LovvalgMedl.UAVK.kode)
        Assertions.assertThat(dekning).isEqualTo(DekningMedl.FULL.kode)
        Assertions.assertThat(lovvalgsland).isEqualTo("BEL")
        Assertions.assertThat(lovvalg).isEqualTo(LovvalgMedl.FORL.kode)
        Assertions.assertThat(grunnlag).isEqualTo("FO_11_4_1")
        Assertions.assertThat(sporingsinformasjon!!.kildedokument)
            .isEqualTo(KildedokumenttypeMedl.HENV_SOKNAD.getKode())
    }

    @Test
    fun oppdaterePeriodeFeilerMedManglendePeriodeId() {
        Assertions.assertThatExceptionOfType(TekniskException::class.java)
            .isThrownBy {
                medlService!!.oppdaterPeriodeForeløpig(
                    lagLovvalgsPeriode(),
                    KildedokumenttypeMedl.HENV_SOKNAD
                )
            }
            .withMessageContaining("Det er ikke lagret noen medlPeriodeID på lovvalgsperiode som skal oppdateres i MEDL")
    }

    @Test
    @Throws(Exception::class)
    fun skalAvvisePeriode() {
        Mockito.`when`(mockRestConsumer!!.hentPeriode(ArgumentMatchers.any())).thenReturn(
            hentJsonResponse("gyldigPeriodeResponse", MedlemskapsunntakForGet::class.java)
        )
        Mockito.`when`(mockRestConsumer!!.oppdaterPeriode(ArgumentMatchers.any())).thenReturn(
            hentJsonResponse("gyldigPeriodeResponse", MedlemskapsunntakForGet::class.java)
        )
        val lovvalgsperiode = lagLovvalgsPeriode()
        lovvalgsperiode.medlPeriodeID = 123456L
        medlService!!.avvisPeriode(123456L, StatusaarsakMedl.AVVIST)
        val captor = ArgumentCaptor.forClass(
            MedlemskapsunntakForPut::class.java
        )
        Mockito.verify(mockRestConsumer).oppdaterPeriode(captor.capture())
        val (unntakId, _, _, status, statusaarsak) = captor.value
        Assertions.assertThat(unntakId).isEqualTo(123456L)
        Assertions.assertThat(status).isEqualTo(PeriodestatusMedl.AVST.kode)
        Assertions.assertThat(statusaarsak).isEqualTo(StatusaarsakMedl.AVVIST.kode)
    }

    private fun lagLovvalgsPeriode(): Lovvalgsperiode {
        val lovvalgsperiode = Lovvalgsperiode()
        lovvalgsperiode.fom = LocalDate.now()
        lovvalgsperiode.tom = LocalDate.now().plusYears(1)
        lovvalgsperiode.dekning = Trygdedekninger.FULL_DEKNING_EOSFO
        lovvalgsperiode.lovvalgsland = Landkoder.BE
        lovvalgsperiode.tilleggsbestemmelse = Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1
        return lovvalgsperiode
    }

    private fun lagMedlemskapsPeriode(): Medlemskapsperiode {
        val medlemskapsperiode = Medlemskapsperiode()
        medlemskapsperiode.medlemskapstype = Medlemskapstyper.FRIVILLIG
        medlemskapsperiode.arbeidsland = Landkoder.BE.kode
        medlemskapsperiode.bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
        medlemskapsperiode.setTrygdedekning(Trygdedekninger.HELSEDEL)
        medlemskapsperiode.fom = LocalDate.now()
        medlemskapsperiode.tom = LocalDate.now().plusYears(1)
        return medlemskapsperiode
    }

    @Throws(IOException::class)
    private fun <T> hentJsonResponse(filnavn: String, clazz: Class<T>): T {
        return objectMapper.readValue(
            javaClass.classLoader.getResource("mock/medlemskap/$filnavn.json"),
            clazz
        )
    }
}
