package no.nav.melosys.service.kontroll.feature.ferdigbehandling.kontroll

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.brev.utkast.UtkastBrev
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode
import no.nav.melosys.domain.dokument.medlemskap.Periode
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.Vertslandsavtale_bestemmelser.DET_INTERNASJONALE_BARENTSSEKRETARIATET_ART14
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_konv_efta_storbritannia
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_us
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.domain.person.Persondata
import no.nav.melosys.exception.KontrolldataFeilType
import no.nav.melosys.integrasjon.trygdeavgift.dto.NOK
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.data.FerdigbehandlingKontrollData
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.data.MedlemskapsperiodeData
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.data.SaksopplysningerData
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.data.TrygdeavgiftsperiodeData
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class FerdigbehandlingKontrollTest {

    private val DATO: LocalDate = LocalDate.parse("2024-01-01")

    @Test
    internal fun utførKontroll_USA_ART5_4PeriodenErMerEnn12Måneder_kontrollfeil() {
        val lovvalgsperiode = Lovvalgsperiode().apply {
            bestemmelse = Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_4
            fom = DATO
            tom = DATO.plusMonths(12)
        }
        val kontrollData = lagFerdigbehandlingKontrollData(lovvalgsperiode = lovvalgsperiode)

        val kontrollfeil = FerdigbehandlingKontroll.periodeOver12Måneder(kontrollData)


        kontrollfeil.shouldNotBeNull().kode.shouldBe(Kontroll_begrunnelser.MER_ENN_12_MD)
    }

    @Test
    internal fun utførKontroll_USA_ART5_2PeriodenErMerEnn5År_kontrollfeil() {
        val lovvalgsperiode = Lovvalgsperiode().apply {
            bestemmelse = Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_2
            fom = DATO
            tom = DATO.plusYears(5)
        }
        val kontrollData = lagFerdigbehandlingKontrollData(lovvalgsperiode = lovvalgsperiode)


        val kontrollfeil = FerdigbehandlingKontroll.periodeOverFemÅr(kontrollData)


        kontrollfeil.shouldNotBeNull().kode.shouldBe(Kontroll_begrunnelser.MER_ENN_FEM_ÅR)
    }

    @Test
    internal fun utførKontroll_USA_ART5_6PeriodenErMerEnn5År_ingenKontrollfeil() {
        val lovvalgsperiode = Lovvalgsperiode().apply {
            bestemmelse = Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_6
            fom = DATO
            tom = DATO.plusYears(5)
        }
        val kontrollData = lagFerdigbehandlingKontrollData(lovvalgsperiode = lovvalgsperiode)


        val kontrollfeil = FerdigbehandlingKontroll.periodeOverFemÅr(kontrollData)


        kontrollfeil.shouldBeNull()
    }

    @Test
    fun `overlappende periode skal gi kontrollfeil uavhengig om det er medlem eller unntaksperiode`() {
        val medlemskapsDokument =
            MedlemskapDokument().apply {
                medlemsperiode = listOf(
                    Medlemsperiode(periode = Periode(LocalDate.now(), LocalDate.now().plusDays(4))).apply {
                        id = 1
                        land = "SWE"
                        status = "GYLD"
                    })
            }

        val lovvalgsperiode = Lovvalgsperiode().apply {
            id = 1
            fom = LocalDate.now()
            tom = LocalDate.now().plusDays(4)
        }
        val kontrollData = lagFerdigbehandlingKontrollData(medlemskapDokument = medlemskapsDokument, lovvalgsperiode = lovvalgsperiode)

        val kontrollfeil = FerdigbehandlingKontroll.overlappendePeriode(kontrollData)

        kontrollfeil.shouldNotBeNull().kode.shouldBe(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER)
    }

    @Test
    fun `overlappende periode med forskuddsvis fakturering skal gi advarsel`() {
        val nyeTrygdeavgiftperioder = listOf(
            lagTrygdeavgiftPeriode(
                LocalDate.now().plusDays(2), LocalDate.now().plusDays(10)
            )
        )

        val tidligereTrygdeavgiftperioder = listOf(
            lagTrygdeavgiftPeriode(
                LocalDate.now(), LocalDate.now().plusDays(10)
            )
        )

        val medlemskapDokument = MedlemskapDokument()

        val kontrollData = lagFerdigbehandlingKontrollData(
            trygdeavgiftperiodeData = TrygdeavgiftsperiodeData(
                nyeTrygdeavgiftperioder,
                tidligereTrygdeavgiftperioder
            ), medlemskapDokument = medlemskapDokument
        )

        val kontrollfeil = FerdigbehandlingKontroll.harOverlappendePeriodeMedForskuddsvisFakturering(kontrollData)

        kontrollfeil.shouldNotBeNull().kode.shouldBe(Kontroll_begrunnelser.OVERLAPPENDE_PERIODE_MED_FORSKUDDSVIS_FAKTURERUNG)
    }

    @Test
    fun `manglende fullmektig, medlem etter vertslandsavtale skal gi advarsel`() {
        val medlemskapDokument = MedlemskapDokument()
        val ikkeOverlappendeMedlemskapsperioder = listOf(
            lagMedlemskapsperiode(
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(10)
            ).apply { bestemmelse = DET_INTERNASJONALE_BARENTSSEKRETARIATET_ART14 }
        )

        val kontrollData = lagFerdigbehandlingKontrollData(
            medlemskapDokument = medlemskapDokument,
            medlemskapsperiodeData = MedlemskapsperiodeData(ikkeOverlappendeMedlemskapsperioder, emptyList()),
            fullmektigSomBetalerTrygdeavgift = null,
            trygdeavgiftMottaker = Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV
        )

        val kontrollfeil = FerdigbehandlingKontroll.sjekkFullmektigForMedlemEtterVertslandsAvtale(kontrollData)

        kontrollfeil.shouldNotBeNull().kode.shouldBe(Kontroll_begrunnelser.MANGLENDE_FULLMEKTIG_MEDLEM_ETTER_VERTSLANDSAVTALE)
    }

    @Test
    fun `ikke manglende fullmektig, medlem etter vertslandsavtale skal ikke gi advarsel`() {
        val medlemskapDokument = MedlemskapDokument()
        val ikkeOverlappendeMedlemskapsperioder = listOf(
            lagMedlemskapsperiode(
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(10)
            ).apply { bestemmelse = DET_INTERNASJONALE_BARENTSSEKRETARIATET_ART14 }
        )

        val kontrollData = lagFerdigbehandlingKontrollData(
            medlemskapDokument = medlemskapDokument,
            medlemskapsperiodeData = MedlemskapsperiodeData(ikkeOverlappendeMedlemskapsperioder, emptyList()),
            fullmektigSomBetalerTrygdeavgift = lagAktoerFullmektigPerson(),
            trygdeavgiftMottaker = Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV
        )

        val kontrollfeil = FerdigbehandlingKontroll.sjekkFullmektigForMedlemEtterVertslandsAvtale(kontrollData)

        kontrollfeil.shouldBeNull()
    }


    @Test
    fun `medlemskapsperioder uten overlapping skal ikke gi kontrollfeil`() {
        val medlemskapsDokument = MedlemskapDokument().apply {
            medlemsperiode = listOf(
                Medlemsperiode(periode = Periode(LocalDate.now(), LocalDate.now().plusDays(4))).apply {
                    id = 1
                    land = "SWE"
                    status = "GYLD"
                }
            )
        }
        val ikkeOverlappendeMedlemskapsperioder = listOf(
            lagMedlemskapsperiode(
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(10)
            )
        )
        val kontrollData = lagFerdigbehandlingKontrollData(
            medlemskapDokument = medlemskapsDokument,
            medlemskapsperiodeData = MedlemskapsperiodeData(ikkeOverlappendeMedlemskapsperioder, emptyList()),
        )


        val kontrollfeil = FerdigbehandlingKontroll.overlappendePeriode(kontrollData)


        kontrollfeil.shouldBeNull()
    }

    @Test
    fun `ingen brevutkast skal gi kontrollfeil`() {
        val kontrollData = lagFerdigbehandlingKontrollData(brevUtkast = emptyList())


        val kontrollfeil = FerdigbehandlingKontroll.åpentUtkastFinnes(kontrollData)


        kontrollfeil.shouldBeNull()
    }

    @Test
    fun `ikke-tom brevutkast liste skal gi kontrollfeil`() {
        val kontrollData = lagFerdigbehandlingKontrollData(brevUtkast = listOf(UtkastBrev()))


        val kontrollfeil = FerdigbehandlingKontroll.åpentUtkastFinnes(kontrollData)


        kontrollfeil.shouldNotBeNull().run {
            kode.shouldBe(Kontroll_begrunnelser.ÅPENT_UTKAST)
            type.shouldBe(KontrolldataFeilType.FEIL)
            felter.shouldBeEmpty()
        }
    }

    @Test
    fun `storbritannia-konv-lovvalgsbestemmelse brukt for periode før 01-01-2024 skal gi kontrollfeil`() {
        val lovvalgsperiode = Lovvalgsperiode().apply {
            bestemmelse = Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART14_1
            fom = LocalDate.parse("2023-12-31")
        }
        val kontrollData = lagFerdigbehandlingKontrollData(lovvalgsperiode = lovvalgsperiode)


        val kontrollfeil = FerdigbehandlingKontroll.storbritanniaKonvensjonBruktForTidlig(kontrollData)


        kontrollfeil.shouldNotBeNull().run {
            kode.shouldBe(Kontroll_begrunnelser.STORBRITANNIA_KONV_BRUKT_FOR_TIDLIG)
            type.shouldBe(KontrolldataFeilType.FEIL)
            felter.shouldBeEmpty()
        }
    }

    @Test
    fun `storbritannia-konv-lovvalgsbestemmelse brukt for periode etter 01-01-2024 skal ikke gi kontrollfeil`() {
        val lovvalgsperiode = Lovvalgsperiode().apply {
            bestemmelse = Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART14_1
            fom = LocalDate.parse("2024-06-01")
        }
        val kontrollData = lagFerdigbehandlingKontrollData(lovvalgsperiode = lovvalgsperiode)


        val kontrollfeil = FerdigbehandlingKontroll.storbritanniaKonvensjonBruktForTidlig(kontrollData)


        kontrollfeil.shouldBeNull()
    }

    @Test
    fun `ikke storbritannia-konv-lovvalgsbestemmelse brukt for periode før 01-01-2024 skal ikke gi kontrollfeil`() {
        val lovvalgsperiode = Lovvalgsperiode().apply {
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
            fom = LocalDate.parse("2023-12-31")
        }
        val kontrollData = lagFerdigbehandlingKontrollData(lovvalgsperiode = lovvalgsperiode)


        val kontrollfeil = FerdigbehandlingKontroll.storbritanniaKonvensjonBruktForTidlig(kontrollData)


        kontrollfeil.shouldBeNull()
    }

    @Test
    fun `medlemskapsperioder med overlapping skal gi kontrollfeil`() {
        val medlemskapsDokument = MedlemskapDokument().apply {
            medlemsperiode = listOf(
                Medlemsperiode(periode = Periode(LocalDate.now(), LocalDate.now().plusDays(4))).apply {
                    id = 1
                    land = "SWE"
                    status = "GYLD"
                }
            )
        }
        val overlappendeMedlemskapsperioder = listOf(
            lagMedlemskapsperiode(
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(10)
            )
        )
        val kontrollData = lagFerdigbehandlingKontrollData(
            medlemskapDokument = medlemskapsDokument,
            medlemskapsperiodeData = MedlemskapsperiodeData(overlappendeMedlemskapsperioder, emptyList()),
        )


        val kontrollfeil = FerdigbehandlingKontroll.overlappendePeriode(kontrollData)


        kontrollfeil.shouldNotBeNull().run {
            kode.shouldBe(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER)
            type.shouldBe(KontrolldataFeilType.FEIL)
        }
    }

    @Test
    fun `medlemskapsperioder med overlapping, men som hører til samme fagsak skal ikke gi kontrollfeil`() {
        val medlemskapsDokument = MedlemskapDokument().apply {
            medlemsperiode = listOf(
                Medlemsperiode(periode = Periode(LocalDate.now(), LocalDate.now().plusDays(4))).apply {
                    id = 12345
                    land = "SWE"
                    status = "GYLD"
                }
            )
        }
        val overlappendeMedlemskapsperioder = listOf(
            lagMedlemskapsperiode(
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(10)
            )
        )
        val tidligereMedlemskapsperioder = listOf(
            lagMedlemskapsperiode(
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(10)
            ).apply { medlPeriodeID = 12345 }
        )
        val kontrollData = lagFerdigbehandlingKontrollData(
            medlemskapDokument = medlemskapsDokument,
            medlemskapsperiodeData = MedlemskapsperiodeData(overlappendeMedlemskapsperioder, tidligereMedlemskapsperioder),
        )


        val kontrollfeil = FerdigbehandlingKontroll.overlappendePeriode(kontrollData)


        kontrollfeil.shouldBeNull()
    }

    @Test
    fun `tidligere trygdeavgiftsperioder som avsluttes dagen før en ny trygdeavgiftsperiode, skal gi kontrollfeil`() {
        val medlemskapsDokument = MedlemskapDokument().apply {
            medlemsperiode = listOf(
                Medlemsperiode(periode = Periode(LocalDate.now(), LocalDate.now().plusDays(4))).apply {
                    id = 12345
                    land = "SWE"
                    status = "GYLD"
                }
            )
        }

        val nyeTrygdeavgiftperioder = listOf(
            lagTrygdeavgiftPeriode(
                LocalDate.now().plusDays(2), LocalDate.now().plusDays(10)
            )
        )

        val tidligereTrygdeavgiftperioder = listOf(
            lagTrygdeavgiftPeriode(
                LocalDate.now(), LocalDate.now().plusDays(1)
            )
        )

        val kontrollData = lagFerdigbehandlingKontrollData(
            medlemskapDokument = medlemskapsDokument,
            medlemskapsperiodeData = MedlemskapsperiodeData(emptyList(), emptyList()),
            trygdeavgiftperiodeData = TrygdeavgiftsperiodeData(nyeTrygdeavgiftperioder, tidligereTrygdeavgiftperioder),
        )

        val kontrollfeil = FerdigbehandlingKontroll.direkteForutgåendePeriode(kontrollData)

        kontrollfeil.shouldNotBeNull().run {
            kode.shouldBe(Kontroll_begrunnelser.DIREKTE_FORUTGÅENDE_PERIODE)
            type.shouldBe(KontrolldataFeilType.ADVARSEL)
        }
    }

    @Test
    fun `tidligere trygdeavgiftsperioder som ikke avsluttes dagen før en ny trygdeavgiftsperiode, skal ikke gi kontrollfeil`() {
        val nyeTrygdeavgiftperioder = listOf(
            lagTrygdeavgiftPeriode(
                LocalDate.now().plusDays(3), LocalDate.now().plusDays(10)
            )
        )

        val tidligereTrygdeavgiftperioder = listOf(
            lagTrygdeavgiftPeriode(
                LocalDate.now(), LocalDate.now().plusDays(1)
            )
        )

        val kontrollData = lagFerdigbehandlingKontrollData(
            medlemskapsperiodeData = MedlemskapsperiodeData(emptyList(), emptyList()),
            trygdeavgiftperiodeData = TrygdeavgiftsperiodeData(nyeTrygdeavgiftperioder, tidligereTrygdeavgiftperioder),
        )

        val kontrollfeil = FerdigbehandlingKontroll.direkteForutgåendePeriode(kontrollData)

        kontrollfeil.shouldBeNull()
    }

    @Test
    fun `medlemskapsperioder med overlapping, men som ikke hører til samme fagsak skal gi kontrollfeil`() {
        val medlemskapsDokument = MedlemskapDokument().apply {
            medlemsperiode = listOf(
                Medlemsperiode(periode = Periode(LocalDate.now(), LocalDate.now().plusDays(4))).apply {
                    id = 12345
                    land = "SWE"
                    status = "GYLD"
                }
            )
        }
        val overlappendeMedlemskapsperioder = listOf(
            lagMedlemskapsperiode(
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(10)
            )
        )
        val tidligereMedlemskapsperioder = listOf(
            lagMedlemskapsperiode(
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(10)
            ).apply { medlPeriodeID = 666 }
        )
        val kontrollData = lagFerdigbehandlingKontrollData(
            medlemskapDokument = medlemskapsDokument,
            medlemskapsperiodeData = MedlemskapsperiodeData(overlappendeMedlemskapsperioder, tidligereMedlemskapsperioder),
        )


        val kontrollfeil = FerdigbehandlingKontroll.overlappendePeriode(kontrollData)

        kontrollfeil.shouldNotBeNull().run {
            kode.shouldBe(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER)
            type.shouldBe(KontrolldataFeilType.FEIL)
        }
    }

    @Test
    fun `medlemskapsperioder med overlapping skal gi kontrollfeil med adversel for visse behandlingstema`() {
        val medlemskapsDokument = MedlemskapDokument().apply {
            medlemsperiode = listOf(
                Medlemsperiode(periode = Periode(LocalDate.now(), LocalDate.now().plusDays(4))).apply {
                    id = 1
                    land = "SWE"
                    status = "GYLD"
                }
            )
        }
        val overlappendeMedlemskapsperioder = listOf(
            lagMedlemskapsperiode(
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(10)
            )
        )
        val lovvalgsperiode = Lovvalgsperiode().apply {
            id = 1
            fom = LocalDate.now()
            tom = LocalDate.now().plusDays(4)
            innvilgelsesresultat = InnvilgelsesResultat.AVSLAATT
        }
        val kontrollData = lagFerdigbehandlingKontrollData(
            medlemskapDokument = medlemskapsDokument,
            lovvalgsperiode = lovvalgsperiode,
            behandlingstema = Behandlingstema.UTSENDT_ARBEIDSTAKER,
            medlemskapsperiodeData = MedlemskapsperiodeData(overlappendeMedlemskapsperioder, emptyList()),
        )


        val kontrollfeil = FerdigbehandlingKontroll.overlappendePeriode(kontrollData)


        kontrollfeil.shouldNotBeNull().run {
            kode.shouldBe(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER)
            type.shouldBe(KontrolldataFeilType.FEIL)
        }
    }

    @Test
    fun `avslag skal gi kontrollfeil med adversel for visse behandlingstema, dersom overlapper med unntaksperiode`() {
        val medlemskapsDokument = MedlemskapDokument().apply {
            medlemsperiode = listOf(
                Medlemsperiode(periode = Periode(LocalDate.now(), LocalDate.now().plusDays(4))).apply {
                    id = 1
                    land = "SWE"
                    status = "GYLD"
                }
            )
        }
        val lovvalgsperiode = Lovvalgsperiode().apply {
            id = 1
            fom = LocalDate.now()
            tom = LocalDate.now().plusDays(4)
            innvilgelsesresultat = InnvilgelsesResultat.AVSLAATT
        }
        val kontrollData = lagFerdigbehandlingKontrollData(
            medlemskapDokument = medlemskapsDokument,
            lovvalgsperiode = lovvalgsperiode,
            behandlingstema = Behandlingstema.UTSENDT_ARBEIDSTAKER,
        )

        val kontrollfeil = FerdigbehandlingKontroll.overlappendePeriode(kontrollData)

        kontrollfeil.shouldNotBeNull().run {
            kode.shouldBe(Kontroll_begrunnelser.OVERLAPPENDE_UNNTAK_PERIODER)
            type.shouldBe(KontrolldataFeilType.ADVARSEL)
        }
    }


    @Test
    fun `overlappende medlemskapsperiode skal gi kontrollfeil`() {
        val medlemskapsDokument =
            MedlemskapDokument().apply {
                medlemsperiode = listOf(
                    Medlemsperiode(periode = Periode(LocalDate.now(), LocalDate.now().plusDays(4))).apply {
                        id = 1
                        land = "NOR"
                        status = "GYLD"
                    })
            }

        val lovvalgsperiode = Lovvalgsperiode().apply {
            id = 1
            fom = LocalDate.now()
            tom = LocalDate.now().plusDays(4)
        }
        val kontrollData = lagFerdigbehandlingKontrollData(
            medlemskapDokument = medlemskapsDokument,
            lovvalgsperiode = lovvalgsperiode,
        )


        val kontrollfeil = FerdigbehandlingKontroll.overlappendeMedlemskapsperiode(kontrollData)


        kontrollfeil.shouldNotBeNull().kode.shouldBe(Kontroll_begrunnelser.OVERLAPPENDE_MEDLEMSKAPSPERIODER)
    }

    @Test
    fun `overlappende unntaksperiodeperiode skal gi kontrollfeil`() {
        val medlemskapsDokument =
            MedlemskapDokument().apply {
                medlemsperiode = listOf(
                    Medlemsperiode(periode = Periode(LocalDate.now(), LocalDate.now().plusDays(4))).apply {
                        id = 1
                        land = "SWE"
                        status = "GYLD"
                    })
            }

        val lovvalgsperiode = Lovvalgsperiode().apply {
            id = 1
            fom = LocalDate.now()
            tom = LocalDate.now().plusDays(4)
        }
        val kontrollData = lagFerdigbehandlingKontrollData(
            medlemskapDokument = medlemskapsDokument,
            lovvalgsperiode = lovvalgsperiode,
        )


        val kontrollfeil = FerdigbehandlingKontroll.overlappendeUnntaksperiode(kontrollData)


        kontrollfeil.shouldNotBeNull().kode.shouldBe(Kontroll_begrunnelser.OVERLAPPENDE_UNNTAK_PERIODER)
    }

    @Test
    fun `person uten registrert adresse skal gi kontrollfeil`() {
        val kontrollData = lagFerdigbehandlingKontrollData(
            persondata = PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser(),
            mottatteOpplysningerData = lagMottatteOpplysningerdata(),
        )


        val kontrollfeil = FerdigbehandlingKontroll.adresseRegistrert(kontrollData)


        kontrollfeil.shouldNotBeNull().kode.shouldBe(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE_BRUKER)
    }

    @Test
    fun `person med registrert adresse skal ikke gi kontrollfeil`() {
        val kontrollData = lagFerdigbehandlingKontrollData(
            persondata = PersonopplysningerObjectFactory.lagPersonopplysninger(),
            mottatteOpplysningerData = lagMottatteOpplysningerdata(),
        )


        val kontrollfeil = FerdigbehandlingKontroll.adresseRegistrert(kontrollData)


        kontrollfeil.shouldBeNull()

    }

    @Test
    fun `person uten registrert adresse med fullmakt med gyldig adresse skal ikke gi kontrollfeil`() {
        val kontrollData = lagFerdigbehandlingKontrollData(
            persondata = PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser(),
            mottatteOpplysningerData = lagMottatteOpplysningerdata(),
            fullmektig = lagAktoerFullmektigPerson(),
            persondataTilFullmektig = PersonopplysningerObjectFactory.lagPersonopplysninger(),
        )


        val kontrollfeil = FerdigbehandlingKontroll.adresseRegistrert(kontrollData)


        kontrollfeil.shouldBeNull()
    }

    @Test
    fun `person uten registrert adresse med fullmektig organisasjon med adresse skal ikke gi kontrollfeil`() {
        val kontrollData = lagFerdigbehandlingKontrollData(
            persondata = PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser(),
            mottatteOpplysningerData = lagMottatteOpplysningerdata(),
            fullmektig = lagAktoerFullmektigOrganisasjon(),
            organisasjonDokument = lagOrganisasjonDokument("1111", "Testegate 4", "2222", "Testegate 5"),
        )


        val kontrollfeil = FerdigbehandlingKontroll.adresseRegistrert(kontrollData)


        kontrollfeil.shouldBeNull()
    }

    @Test
    fun `person uten registrert adresse med fullmektig organisasjon uten adresse skal gi kontrollfeil`() {
        val kontrollData = lagFerdigbehandlingKontrollData(
            persondata = PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser(),
            mottatteOpplysningerData = lagMottatteOpplysningerdata(),
            fullmektig = lagAktoerFullmektigOrganisasjon(),
            organisasjonDokument = lagOrganisasjonDokument("", "Testegate 4", "", "NO"),
        )


        val kontrollfeil = FerdigbehandlingKontroll.adresseRegistrert(kontrollData)


        kontrollfeil.shouldNotBeNull().kode.shouldBe(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE_REPRESENTANT)
    }

    @Test
    fun `innvilget medlemsskapsperiode over 24 måneder med lovvalgsbestemmelse KONV_EFTA_STORBRITANNIA_ART14_1 skal gi kontrollfeil`() {
        val lovvalgsperiode = Lovvalgsperiode().apply {
            id = 1
            fom = LocalDate.now()
            tom = LocalDate.now().plusMonths(25)
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            bestemmelse = Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART14_1
        }
        val kontrollData = lagFerdigbehandlingKontrollData(
            lovvalgsperiode = lovvalgsperiode,
        )

        val kontrollfeil = FerdigbehandlingKontroll.periodeOver24Mnd(kontrollData)

        kontrollfeil.shouldNotBeNull().kode.shouldBe(Kontroll_begrunnelser.PERIODEN_OVER_24_MD)
    }

    @Test
    fun `avslått medlemsskapsperiode over 24 måneder med lovvalgsbestemmelse KONV_EFTA_STORBRITANNIA_ART14_1 skal ikke gi kontrollfeil`() {
        val lovvalgsperiode = Lovvalgsperiode().apply {
            id = 1
            fom = LocalDate.now()
            tom = LocalDate.now().plusMonths(25)
            innvilgelsesresultat = InnvilgelsesResultat.AVSLAATT
            bestemmelse = Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART14_1
        }
        val kontrollData = lagFerdigbehandlingKontrollData(
            lovvalgsperiode = lovvalgsperiode,
        )

        val kontrollfeil = FerdigbehandlingKontroll.periodeOver24Mnd(kontrollData)

        kontrollfeil.shouldBeNull()
    }

    @Test
    fun `innvilget medlemsskapsperiode over 24 måneder med lovvalgsbestemmelse KONV_EFTA_STORBRITANNIA_ART16_3 skal ikke gi kontrollfeil`() {
        val lovvalgsperiode = Lovvalgsperiode().apply {
            id = 1
            fom = LocalDate.now()
            tom = LocalDate.now().plusMonths(25)
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            bestemmelse = Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART16_3
        }
        val kontrollData = lagFerdigbehandlingKontrollData(
            lovvalgsperiode = lovvalgsperiode,
        )

        val kontrollfeil = FerdigbehandlingKontroll.periodeOver24Mnd(kontrollData)

        kontrollfeil.shouldBeNull()
    }

    @Test
    fun `EØS behandling med behandlingstema UTSENDT_SELVSTENDIG og to avklarte virksomheter skal gi kontrollfeil`() {
        val kontrollData = lagFerdigbehandlingKontrollData(
            antallArbeidsgivere = 2,
            behandlingstema = Behandlingstema.UTSENDT_SELVSTENDIG
        )

        val kontrollfeil = FerdigbehandlingKontroll.kunEnAvklartVirksomhet(kontrollData)

        kontrollfeil.shouldNotBeNull().kode.shouldBe(Kontroll_begrunnelser.IKKE_KUN_EN_VIRKSOMHET_BREV)
    }

    @Test
    fun `EØS behandling med behandlingstema UTSENDT_SELVSTENDIG og en avklart virksomhet skal ikke gi kontrollfeil`() {
        val kontrollData = lagFerdigbehandlingKontrollData(
            antallArbeidsgivere = 1,
            behandlingstema = Behandlingstema.UTSENDT_SELVSTENDIG
        )

        val kontrollfeil = FerdigbehandlingKontroll.kunEnAvklartVirksomhet(kontrollData)

        kontrollfeil.shouldBeNull()
    }

    @Test
    fun `EØS behandling med behandlingstema ARBEID_FLERE_LAND og to avklarte virksomheter skal ikke gi kontrollfeil`() {
        val kontrollData = lagFerdigbehandlingKontrollData(
            antallArbeidsgivere = 2,
            behandlingstema = Behandlingstema.ARBEID_FLERE_LAND
        )

        val kontrollfeil = FerdigbehandlingKontroll.kunEnAvklartVirksomhet(kontrollData)

        kontrollfeil.shouldBeNull()
    }


    @Test
    fun `ny vurdering på FTRL med endret trygdeavgiftsperioder tilbake i tid skal gi feil`() {
        val trygdeavgiftsperiode = listOf(
            lagTrygdeavgiftPeriode(
                LocalDate.now().withDayOfMonth(1).minusYears(1), LocalDate.now().withDayOfMonth(10)
            )
        )
        val trygdeavgiftsperiodeTidligere = listOf(
            lagTrygdeavgiftPeriode(
                LocalDate.now().withDayOfMonth(2).minusYears(1), LocalDate.now().withDayOfMonth(20)
            )
        )

        val kontrollData = lagFerdigbehandlingKontrollData(
            behandlingstyper = Behandlingstyper.NY_VURDERING,
            trygdeavgiftperiodeData = TrygdeavgiftsperiodeData(trygdeavgiftsperiode, emptyList()),
            trygdeavgiftsperioderTidligereBehandling = trygdeavgiftsperiodeTidligere,
            harÅrsavregningPåSak = true
        )

        FerdigbehandlingKontroll.behandlingHarEndretTrygdeavgiftITidligereÅr(kontrollData)
            .shouldNotBeNull()
            .kode.shouldBe(Kontroll_begrunnelser.TRYGDEAVGIFT_ENDRET)
    }

    @Test
    fun `ny vurdering på FTRL med endret trygdeavgiftsperioder uten årsavregning skal ikke gi kontrollfeil`() {
        val trygdeavgiftsperiode = listOf(
            lagTrygdeavgiftPeriode(
                LocalDate.now().withDayOfMonth(1).minusYears(1), LocalDate.now().withDayOfMonth(10)
            )
        )
        val trygdeavgiftsperiodeTidligere = listOf(
            lagTrygdeavgiftPeriode(
                LocalDate.now().withDayOfMonth(2).minusYears(1), LocalDate.now().withDayOfMonth(20)
            )
        )

        val kontrollData = lagFerdigbehandlingKontrollData(
            behandlingstyper = Behandlingstyper.NY_VURDERING,
            trygdeavgiftperiodeData = TrygdeavgiftsperiodeData(trygdeavgiftsperiode, emptyList()),
            trygdeavgiftsperioderTidligereBehandling = trygdeavgiftsperiodeTidligere,
        )

        FerdigbehandlingKontroll.behandlingHarEndretTrygdeavgiftITidligereÅr(kontrollData)
            .shouldBeNull()
    }

    @Test
    fun `FTRL skal ikke kontrollere hvis type er ulik NY_VURDERING`() {
        val trygdeavgiftsperiode = listOf(
            lagTrygdeavgiftPeriode(
                LocalDate.now().withDayOfMonth(1).minusYears(1), LocalDate.now().withDayOfMonth(10)
            )
        )
        val trygdeavgiftsperiodeTidligere = listOf(
            lagTrygdeavgiftPeriode(
                LocalDate.now().withDayOfMonth(1).minusYears(1), LocalDate.now().withDayOfMonth(20)
            )
        )

        val kontrollData = lagFerdigbehandlingKontrollData(
            behandlingstyper = Behandlingstyper.SATSENDRING,
            trygdeavgiftperiodeData = TrygdeavgiftsperiodeData(trygdeavgiftsperiode, emptyList()),
            trygdeavgiftsperioderTidligereBehandling = trygdeavgiftsperiodeTidligere,
            harÅrsavregningPåSak = true
        )

        FerdigbehandlingKontroll.behandlingHarEndretTrygdeavgiftITidligereÅr(kontrollData).shouldBeNull()
    }

    private fun lagAktoerFullmektigOrganisasjon(): Aktoer {
        val aktoer = Aktoer()
        aktoer.rolle = Aktoersroller.FULLMEKTIG
        aktoer.orgnr = "123456789"
        return aktoer
    }

    private fun lagAktoerFullmektigPerson(): Aktoer {
        val aktoer = Aktoer()
        aktoer.rolle = Aktoersroller.FULLMEKTIG
        aktoer.personIdent = "12345678911"
        return aktoer
    }

    private fun lagOrganisasjonDokument(
        forretningsPostnr: String,
        forretningsGatenavn: String,
        postadressePostnr: String,
        postadresseLand: String
    ): OrganisasjonDokument {
        val forretningsadresse = SemistrukturertAdresse().apply {
            adresselinje1 = forretningsGatenavn
            postnr = forretningsPostnr
            poststed = "Forretningspoststed"
            landkode = "NO"
            gyldighetsperiode = no.nav.melosys.domain.dokument.felles.Periode(
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(1)
            )
        }
        val postadresse = SemistrukturertAdresse().apply {
            adresselinje1 = "Postgatenavn"
            postnr = postadressePostnr
            poststed = "Postpoststed"
            landkode = postadresseLand
            gyldighetsperiode = no.nav.melosys.domain.dokument.felles.Periode(
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(1)
            )
        }
        val organisasjonsDetaljer = OrganisasjonsDetaljerTestFactory.builder()
            .forretningsadresse(forretningsadresse)
            .postadresse(postadresse)
            .build()
        return OrganisasjonDokumentTestFactory.builder()
            .organisasjonsDetaljer(organisasjonsDetaljer)
            .build()
    }

    private fun lagMottatteOpplysningerdata(): MottatteOpplysningerData {
        val mottatteOpplysningerData = MottatteOpplysningerData()
        mottatteOpplysningerData.soeknadsland = Soeknadsland(listOf("AT"), false)
        return mottatteOpplysningerData
    }

    private fun lagMedlemskapsperiode(fraOgMed: LocalDate, tilOgMed: LocalDate): Medlemskapsperiode {
        val medlemskapsperiode = Medlemskapsperiode().apply {
            id = 1L
            fom = fraOgMed
            tom = tilOgMed
            innvilgelsesresultat = InnvilgelsesResultat.DELVIS_INNVILGET
            medlemskapstype = Medlemskapstyper.FRIVILLIG
            trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON
            behandlingsresultat = Behandlingsresultat().apply {
                behandling = Behandling().apply {
                    fagsak = Fagsak(saksnummer = "test", tema = Sakstemaer.MEDLEMSKAP_LOVVALG, status = Saksstatuser.OPPRETTET, type = Sakstyper.FTRL)
                }
            }
        }
        return medlemskapsperiode
    }

    private fun lagTrygdeavgiftPeriode(fraOgMed: LocalDate, tilOgMed: LocalDate): Trygdeavgiftsperiode {
        return Trygdeavgiftsperiode(
            periodeFra = fraOgMed,
            periodeTil = tilOgMed,
            trygdeavgiftsbeløpMd = Penger(BigDecimal(1000), NOK.kode),
            trygdesats = BigDecimal(5),
            grunnlagInntekstperiode = Inntektsperiode()
        )
    }

    private fun lagFerdigbehandlingKontrollData(
        medlemskapDokument: MedlemskapDokument? = null,
        persondata: Persondata = PersonopplysningerObjectFactory.lagPersonopplysninger(),
        mottatteOpplysningerData: MottatteOpplysningerData? = null,
        lovvalgsperiode: Lovvalgsperiode? = null,
        opprinneligLovvalgsperiode: Lovvalgsperiode? = null,
        saksopplysningerData: SaksopplysningerData? = null,
        behandlingstema: Behandlingstema? = null,
        fullmektig: Aktoer? = null,
        organisasjonDokument: OrganisasjonDokument? = null,
        persondataTilFullmektig: Persondata? = null,
        medlemskapsperiodeData: MedlemskapsperiodeData? = null,
        brevUtkast: List<UtkastBrev> = emptyList(),
        antallArbeidsgivere: Int = 1,
        trygdeavgiftperiodeData: TrygdeavgiftsperiodeData? = null,
        trygdeavgiftMottaker: Trygdeavgiftmottaker? = null,
        fullmektigSomBetalerTrygdeavgift: Aktoer? = null,
        trygdeavgiftsperioderTidligereBehandling: List<Trygdeavgiftsperiode> = emptyList(),
        behandlingstyper: Behandlingstyper? = null,
        harÅrsavregningPåSak: Boolean? = null
    ) = FerdigbehandlingKontrollData(
        medlemskapDokument,
        persondata,
        mottatteOpplysningerData,
        lovvalgsperiode,
        opprinneligLovvalgsperiode,
        saksopplysningerData,
        behandlingstema,
        fullmektig,
        organisasjonDokument,
        persondataTilFullmektig,
        medlemskapsperiodeData,
        brevUtkast,
        antallArbeidsgivere,
        trygdeavgiftperiodeData,
        trygdeavgiftMottaker,
        fullmektigSomBetalerTrygdeavgift,
        trygdeavgiftsperioderTidligereBehandling,
        behandlingstyper,
        harÅrsavregningPåSak
    )
}
