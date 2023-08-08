package no.nav.melosys.service.kontroll.feature.ferdigbehandling.kontroll

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.Aktoer
import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode
import no.nav.melosys.domain.dokument.medlemskap.Periode
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_us
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.data.FerdigbehandlingKontrollData
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.List

class FerdigbehandlingKontrollTest {

    companion object {
        val NOW = LocalDate.now()
        const val BRUKER_FNR = "11111111111"
        const val BRUKER_AKTØRID = "12345678911"
        const val REPRESENTANT_ORGNR = "123456789"
    }

    @Test
    internal fun utførKontroll_USA_ART5_4PeriodenErMerEnn12Måneder_kontrollfeil() {
        val lovvalgsperiode = Lovvalgsperiode().apply {
            bestemmelse = Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_4
            fom = NOW
            tom = NOW.plusMonths(12)
        }
        val kontrollData =
            FerdigbehandlingKontrollData(null, null, null, lovvalgsperiode, null, null, null, null, null, null)


        val kontrollfeil = FerdigbehandlingKontroll.periodeOver12Måneder(kontrollData)


        kontrollfeil.kode.shouldBe(Kontroll_begrunnelser.MER_ENN_12_MD)
    }

    @Test
    internal fun utførKontroll_USA_ART5_2PeriodenErMerEnn5År_kontrollfeil() {
        val lovvalgsperiode = Lovvalgsperiode().apply {
            bestemmelse = Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_2
            fom = NOW
            tom = NOW.plusYears(5)
        }
        val kontrollData =
            FerdigbehandlingKontrollData(null, null, null, lovvalgsperiode, null, null, null, null, null, null)


        val kontrollfeil = FerdigbehandlingKontroll.periodeOverFemÅr(kontrollData)


        kontrollfeil.kode.shouldBe(Kontroll_begrunnelser.MER_ENN_FEM_ÅR)
    }

    @Test
    internal fun utførKontroll_USA_ART5_6PeriodenErMerEnn5År_ingenKontrollfeil() {
        val lovvalgsperiode = Lovvalgsperiode().apply {
            bestemmelse = Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_6
            fom = NOW
            tom = NOW.plusYears(5)
        }
        val kontrollData =
            FerdigbehandlingKontrollData(null, null, null, lovvalgsperiode, null, null, null, null, null, null)


        val kontrollfeil = FerdigbehandlingKontroll.periodeOverFemÅr(kontrollData)


        kontrollfeil.shouldBeNull()
    }

    @Test
    fun `overlappende periode skal gi kontrollfeil uavhengig om det er medlem eller unntaksperiode`() {
        val medlemskapsDokument =
            MedlemskapDokument().apply {
                medlemsperiode = listOf(
                    Medlemsperiode().apply {
                        id = 1
                        land = "SWE"
                        status = "GYLD"
                        periode = Periode(LocalDate.now(), LocalDate.now().plusDays(4))
                    })
            }

        val lovvalgsperiode = Lovvalgsperiode().apply {
            id = 1
            fom = LocalDate.now()
            tom = LocalDate.now().plusDays(4)
        }
        val kontrollData = FerdigbehandlingKontrollData(
            medlemskapsDokument,
            null,
            null,
            lovvalgsperiode,
            null,
            null,
            null,
            null,
            null,
            null
        )
        val kontrollfeil = FerdigbehandlingKontroll.overlappendePeriode(kontrollData)

        kontrollfeil.kode.shouldBe(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER)
    }

    @Test
    fun `overlappende medlemskapsperiode skal gi kontrollfeil`() {
        val medlemskapsDokument =
            MedlemskapDokument().apply {
                medlemsperiode = listOf(
                    Medlemsperiode().apply {
                        id = 1
                        land = "NOR"
                        status = "GYLD"
                        periode = Periode(LocalDate.now(), LocalDate.now().plusDays(4))
                    })
            }

        val lovvalgsperiode = Lovvalgsperiode().apply {
            id = 1
            fom = LocalDate.now()
            tom = LocalDate.now().plusDays(4)
        }
        val kontrollData = FerdigbehandlingKontrollData(
            medlemskapsDokument,
            null,
            null,
            lovvalgsperiode,
            null,
            null,
            null,
            null,
            null,
            null
        )
        val kontrollfeil = FerdigbehandlingKontroll.overlappendeMedlemsperiode(kontrollData)

        kontrollfeil.kode.shouldBe(Kontroll_begrunnelser.OVERLAPPENDE_MEDLEMSKAPSPERIODER)
    }

    @Test
    fun `overlappende unntaksperiodeperiode skal gi kontrollfeil`() {
        val medlemskapsDokument =
            MedlemskapDokument().apply {
                medlemsperiode = listOf(
                    Medlemsperiode().apply {
                        id = 1
                        land = "SWE"
                        status = "GYLD"
                        periode = Periode(LocalDate.now(), LocalDate.now().plusDays(4))
                    })
            }

        val lovvalgsperiode = Lovvalgsperiode().apply {
            id = 1
            fom = LocalDate.now()
            tom = LocalDate.now().plusDays(4)
        }
        val kontrollData = FerdigbehandlingKontrollData(
            medlemskapsDokument,
            null,
            null,
            lovvalgsperiode,
            null,
            null,
            null,
            null,
            null,
            null
        )
        val kontrollfeil = FerdigbehandlingKontroll.overlappendeUnntaksperiode(kontrollData)

        kontrollfeil.kode.shouldBe(Kontroll_begrunnelser.OVERLAPPENDE_UNNTAK_PERIODER)
    }

