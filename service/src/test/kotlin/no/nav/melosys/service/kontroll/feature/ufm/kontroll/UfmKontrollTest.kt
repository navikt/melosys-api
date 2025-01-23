package no.nav.melosys.service.kontroll.feature.ufm.kontroll

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
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.List


class UfmKontrollTest {
    private val DATE: LocalDate = LocalDate.EPOCH

    @Test
    fun feilIPeriode_erFeil_verifiserBegrunnelse() {
        Assertions.assertThat(UfmKontroll.feilIPeriode(kontrollData(null, null))).isEqualTo(Kontroll_begrunnelser.FEIL_I_PERIODEN)
    }

    @Test
    fun periodeErÅpen_erFeil_verifiserBegrunnelse() {
        Assertions.assertThat(UfmKontroll.periodeErÅpen(kontrollData(DATE, null))).isEqualTo(Kontroll_begrunnelser.INGEN_SLUTTDATO)
    }

    @Test
    fun periodeOver24MndOgEnDag_erFeil_verifiserBegrunnelse() {
        Assertions.assertThat(UfmKontroll.periodeOver24MånederOgEnDag(kontrollData())).isEqualTo(Kontroll_begrunnelser.PERIODEN_OVER_24_MD)
    }

    @Test
    fun periodeOver24MndOgEnDag_medNøyaktig2ÅrOg1Dag_erRett() {
        val kontrollData = kontrollData(DATE.plusYears(2), DATE.plusYears(4))


        Assertions.assertThat(UfmKontroll.periodeOver24MånederOgEnDag(kontrollData)).isNull()
    }

    @Test
    fun periodeOver24MndOgEnDag_medOver1DagOverlapp_erFeil_verifiserBegrunnelse() {
        val kontrollData = kontrollData(DATE.plusYears(2).minusDays(1), DATE.plusYears(4))


        Assertions.assertThat(UfmKontroll.periodeOver24MånederOgEnDag(kontrollData)).isEqualTo(Kontroll_begrunnelser.PERIODEN_OVER_24_MD)
    }


    @Test
    fun periodeOver5År_erFeil_verifiserBegrunnelse() {
        Assertions.assertThat(UfmKontroll.periodeOver5År(kontrollData())).isEqualTo(Kontroll_begrunnelser.PERIODEN_OVER_5_AR)
    }

    @Test
    fun periodeEldreEnn5År_erFeil_verifiserBegrunnelse() {
        Assertions.assertThat(UfmKontroll.periodeStarterFørFørsteJuni2012(kontrollData(DATE.minusYears(11), null)))
            .isEqualTo(Kontroll_begrunnelser.PERIODE_FOR_GAMMEL)
    }

    @Test
    fun periodeOver1ÅrFremITid_erFeil_verifiserBegrunnelse() {
        Assertions.assertThat(UfmKontroll.periodeOver1ÅrFremITid(kontrollData(LocalDate.now())))
            .isEqualTo(Kontroll_begrunnelser.PERIODE_LANGT_FREM_I_TID)
    }

    @Test
    fun utbetaltYtelserFraOffentligIPeriode_erFeil_verifiserBegrunnelse() {
        Assertions.assertThat(UfmKontroll.utbetaltYtelserFraOffentligIPeriode(kontrollData(LocalDate.now())))
            .isEqualTo(Kontroll_begrunnelser.MOTTAR_YTELSER)
    }

    @Test
    fun lovvalgslandErNorge_erFeil_verifiserBegrunnelse() {
        Assertions.assertThat(UfmKontroll.lovvalgslandErNorge(kontrollData())).isEqualTo(Kontroll_begrunnelser.LOVVALGSLAND_NORGE)
    }

    @Test
    fun overlappendeMedlemsperiode_erFeil_verifiserBegrunnelse() {
        Assertions.assertThat(UfmKontroll.overlappendeMedlemsperiode(kontrollData())).isEqualTo(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER)
    }

    @Test
    fun statsborgerskapIkkeMedlemsland_erFeil_verifiserBegrunnelse() {
        Assertions.assertThat(UfmKontroll.statsborgerskapIkkeMedlemsland(kontrollData()))
            .isEqualTo(Kontroll_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND)
    }

    @Test
    fun statsborgerskapStatsløs_erOK_ikkeSjekkMedlemsland() {
        val kontrollData = kontrollData()
        kontrollData.sedDokument.statsborgerskapKoder = listOf("XS")
        Assertions.assertThat(UfmKontroll.statsborgerskapIkkeMedlemsland(kontrollData)).isNull()
    }

