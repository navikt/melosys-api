package no.nav.melosys.integrasjon.medl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equality.FieldsEqualityCheckConfig
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.melosys.domain.*
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode
import no.nav.melosys.domain.dokument.medlemskap.Periode
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Overgangsregelbestemmelser
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.SedGrunnlag
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.integrasjon.medl.api.v1.MedlemskapsunntakForGet
import no.nav.melosys.integrasjon.medl.api.v1.MedlemskapsunntakForPost
import no.nav.melosys.integrasjon.medl.api.v1.MedlemskapsunntakForPut
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MedlServiceTest {

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
                Medlemsperiode(
                    id = 123456L,
                    type = "PUMEDSKP",
                    status = PeriodestatusMedl.GYLD.kode,
                    grunnlagstype = "IMEDEOS",
                    land = "NOR",
                    lovvalg = LovvalgMedl.ENDL.kode,
                    trygdedekning = "Unntatt",
                    kildedokumenttype = "Dokument",
                    kilde = "INFOTR",
                    periode = Periode(LocalDate.of(2021, 9, 1), LocalDate.of(2021, 10, 1))
                ), FieldsEqualityCheckConfig(ignorePrivateFields = false)
            )
    }

    @Test
    fun skalOpprettPeriodeEndelig() {
        val medlemskapsunntakForPostCapturingSlot = slot<MedlemskapsunntakForPost>()
        val lovvalgsperiode = lagLovvalgsPeriode().apply {
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A
        }
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
                        kildedokument = KildedokumenttypeMedl.HENV_SOKNAD.kode
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
        val lovvalgsperiode = lagLovvalgsPeriode().apply {
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A
        }
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
                        kildedokument = KildedokumenttypeMedl.HENV_SOKNAD.kode
                    )
                )
            )
            hentMedlemskapsunntak()
        }
        medlService.opprettPeriodeUnderAvklaring(FNR, lovvalgsperiode, KildedokumenttypeMedl.HENV_SOKNAD)
            .shouldBe(123456)
    }

    @Test
    fun skalOpprettPeriodeMedOvergangsregelSomGrunnlag() {
        val lovvalgsperiode =
            lagLovvalgsPeriode().apply {
                bestemmelse = Tilleggsbestemmelser_883_2004.FO_883_2004_ART87A
                tilleggsbestemmelse = Tilleggsbestemmelser_883_2004.FO_883_2004_ART87A
                behandlingsresultat = lagBehandlingsresultatMedOvergangsregelbestemmelser()
            }
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
                    grunnlag = GrunnlagMedl.FO_1408_14_2_A.kode,
                    sporingsinformasjon = MedlemskapsunntakForPost.SporingsinformasjonForPost(
                        kildedokument = KildedokumenttypeMedl.HENV_SOKNAD.kode
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
        val lovvalgsperiode = lagLovvalgsPeriode().apply {
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A
        }
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
                        kildedokument = KildedokumenttypeMedl.HENV_SOKNAD.kode
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
        val lovvalgsperiode = lagLovvalgsPeriode().apply {
            medlPeriodeID = 123456L
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A
            tilleggsbestemmelse = Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1
        }
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
                        kildedokument = KildedokumenttypeMedl.HENV_SOKNAD.kode
                    )
                )
            )
            hentMedlemskapsunntak()
        }
        medlService.oppdaterPeriodeEndelig(lovvalgsperiode, KildedokumenttypeMedl.HENV_SOKNAD)
    }

    @Test
    fun skalOpprettPeriodeForeløpigUnntak() {
        val lovvalgsperiode = lagLovvalgsPeriode().apply {
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
            tilleggsbestemmelse = Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5
        }
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
                    grunnlag = GrunnlagMedl.FO_12_1.kode,
                    sporingsinformasjon = MedlemskapsunntakForPost.SporingsinformasjonForPost(
                        kildedokument = KildedokumenttypeMedl.HENV_SOKNAD.kode
                    )
                )
            )
            hentMedlemskapsunntak()
        }
        medlService.opprettPeriodeForeløpig(FNR, lovvalgsperiode, KildedokumenttypeMedl.HENV_SOKNAD)
            .shouldBe(123456)
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
                    lovvalgsland = "NOR",
                    lovvalg = LovvalgMedl.ENDL.kode,
                    grunnlag = GrunnlagMedl.FTL_2_8_1_LEDD_A.kode,
                    sporingsinformasjon = MedlemskapsunntakForPost.SporingsinformasjonForPost(
                        kildedokument = KildedokumenttypeMedl.HENV_SOKNAD.kode
                    )
                )
            )
            hentMedlemskapsunntak()
        }
        medlService.opprettPeriodeEndelig(FNR, medlemskapsperiode, KildedokumenttypeMedl.HENV_SOKNAD)
    }

    @Test
    fun skalOppretteOpphørtPeriodeEndelig() {
        val medlemskapsperiode = lagMedlemskapsPeriode().apply { bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_15_ANDRE_LEDD }

        val medlemskapsunntakForPostCapturingSlot = slot<MedlemskapsunntakForPost>()
        every {
            mockRestConsumer.opprettPeriode(capture(medlemskapsunntakForPostCapturingSlot))
        }.answers {
            medlemskapsunntakForPostCapturingSlot.captured.shouldBeEqualToComparingFields(
                MedlemskapsunntakForPost(
                    ident = FNR,
                    fraOgMed = medlemskapsperiode.fom,
                    tilOgMed = medlemskapsperiode.tom,
                    status = PeriodestatusMedl.AVST.kode,
                    statusaarsak = StatusaarsakMedl.OPPHORT.kode,
                    dekning = DekningMedl.FTRL_2_9_1_LEDD_A.kode,
                    lovvalgsland = "NOR",
                    lovvalg = LovvalgMedl.ENDL.kode,
                    grunnlag = GrunnlagMedl.FTL_2_15_2_LEDD.kode,
                    sporingsinformasjon = MedlemskapsunntakForPost.SporingsinformasjonForPost(
                        kildedokument = KildedokumenttypeMedl.HENV_SOKNAD.kode
                    )
                )
            )
            hentMedlemskapsunntak()
        }
        medlService.opprettOpphørtPeriode(FNR, medlemskapsperiode, KildedokumenttypeMedl.HENV_SOKNAD)
    }

    @Test
    fun skalOppdatereOpphørtPeriode() {
        every { mockRestConsumer.hentPeriode(any()) } returns hentMedlemskapsunntak()
        val medlemskapsperiode = lagMedlemskapsPeriode().apply {
            medlPeriodeID = 123456L
            bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_15_ANDRE_LEDD
        }
        val medlemskapsunntakForPutCapturingSlot = slot<MedlemskapsunntakForPut>()
        every {
            mockRestConsumer.oppdaterPeriode(capture(medlemskapsunntakForPutCapturingSlot))
        }.answers {
            medlemskapsunntakForPutCapturingSlot.captured.shouldBeEqualToComparingFields(
                MedlemskapsunntakForPut(
                    unntakId = 123456,
                    fraOgMed = medlemskapsperiode.fom,
                    tilOgMed = medlemskapsperiode.tom,
                    status = PeriodestatusMedl.AVST.kode,
                    statusaarsak = StatusaarsakMedl.OPPHORT.kode,
                    dekning = DekningMedl.FTRL_2_9_1_LEDD_A.kode,
                    lovvalgsland = "NOR",
                    lovvalg = LovvalgMedl.ENDL.kode,
                    grunnlag = GrunnlagMedl.FTL_2_15_2_LEDD.kode,
                    sporingsinformasjon = MedlemskapsunntakForPut.SporingsinformasjonForPut(
                        versjon = 1,
                        kildedokument = KildedokumenttypeMedl.HENV_SOKNAD.kode
                    )
                )
            )
            hentMedlemskapsunntak()
        }
        medlService.oppdaterOpphørtPeriode(medlemskapsperiode, KildedokumenttypeMedl.HENV_SOKNAD)
    }

    @Test
    fun skalOppdaterePeriodeForeløpig() {
        every { mockRestConsumer.hentPeriode(any()) } returns hentMedlemskapsunntak()
        val lovvalgsperiode = lagLovvalgsPeriode().apply {
            medlPeriodeID = 123456L
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A
        }
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
                        kildedokument = KildedokumenttypeMedl.HENV_SOKNAD.kode
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
                    fraOgMed = LocalDate.of(2021, 9, 1),
                    tilOgMed = LocalDate.of(2021, 10, 1),
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

    private fun lagLovvalgsPeriode() = Lovvalgsperiode().apply {
        fom = LocalDate.now()
        tom = LocalDate.now().plusYears(1)
        dekning = Trygdedekninger.FULL_DEKNING_EOSFO
        lovvalgsland = Land_iso2.BE
        tilleggsbestemmelse = Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1
    }

    private fun lagMedlemskapsPeriode() = Medlemskapsperiode().apply {
        medlemskapstype = Medlemskapstyper.FRIVILLIG
        trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE
        fom = LocalDate.now()
        tom = LocalDate.now().plusYears(1)
        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A
    }

    private fun lagBehandlingsresultatMedOvergangsregelbestemmelser(): Behandlingsresultat {
        val behandlingsresultat = Behandlingsresultat()
        FagsakTestFactory.lagFagsak()

        behandlingsresultat.apply {
            id = 1L
            type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            behandling = Behandling.buildWithDefaults {
                id = 1233
                tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
                type = Behandlingstyper.FØRSTEGANG
                status = Behandlingsstatus.VURDER_DOKUMENT
                mottatteOpplysninger = MottatteOpplysninger().apply {
                    mottatteOpplysningerData = SedGrunnlag().apply {
                        overgangsregelbestemmelser = listOf(Overgangsregelbestemmelser.FO_1408_1971_ART14_2_A)
                    }
                }
            }
        }


        return behandlingsresultat
    }


    private fun hentMedlemskapsunntakListe() = objectMapper.readValue(
        javaClass.classLoader.getResource("mock/medlemskap/gyldigPeriodelisteResponse.json"),
        Array<MedlemskapsunntakForGet>::class.java
    ).toList()

    private fun hentMedlemskapsunntak() = objectMapper.readValue(
        javaClass.classLoader.getResource("mock/medlemskap/gyldigPeriodeResponse.json"),
        MedlemskapsunntakForGet::class.java
    )

    companion object {
        private const val FNR = "77777777773"
    }
}