    @Test
    fun `person uten registrert adresse skal gi kontrollfeil`() {
        val kontrollData = FerdigbehandlingKontrollData(
            null,
            PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser(),
            lagMottatteOpplysningerdata(),
            null,
            null,
            null,
            null,
            null,
            null,
            null
        )
        val kontrollfeil = FerdigbehandlingKontroll.adresseRegistrert(kontrollData)

        kontrollfeil.kode.shouldBe(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE_BRUKER)
    }

    @Test
    fun `person med registrert adresse skal ikke gi kontrollfeil`() {
        val kontrollData = FerdigbehandlingKontrollData(
            null,
            PersonopplysningerObjectFactory.lagPersonopplysninger(),
            lagMottatteOpplysningerdata(),
            null,
            null,
            null,
            null,
            null,
            null,
            null
        )
        val kontrollfeil = FerdigbehandlingKontroll.adresseRegistrert(kontrollData)

        kontrollfeil.shouldBeNull()
    }

    @Test
    fun `person uten registrert adresse med fullmakt med gyldig adresse skal ikke gi kontrollfeil`() {
        val kontrollData = FerdigbehandlingKontrollData(
            null,
            PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser(),
            lagMottatteOpplysningerdata(),
            null,
            null,
            null,
            null,
            lagAktoerRepresentantPerson(),
            null,
            PersonopplysningerObjectFactory.lagPersonopplysninger()
        )
        val kontrollfeil = FerdigbehandlingKontroll.adresseRegistrert(kontrollData)

        kontrollfeil.shouldBeNull()
    }

    @Test
    fun `person uten registrert adresse med fullmektig organisasjon med adresse skal ikke gi kontrollfeil`() {
        val kontrollData = FerdigbehandlingKontrollData(
            null,
            PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser(),
            lagMottatteOpplysningerdata(),
            null,
            null,
            null,
            null,
            lagAktoerRepresentantOrganisasjon(),
            lagOrganisasjonDokument("1111", "Testegate 4", "2222", "Testegate 5"),
            null
        )
        val kontrollfeil = FerdigbehandlingKontroll.adresseRegistrert(kontrollData)

        kontrollfeil.shouldBeNull()
    }

    @Test
    fun `person uten registrert adresse med fullmektig organisasjon uten adresse skal gi kontrollfeil`() {
        val kontrollData = FerdigbehandlingKontrollData(
            null,
            PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser(),
            lagMottatteOpplysningerdata(),
            null,
            null,
            null,
            null,
            lagAktoerRepresentantOrganisasjon(),
            lagOrganisasjonDokument("", "Testegate 4", "", "Testegate 5"),
            null
        )
        val kontrollfeil = FerdigbehandlingKontroll.adresseRegistrert(kontrollData)

        kontrollfeil.kode.shouldBe(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE_REPRESENTANT)
    }

    private fun lagAktoerRepresentantOrganisasjon(): Aktoer {
        val aktoer = Aktoer()
        aktoer.rolle = Aktoersroller.REPRESENTANT
        aktoer.orgnr = REPRESENTANT_ORGNR
        return aktoer
    }


    private fun lagAktoerRepresentantPerson(): Aktoer {
        val aktoer = Aktoer()
        aktoer.rolle = Aktoersroller.REPRESENTANT
        aktoer.personIdent = BRUKER_AKTØRID
        return aktoer
    }

    private fun lagOrganisasjonDokument(
        forretningsPostnr: String,
        forretningsGatenavn: String,
        postadressePostnr: String,
        postadresseLand: String
    ): OrganisasjonDokument {
        val organisasjonDokument = OrganisasjonDokument()
        val organisasjonsDetaljer = OrganisasjonsDetaljer()
        organisasjonDokument.setOrganisasjonDetaljer(organisasjonsDetaljer)
        val forretningsadresse = SemistrukturertAdresse()
        organisasjonsDetaljer.forretningsadresse.add(forretningsadresse)
        forretningsadresse.adresselinje1 = forretningsGatenavn
        forretningsadresse.postnr = forretningsPostnr
        forretningsadresse.poststed = "Forretningspoststed"
        forretningsadresse.landkode = "NO"
        forretningsadresse.gyldighetsperiode =
            no.nav.melosys.domain.dokument.felles.Periode(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1))
        val postadresse = SemistrukturertAdresse()
        organisasjonsDetaljer.postadresse.add(postadresse)
        postadresse.adresselinje1 = "Postgatenavn"
        postadresse.postnr = postadressePostnr
        postadresse.poststed = "Postpoststed"
        postadresse.landkode = postadresseLand
        postadresse.gyldighetsperiode =
            no.nav.melosys.domain.dokument.felles.Periode(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1))
        return organisasjonDokument
    }

    private fun lagMottatteOpplysningerdata(): MottatteOpplysningerData? {
        val mottatteOpplysningerData = MottatteOpplysningerData()
        mottatteOpplysningerData.soeknadsland = Soeknadsland(List.of("AT"), false)
        return mottatteOpplysningerData
    }
}