    @Test
    fun avtalelandErSverige_erOK_ikkeSjekkMedlemsland() {
        val kontrollData = kontrollData()
        kontrollData.sedDokument.avsenderLandkode = Landkoder.SE
        Assertions.assertThat(UfmKontroll.statsborgerskapIkkeMedlemsland(kontrollData)).isNull()
    }

    @Test
    fun personDød_erFeil_verifiserBegrunnelse() {
        Assertions.assertThat(UfmKontroll.personDød(kontrollData())).isEqualTo(Kontroll_begrunnelser.PERSON_DOD)
    }

    @Test
    fun personBosattINorge_erFeil_verifiserBegrunnelse() {
        Assertions.assertThat(UfmKontroll.personBosattINorge(kontrollData())).isEqualTo(Kontroll_begrunnelser.BOSATT_I_NORGE)
    }

    @Test
    fun arbeidsland_erSvalbard_verifiserBegrunnelse() {
        Assertions.assertThat(UfmKontroll.arbeidsland(kontrollData())).isEqualTo(Kontroll_begrunnelser.ARBEIDSSTED_UTENFOR_EOS)
    }

    private fun kontrollData(localDate: LocalDate): UfmKontrollData {
        return kontrollData(localDate.plusMonths(15), localDate.plusYears(10))
    }

    private fun kontrollData(fom: LocalDate? = DATE.plusMonths(15), tom: LocalDate? = DATE.plusYears(10)): UfmKontrollData {
        val sedDokument = SedDokument()
        sedDokument.lovvalgsperiode = Periode(fom, tom)
        sedDokument.lovvalgslandKode = Landkoder.NO
        sedDokument.statsborgerskapKoder.add("US")
        val adresse_1 = Adresse()
        adresse_1.by = "By_1"
        adresse_1.land = "XY"
        val adresse_2 = Adresse()
        adresse_2.by = "By_2"
        adresse_2.land = "SJ"
        val arbeidssted_1 = Arbeidssted("sted1", adresse_1)
        val arbeidssted_2 = Arbeidssted("sted2", adresse_2)
        val arbeidssteder = List.of(arbeidssted_1, arbeidssted_2)
        sedDokument.arbeidssteder = arbeidssteder

        val arbeidsland_1 = Arbeidsland("XY", arbeidssteder)
        val arbeidsland_2 = Arbeidsland("SJ", arbeidssteder)
        val arbeidsland = List.of(arbeidsland_1, arbeidsland_2)

        sedDokument.arbeidsland = arbeidsland

        val personDokument = PersonDokument()
        personDokument.dødsdato = DATE
        personDokument.bostedsadresse = Bostedsadresse().apply {
            land = Land("NOR")
            postnr = "1234"
            poststed = "Oslo"
            gateadresse.gatenavn = "Gatenavn"
            gateadresse.husnummer = 1
            gateadresse.husbokstav = "A"
        }

        val personhistorikkDokument = PersonhistorikkDokument()
        personhistorikkDokument.bostedsadressePeriodeListe = listOf()

        val personhistorikkDokumenter: MutableList<PersonhistorikkDokument?> = ArrayList(listOf<PersonhistorikkDokument>())
        personhistorikkDokumenter.add(personhistorikkDokument)

        val medlemskapDokument = MedlemskapDokument()
        val medlemsperiode = Medlemsperiode(periode = Periode(DATE, DATE.plusYears(2)))
        medlemsperiode.status = PeriodestatusMedl.UAVK.kode
        medlemskapDokument.getMedlemsperiode().add(medlemsperiode)

        val inntektDokument = InntektDokument()
        val inntekt = createInntektForTest(InntektType.YtelseFraOffentlige, YearMonth.now().plusYears(2))

        val arbeidsInntektMaaned = ArbeidsInntektMaaned(
            null, null,
            ArbeidsInntektInformasjon(List.of(inntekt), emptyList())
        )

        val arbeidsInntektMaanedListe: MutableList<ArbeidsInntektMaaned> = ArrayList()
        arbeidsInntektMaanedListe.add(arbeidsInntektMaaned)
        inntektDokument.arbeidsInntektMaanedListe = arbeidsInntektMaanedListe

        val utbetalingDokument = UtbetalingDokument()
        utbetalingDokument.utbetalinger = listOf(Utbetaling())

        return UfmKontrollData(
            sedDokument, personDokument, medlemskapDokument, inntektDokument,
            utbetalingDokument, null, personhistorikkDokumenter, null
        )
    }
}
