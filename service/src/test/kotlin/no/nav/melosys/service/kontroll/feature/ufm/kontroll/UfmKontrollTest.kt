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
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import java.time.YearMonth

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UfmKontrollTest {

    @ParameterizedTest(name = "{index} - {argumentSetName} - {2}")
    @MethodSource("kontrollCases")
    fun `kontroller returnerer korrekt begrunnelse`(
        data: UfmKontrollData,
        kontroll: UfmKontrollData.() -> Kontroll_begrunnelser?,
        forventet: Kontroll_begrunnelser?
    ) {
        data.kontroll() shouldBe forventet
    }

    fun kontrollCases() = listOf(
        kontrollTestCase {
            name = "feil i periode gir korrekt begrunnelse"
            data = kontrollData(null, null)
            kontroll = UfmKontroll::feilIPeriode
            expected = Kontroll_begrunnelser.FEIL_I_PERIODEN
        },

        kontrollTestCase {
            name = "periode er åpen gir korrekt begrunnelse"
            data = kontrollData(DATE, null)
            kontroll = UfmKontroll::periodeErÅpen
            expected = Kontroll_begrunnelser.INGEN_SLUTTDATO
        },

        kontrollTestCase {
            name = "periode over 24 måneder og 1 dag gir korrekt begrunnelse"
            data = kontrollData()
            kontroll = UfmKontroll::periodeOver24MånederOgEnDag
            expected = Kontroll_begrunnelser.PERIODEN_OVER_24_MD
        },

        kontrollTestCase {
            name = "periode med nøyaktig 2 år og 1 dag er OK"
            data = kontrollData(DATE.plusYears(2), DATE.plusYears(4))
            kontroll = UfmKontroll::periodeOver24MånederOgEnDag
            expected = null
        },

        kontrollTestCase {
            name = "periode med over 24 måneder og 1 dag overlapp gir korrekt begrunnelse"
            data = kontrollData(DATE.plusYears(2).minusDays(1), DATE.plusYears(4))
            kontroll = UfmKontroll::periodeOver24MånederOgEnDag
            expected = Kontroll_begrunnelser.PERIODEN_OVER_24_MD
        },

        kontrollTestCase {
            name = "periode over 5 år gir korrekt begrunnelse"
            data = kontrollData()
            kontroll = UfmKontroll::periodeOver5År
            expected = Kontroll_begrunnelser.PERIODEN_OVER_5_AR
        },

        kontrollTestCase {
            name = "periode eldre enn 5 år gir korrekt begrunnelse"
            data = kontrollData(DATE.minusYears(11), null)
            kontroll = UfmKontroll::periodeStarterFørFørsteJuni2012
            expected = Kontroll_begrunnelser.PERIODE_FOR_GAMMEL
        },

        kontrollTestCase {
            name = "periode over 1 år frem i tid gir korrekt begrunnelse"
            data = kontrollData(LocalDate.now())
            kontroll = UfmKontroll::periodeOver1ÅrFremITid
            expected = Kontroll_begrunnelser.PERIODE_LANGT_FREM_I_TID
        },

        kontrollTestCase {
            name = "ytelser fra offentlig i periode gir korrekt begrunnelse"
            data = kontrollData(LocalDate.now())
            kontroll = UfmKontroll::utbetaltYtelserFraOffentligIPeriode
            expected = Kontroll_begrunnelser.MOTTAR_YTELSER
        },

        kontrollTestCase {
            name = "lovvalgsland Norge gir korrekt begrunnelse"
            data = kontrollData()
            kontroll = UfmKontroll::lovvalgslandErNorge
            expected = Kontroll_begrunnelser.LOVVALGSLAND_NORGE
        },

        kontrollTestCase {
            name = "overlappende medlemsperiode gir korrekt begrunnelse"
            data = kontrollData()
            kontroll = UfmKontroll::overlappendeMedlemsperiode
            expected = Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER
        },

        kontrollTestCase {
            name = "statsborgerskap ikke medlemsland gir korrekt begrunnelse"
            data = kontrollData()
            kontroll = UfmKontroll::statsborgerskapIkkeMedlemsland
            expected = Kontroll_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND
        },

        kontrollTestCase {
            name = "statsløs statsborgerskap er ok"
            data = kontrollData().apply {
                sedDokument.statsborgerskapKoder = listOf("XS")
            }
            kontroll = UfmKontroll::statsborgerskapIkkeMedlemsland
            expected = null
        },

        kontrollTestCase {
            name = "avsenderland Sverige er ok"
            data = kontrollData().apply {
                sedDokument.avsenderLandkode = Landkoder.SE
            }
            kontroll = UfmKontroll::statsborgerskapIkkeMedlemsland
            expected = null
        },

        kontrollTestCase {
            name = "person død gir korrekt begrunnelse"
            data = kontrollData()
            kontroll = UfmKontroll::personDød
            expected = Kontroll_begrunnelser.PERSON_DOD
        },

        kontrollTestCase {
            name = "person bosatt i Norge gir korrekt begrunnelse"
            data = kontrollData()
            kontroll = UfmKontroll::personBosattINorge
            expected = Kontroll_begrunnelser.BOSATT_I_NORGE
        },

        kontrollTestCase {
            name = "arbeidsland er Svalbard gir korrekt begrunnelse"
            data = kontrollData()
            kontroll = UfmKontroll::arbeidsland
            expected = Kontroll_begrunnelser.ARBEIDSSTED_UTENFOR_EOS
        }
    )

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
