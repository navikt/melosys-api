package no.nav.melosys.integrasjon.medl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.kotest.assertions.throwables.shouldThrow
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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.util.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equality.FieldsEqualityCheckConfig
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode
import no.nav.melosys.domain.dokument.medlemskap.Periode

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MedlServiceTest {
    private val FNR = "77777777773"
    private var mockRestConsumer = mockk<MedlemskapRestConsumer>()
    private val objectMapper = ObjectMapper().apply { registerModule(JavaTimeModule()) }
    private val medlService: MedlService = MedlService(mockRestConsumer, objectMapper)

    @Test
    fun skalHentPeriodeliste() {
        every {
            mockRestConsumer.hentPeriodeListe(
                FNR, LocalDate.now(), LocalDate.now()
            )
        } returns hentMedlemskapsunntakListe()

        val saksopplysning = medlService.hentPeriodeListe(FNR, LocalDate.now(), LocalDate.now())

        saksopplysning
            .shouldNotBeNull()
            .kilder
            .shouldHaveSize(1)
            .first()
            .mottattDokument.isNullOrEmpty()

        saksopplysning.dokument
            .shouldBeInstanceOf<MedlemskapDokument>()
            .medlemsperiode.shouldHaveSize(1)
            .first()
            .shouldBeEqualToComparingFields(
                Medlemsperiode().apply {
                    id = 123456L
                    type = "PUMEDSKP"
                    status = PeriodestatusMedl.GYLD.kode
                    grunnlagstype = "IMEDEOS"
                    land = "NOR"
                    lovvalg = LovvalgMedl.ENDL.kode
                    trygdedekning = "Unntatt"
                    kildedokumenttype = "Dokument"
                    kilde = "INFOTR"
                    periode = Periode(LocalDate.of(2021, 9, 1), LocalDate.of(2021, 10, 1))
                }, FieldsEqualityCheckConfig(ignorePrivateFields = false)
            )
    }

    @Test
    fun skalOpprettPeriodeEndelig() {
        val medlemskapsunntakForPostCapturingSlot = slot<MedlemskapsunntakForPost>()
        val lovvalgsperiode = lagLovvalgsPeriode()
        every {
            mockRestConsumer.opprettPeriode(capture(medlemskapsunntakForPostCapturingSlot))
        }.answers {
            medlemskapsunntakForPostCapturingSlot.captured.shouldBeEqualToComparingFields(
                MedlemskapsunntakForPost(
                    ident = FNR,
                    fraOgMed = lovvalgsperiode.fom,
                    tilOgMed = lovvalgsperiode.tom,
                    status = PeriodestatusMedl.GYLD.kode,
                    dekning = DekningMedl.FULL.kode,
                    lovvalgsland = "BEL",
                    lovvalg = LovvalgMedl.ENDL.kode,
                    grunnlag = GrunnlagMedl.FO_11_4_1.kode,
                    sporingsinformasjon = MedlemskapsunntakForPost.SporingsinformasjonForPost(
                        kildedokument = KildedokumenttypeMedl.HENV_SOKNAD.getKode()
                    )
                )
            )
            hentMedlemskapsunntak()
        }
        medlService.opprettPeriodeEndelig(FNR, lovvalgsperiode, KildedokumenttypeMedl.HENV_SOKNAD)
            .shouldBe(123456)
    }

    @Test
    fun skalOpprettPeriodeUnderAvklaring() {
        val lovvalgsperiode = lagLovvalgsPeriode()
        val medlemskapsunntakForPostCapturingSlot = slot<MedlemskapsunntakForPost>()
        every {
            mockRestConsumer.opprettPeriode(capture(medlemskapsunntakForPostCapturingSlot))
        } answers {
            medlemskapsunntakForPostCapturingSlot.captured.shouldBeEqualToComparingFields(
                MedlemskapsunntakForPost(
                    ident = FNR,
                    fraOgMed = lovvalgsperiode.fom,
                    tilOgMed = lovvalgsperiode.tom,
                    status = LovvalgMedl.UAVK.kode,
                    dekning = DekningMedl.FULL.kode,
                    lovvalgsland = "BEL",
                    lovvalg = LovvalgMedl.UAVK.kode,
                    grunnlag = GrunnlagMedl.FO_11_4_1.kode,
                    sporingsinformasjon = MedlemskapsunntakForPost.SporingsinformasjonForPost(
                        kildedokument = KildedokumenttypeMedl.HENV_SOKNAD.getKode()
                    )
                )
            )
            hentMedlemskapsunntak()
        }
        medlService.opprettPeriodeUnderAvklaring(FNR, lovvalgsperiode, KildedokumenttypeMedl.HENV_SOKNAD)
            .shouldBe(123456)
    }

    @Test
    fun skalOpprettPeriodeForeløpig() {
        val lovvalgsperiode = lagLovvalgsPeriode()
        val medlemskapsunntakForPostCapturingSlot = slot<MedlemskapsunntakForPost>()
        every {
            mockRestConsumer.opprettPeriode(capture(medlemskapsunntakForPostCapturingSlot))
        } answers {
            medlemskapsunntakForPostCapturingSlot.captured.shouldBeEqualToComparingFields(
                MedlemskapsunntakForPost(
                    ident = FNR,
                    fraOgMed = lovvalgsperiode.fom,
                    tilOgMed = lovvalgsperiode.tom,
                    status = LovvalgMedl.UAVK.kode,
                    dekning = DekningMedl.FULL.kode,
                    lovvalgsland = "BEL",
                    lovvalg = LovvalgMedl.FORL.kode,
                    grunnlag = GrunnlagMedl.FO_11_4_1.kode,
                    sporingsinformasjon = MedlemskapsunntakForPost.SporingsinformasjonForPost(
                        kildedokument = KildedokumenttypeMedl.HENV_SOKNAD.getKode()
                    )
                )
            )
            hentMedlemskapsunntak()
        }
        medlService.opprettPeriodeForeløpig(FNR, lovvalgsperiode, KildedokumenttypeMedl.HENV_SOKNAD)
            .shouldBe(123456)
    }

    @Test
    fun skalOppdaterePeriodeEndelig() {
        every { mockRestConsumer.hentPeriode(any()) } returns hentMedlemskapsunntak()
        val lovvalgsperiode = lagLovvalgsPeriode().apply { medlPeriodeID = 123456L }
        val medlemskapsunntakForPutCapturingSlot = slot<MedlemskapsunntakForPut>()
        every {
            mockRestConsumer.oppdaterPeriode(capture(medlemskapsunntakForPutCapturingSlot))
        }.answers {
            medlemskapsunntakForPutCapturingSlot.captured.shouldBeEqualToComparingFields(
                MedlemskapsunntakForPut(
                    unntakId = 123456,
                    fraOgMed = lovvalgsperiode.fom,
                    tilOgMed = lovvalgsperiode.tom,
                    status = PeriodestatusMedl.GYLD.kode,
                    dekning = DekningMedl.FULL.kode,
                    lovvalgsland = "BEL",
                    lovvalg = LovvalgMedl.ENDL.kode,
                    grunnlag = GrunnlagMedl.FO_11_4_1.kode,
                    sporingsinformasjon = MedlemskapsunntakForPut.SporingsinformasjonForPut(
                        versjon = 1,
                        kildedokument = KildedokumenttypeMedl.HENV_SOKNAD.getKode()
                    )
                )
            )
            hentMedlemskapsunntak()
        }
        medlService.oppdaterPeriodeEndelig(lovvalgsperiode, KildedokumenttypeMedl.HENV_SOKNAD)
    }

    @Test
    fun skalOpprettePeriodeEndeligFtrl() {
        val medlemskapsperiode = lagMedlemskapsPeriode()
        val medlemskapsunntakForPostCapturingSlot = slot<MedlemskapsunntakForPost>()
        every {
            mockRestConsumer.opprettPeriode(capture(medlemskapsunntakForPostCapturingSlot))
        }.answers {
            medlemskapsunntakForPostCapturingSlot.captured.shouldBeEqualToComparingFields(
                MedlemskapsunntakForPost(
                    ident = FNR,
                    fraOgMed = medlemskapsperiode.fom,
                    tilOgMed = medlemskapsperiode.tom,
                    status = PeriodestatusMedl.GYLD.kode,
                    dekning = DekningMedl.FTRL_2_9_1_LEDD_A.kode,
                    lovvalgsland = "BEL",
                    lovvalg = LovvalgMedl.ENDL.kode,
                    grunnlag = GrunnlagMedl.FTL_2_8.kode,
                    sporingsinformasjon = MedlemskapsunntakForPost.SporingsinformasjonForPost(
                        kildedokument = KildedokumenttypeMedl.HENV_SOKNAD.getKode()
                    )
                )
            )
            hentMedlemskapsunntak()
        }
        medlService.opprettPeriodeEndelig(FNR, medlemskapsperiode, KildedokumenttypeMedl.HENV_SOKNAD)
    }

    @Test
    fun skalOppdaterePeriodeForeløpig() {
        every { mockRestConsumer.hentPeriode(any()) } returns hentMedlemskapsunntak()
        val lovvalgsperiode = lagLovvalgsPeriode().apply { medlPeriodeID = 123456L }
        val medlemskapsunntakForPutCapturingSlot = slot<MedlemskapsunntakForPut>()
        every {
            mockRestConsumer.oppdaterPeriode(capture(medlemskapsunntakForPutCapturingSlot))
        }.answers {
            medlemskapsunntakForPutCapturingSlot.captured.shouldBeEqualToComparingFields(
                MedlemskapsunntakForPut(
                    unntakId = 123456,
                    fraOgMed = lovvalgsperiode.fom,
                    tilOgMed = lovvalgsperiode.tom,
                    status = PeriodestatusMedl.UAVK.kode,
                    dekning = DekningMedl.FULL.kode,
                    lovvalgsland = "BEL",
                    lovvalg = LovvalgMedl.FORL.kode,
                    grunnlag = GrunnlagMedl.FO_11_4_1.kode,
                    sporingsinformasjon = MedlemskapsunntakForPut.SporingsinformasjonForPut(
                        versjon = 1,
                        kildedokument = KildedokumenttypeMedl.HENV_SOKNAD.getKode()
                    )
                )
            )
            hentMedlemskapsunntak()
        }
        medlService.oppdaterPeriodeForeløpig(lovvalgsperiode, KildedokumenttypeMedl.HENV_SOKNAD)
    }

    @Test
    fun oppdaterePeriodeFeilerMedManglendePeriodeId() {
        shouldThrow<TekniskException> {
            medlService.oppdaterPeriodeForeløpig(lagLovvalgsPeriode(), KildedokumenttypeMedl.HENV_SOKNAD)
        }.message.shouldContain("Det er ikke lagret noen medlPeriodeID på lovvalgsperiode som skal oppdateres i MEDL")
    }

    @Test
    fun skalAvvisePeriode() {
        every { mockRestConsumer.hentPeriode(any()) } returns hentMedlemskapsunntak()
        val medlemskapsunntakForPutCapturingSlot = slot<MedlemskapsunntakForPut>()
        every {
            mockRestConsumer.oppdaterPeriode(capture(medlemskapsunntakForPutCapturingSlot))
        }.answers {
            medlemskapsunntakForPutCapturingSlot.captured.shouldBeEqualToComparingFields(
                MedlemskapsunntakForPut(
                    unntakId = 123456,
                    fraOgMed = LocalDate.of(2021,9,1),
                    tilOgMed = LocalDate.of(2021,10,1),
                    status = PeriodestatusMedl.AVST.kode,
                    statusaarsak = StatusaarsakMedl.AVVIST.kode,
                    dekning = DekningMedl.UNNTATT.kode,
                    lovvalgsland = "NOR",
                    lovvalg = LovvalgMedl.ENDL.kode,
                    grunnlag = "IMEDEOS",
                    sporingsinformasjon = MedlemskapsunntakForPut.SporingsinformasjonForPut(
                        versjon = 1,
                        kildedokument = "Dokument"
                    )
                )
            )
            hentMedlemskapsunntak()
        }
        medlService.avvisPeriode(123456L, StatusaarsakMedl.AVVIST)
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

    private fun hentMedlemskapsunntakListe(): List<MedlemskapsunntakForGet> {
        return objectMapper.readValue(
            javaClass.classLoader.getResource("mock/medlemskap/gyldigPeriodelisteResponse.json"),
            Array<MedlemskapsunntakForGet>::class.java
        ).toList()
    }

    private fun hentMedlemskapsunntak(): MedlemskapsunntakForGet {
        return objectMapper.readValue(
            javaClass.classLoader.getResource("mock/medlemskap/gyldigPeriodeResponse.json"),
            MedlemskapsunntakForGet::class.java
        )
    }
}
