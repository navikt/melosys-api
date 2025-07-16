package no.nav.melosys.service.kontroll.feature.ufm.kontroll

import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.dokument.felles.Land
import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektInformasjon
import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektMaaned
import no.nav.melosys.domain.dokument.inntekt.InntektDokument
import no.nav.melosys.domain.dokument.inntekt.InntektType
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode
import no.nav.melosys.domain.dokument.medlemskap.Periode
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.dokument.person.PersonhistorikkDokument
import no.nav.melosys.domain.dokument.person.adresse.Bostedsadresse
import no.nav.melosys.domain.dokument.person.adresse.Gateadresse
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.dokument.utbetaling.Utbetaling
import no.nav.melosys.domain.dokument.utbetaling.UtbetalingDokument
import no.nav.melosys.domain.eessi.melding.Adresse
import no.nav.melosys.domain.eessi.melding.Arbeidsland
import no.nav.melosys.domain.eessi.melding.Arbeidssted
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser
import no.nav.melosys.integrasjon.medl.PeriodestatusMedl
import no.nav.melosys.service.kontroll.feature.ufm.data.UfmKontrollData
import no.nav.melosys.service.kontroll.feature.ufm.kontroll.InntektTestFactory.createInntektForTest
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class UfmKontrollTest {

    @Test
    fun `feil i periode gir korrekt begrunnelse`() {
        UfmKontroll.feilIPeriode(kontrollData(null, null)) shouldBe Kontroll_begrunnelser.FEIL_I_PERIODEN
    }

    @Test
    fun `periode er åpen gir korrekt begrunnelse`() {
        UfmKontroll.periodeErÅpen(kontrollData(DATE, null)) shouldBe Kontroll_begrunnelser.INGEN_SLUTTDATO
    }

    @Test
    fun `periode over 24 måneder og 1 dag gir korrekt begrunnelse`() {
        UfmKontroll.periodeOver24MånederOgEnDag(kontrollData()) shouldBe Kontroll_begrunnelser.PERIODEN_OVER_24_MD
    }

    @Test
    fun `periode med nøyaktig 2 år og 1 dag er OK`() {
        val kontrollData = kontrollData(DATE.plusYears(2), DATE.plusYears(4))
        UfmKontroll.periodeOver24MånederOgEnDag(kontrollData) shouldBe null
    }

    @Test
    fun `periode med over 1 dag overlapp gir feil`() {
        val kontrollData = kontrollData(DATE.plusYears(2).minusDays(1), DATE.plusYears(4))
        UfmKontroll.periodeOver24MånederOgEnDag(kontrollData) shouldBe Kontroll_begrunnelser.PERIODEN_OVER_24_MD
    }

    @Test
    fun `periode over 5 år gir korrekt begrunnelse`() {
        UfmKontroll.periodeOver5År(kontrollData()) shouldBe Kontroll_begrunnelser.PERIODEN_OVER_5_AR
    }

    @Test
    fun `periode eldre enn 5 år gir korrekt begrunnelse`() {
        UfmKontroll.periodeStarterFørFørsteJuni2012(kontrollData(DATE.minusYears(11), null)) shouldBe Kontroll_begrunnelser.PERIODE_FOR_GAMMEL
    }

    @Test
    fun `periode over 1 år frem i tid gir korrekt begrunnelse`() {
        UfmKontroll.periodeOver1ÅrFremITid(kontrollData(LocalDate.now())) shouldBe Kontroll_begrunnelser.PERIODE_LANGT_FREM_I_TID
    }

    @Test
    fun `ytelser fra offentlig i periode gir korrekt begrunnelse`() {
        UfmKontroll.utbetaltYtelserFraOffentligIPeriode(kontrollData(LocalDate.now())) shouldBe Kontroll_begrunnelser.MOTTAR_YTELSER
    }

    @Test
    fun `lovvalgsland Norge gir korrekt begrunnelse`() {
        UfmKontroll.lovvalgslandErNorge(kontrollData()) shouldBe Kontroll_begrunnelser.LOVVALGSLAND_NORGE
    }

    @Test
    fun `overlappende medlemsperiode gir korrekt begrunnelse`() {
        UfmKontroll.overlappendeMedlemsperiode(kontrollData()) shouldBe Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER
    }

    @Test
    fun `statsborgerskap ikke medlemsland gir korrekt begrunnelse`() {
        UfmKontroll.statsborgerskapIkkeMedlemsland(kontrollData()) shouldBe Kontroll_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND
    }

    @Test
    fun `statsløs statsborgerskap returnerer null`() {
        kontrollData().apply {
            sedDokument.statsborgerskapKoder = listOf("XS")
        }.run {
            UfmKontroll.statsborgerskapIkkeMedlemsland(this) shouldBe null
        }
    }

    @Test
    fun `avsenderland Sverige returnerer null`() {
        kontrollData().apply {
            sedDokument.avsenderLandkode = Landkoder.SE
        }.run {
            UfmKontroll.statsborgerskapIkkeMedlemsland(this) shouldBe null
        }
    }

    @Test
    fun `person død gir korrekt begrunnelse`() {
        UfmKontroll.personDød(kontrollData()) shouldBe Kontroll_begrunnelser.PERSON_DOD
    }

    @Test
    fun `person bosatt i Norge gir korrekt begrunnelse`() {
        UfmKontroll.personBosattINorge(kontrollData()) shouldBe Kontroll_begrunnelser.BOSATT_I_NORGE
    }

    @Test
    fun `arbeidsland er Svalbard gir korrekt begrunnelse`() {
        UfmKontroll.arbeidsland(kontrollData()) shouldBe Kontroll_begrunnelser.ARBEIDSSTED_UTENFOR_EOS
    }

    private fun kontrollData(date: LocalDate): UfmKontrollData =
        kontrollData(date.plusMonths(15), date.plusYears(10))

    private fun kontrollData(
        fom: LocalDate? = DATE.plusMonths(15),
        tom: LocalDate? = DATE.plusYears(10)
    ) =
        UfmKontrollData(
            sedDokument = SedDokument().apply {
                lovvalgsperiode = Periode(fom, tom)
                lovvalgslandKode = Landkoder.NO
                statsborgerskapKoder.add("US")
                val arbeidsstedListe = listOf(
                    Arbeidssted(navn = "sted1", Adresse(by = "By_1", land = "XY")),
                    Arbeidssted(navn = "sted2", Adresse(by = "By_2", land = "SJ"))
                )
                arbeidssteder = arbeidsstedListe
                arbeidsland = listOf(
                    Arbeidsland(land = "XY", arbeidssted = arbeidsstedListe),
                    Arbeidsland(land = "SJ", arbeidssted = arbeidsstedListe)
                )
            },

            persondata = PersonDokument().apply {
                dødsdato = DATE
                bostedsadresse = Bostedsadresse(
                    land = Land("NOR"),
                    postnr = "1234",
                    poststed = "Oslo",
                    gateadresse = Gateadresse(
                        gatenavn = "Gatenavn",
                        husnummer = 1,
                        husbokstav = "A"
                    )
                )
            },

            medlemskapDokument = MedlemskapDokument().apply {
                getMedlemsperiode().add(
                    Medlemsperiode(
                        periode = Periode(DATE, DATE.plusYears(2)),
                        status = PeriodestatusMedl.UAVK.kode
                    )
                )
            },

            inntektDokument = InntektDokument().apply {
                arbeidsInntektMaanedListe = listOf(
                    ArbeidsInntektMaaned(
                        aarMaaned = null,
                        avvikListe = null,
                        arbeidsInntektInformasjon = ArbeidsInntektInformasjon(
                            listOf(createInntektForTest(InntektType.YtelseFraOffentlige, YearMonth.now().plusYears(2))),
                            emptyList()
                        )
                    )
                )
            },

            utbetalingDokument = UtbetalingDokument().apply {
                utbetalinger = listOf(Utbetaling())
            },

            mottatteOpplysningerData = null,

            personhistorikkDokumenter = listOf(
                PersonhistorikkDokument(bostedsadressePeriodeListe = listOf())
            ),

            persondataMedHistorikk = null
        )

    companion object {
        private val DATE = LocalDate.EPOCH
    }
}
