package no.nav.melosys.service.kontroll.feature.ferdigbehandling.kontroll

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.*
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode
import no.nav.melosys.domain.dokument.medlemskap.Periode
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_us
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.exception.KontrolldataFeilType
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.data.FerdigbehandlingKontrollData
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.data.MedlemskapsperiodeData
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FerdigbehandlingKontrollTest {

    companion object {
        val NOW = LocalDate.now()
        const val BRUKER_AKTØRID = "12345678911"
        const val FULLMEKTIG_ORGNR = "123456789"
    }

    @Test
    internal fun utførKontroll_USA_ART5_4PeriodenErMerEnn12Måneder_kontrollfeil() {
        val lovvalgsperiode = Lovvalgsperiode().apply {
            bestemmelse = Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_4
            fom = NOW
            tom = NOW.plusMonths(12)
        }
        val kontrollData =
            FerdigbehandlingKontrollData(null, null, null, lovvalgsperiode, null, null, null, null, null, null, null)


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
            FerdigbehandlingKontrollData(null, null, null, lovvalgsperiode, null, null, null, null, null, null, null)


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
            FerdigbehandlingKontrollData(null, null, null, lovvalgsperiode, null, null, null, null, null, null, null)


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
            null,
            null
        )


        val kontrollfeil = FerdigbehandlingKontroll.overlappendePeriode(kontrollData)


        kontrollfeil.kode.shouldBe(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER)
    }

    @Test
    fun `medlemskapsperioder uten overlapping skal ikke gi kontrollfeil`() {
        val medlemskapsDokument = MedlemskapDokument().apply {
            medlemsperiode = listOf(
                Medlemsperiode().apply {
                    id = 1
                    land = "SWE"
                    status = "GYLD"
                    periode = Periode(LocalDate.now(), LocalDate.now().plusDays(4))
                }
            )
        }

        val ikkeOverlappendeMedlemskapsperioder = listOf(
            lagMedlemskapsperiode(
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(10)
            )
        )

        val kontrollData = FerdigbehandlingKontrollData(
            medlemskapsDokument,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            MedlemskapsperiodeData(ikkeOverlappendeMedlemskapsperioder, emptyList())
        )


        val kontrollfeil = FerdigbehandlingKontroll.overlappendePeriode(kontrollData)


        kontrollfeil.shouldBeNull()
    }

    @Test
    fun `medlemskapsperioder med overlapping skal gi kontrollfeil`() {
        val medlemskapsDokument = MedlemskapDokument().apply {
            medlemsperiode = listOf(
                Medlemsperiode().apply {
                    id = 1
                    land = "SWE"
                    status = "GYLD"
                    periode = Periode(LocalDate.now(), LocalDate.now().plusDays(4))
                }
            )
        }

        val overlappendeMedlemskapsperioder = listOf(
            lagMedlemskapsperiode(
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(10)
            )
        )

        val kontrollData = FerdigbehandlingKontrollData(
            medlemskapsDokument,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            MedlemskapsperiodeData(overlappendeMedlemskapsperioder, emptyList())
        )


        val kontrollfeil = FerdigbehandlingKontroll.overlappendePeriode(kontrollData)


        kontrollfeil.kode.shouldBe(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER)
        kontrollfeil.type.shouldBe(KontrolldataFeilType.FEIL)
    }

    @Test
    fun `medlemskapsperioder med overlapping, men som hører til samme fagsak skal ikke gi kontrollfeil`() {
        val medlemskapsDokument = MedlemskapDokument().apply {
            medlemsperiode = listOf(
                Medlemsperiode().apply {
                    id = 12345
                    land = "SWE"
                    status = "GYLD"
                    periode = Periode(LocalDate.now(), LocalDate.now().plusDays(4))
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


        val kontrollData = FerdigbehandlingKontrollData(
            medlemskapsDokument,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            MedlemskapsperiodeData(overlappendeMedlemskapsperioder, tidligereMedlemskapsperioder)
        )


        val kontrollfeil = FerdigbehandlingKontroll.overlappendePeriode(kontrollData)

        kontrollfeil.shouldBeNull()
    }

    @Test
    fun `medlemskapsperioder med overlapping, men som ikke hører til samme fagsak skal gi kontrollfeil`() {
        val medlemskapsDokument = MedlemskapDokument().apply {
            medlemsperiode = listOf(
                Medlemsperiode().apply {
                    id = 12345
                    land = "SWE"
                    status = "GYLD"
                    periode = Periode(LocalDate.now(), LocalDate.now().plusDays(4))
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


        val kontrollData = FerdigbehandlingKontrollData(
            medlemskapsDokument,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            MedlemskapsperiodeData(overlappendeMedlemskapsperioder, tidligereMedlemskapsperioder)
        )


        val kontrollfeil = FerdigbehandlingKontroll.overlappendePeriode(kontrollData)

        kontrollfeil.kode.shouldBe(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER)
        kontrollfeil.type.shouldBe(KontrolldataFeilType.FEIL)
    }

    @Test
    fun `medlemskapsperioder med overlapping skal gi kontrollfeil med adversel for visse behandlingstema`() {
        val medlemskapsDokument = MedlemskapDokument().apply {
            medlemsperiode = listOf(
                Medlemsperiode().apply {
                    id = 1
                    land = "SWE"
                    status = "GYLD"
                    periode = Periode(LocalDate.now(), LocalDate.now().plusDays(4))
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

        val kontrollData = FerdigbehandlingKontrollData(
            medlemskapsDokument,
            null,
            null,
            lovvalgsperiode,
            null,
            null,
            Behandlingstema.UTSENDT_ARBEIDSTAKER,
            null,
            null,
            null,
            MedlemskapsperiodeData(overlappendeMedlemskapsperioder, emptyList())
        )


        val kontrollfeil = FerdigbehandlingKontroll.overlappendePeriode(kontrollData)


        kontrollfeil.kode.shouldBe(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER)
        kontrollfeil.type.shouldBe(KontrolldataFeilType.FEIL)
    }

    @Test
    fun `avslag skal gi kontrollfeil med adversel for visse behandlingstema, dersom overlapper med unntaksperiode`() {
        val medlemskapsDokument = MedlemskapDokument().apply {
            medlemsperiode = listOf(
                Medlemsperiode().apply {
                    id = 1
                    land = "SWE"
                    status = "GYLD"
                    periode = Periode(LocalDate.now(), LocalDate.now().plusDays(4))
                }
            )
        }

        val lovvalgsperiode = Lovvalgsperiode().apply {
            id = 1
            fom = LocalDate.now()
            tom = LocalDate.now().plusDays(4)
            innvilgelsesresultat = InnvilgelsesResultat.AVSLAATT
        }

        val kontrollData = FerdigbehandlingKontrollData(
            medlemskapsDokument,
            null,
            null,
            lovvalgsperiode,
            null,
            null,
            Behandlingstema.UTSENDT_ARBEIDSTAKER,
            null,
            null,
            null,
            null
        )

        val kontrollfeil = FerdigbehandlingKontroll.overlappendePeriode(kontrollData)

        kontrollfeil.kode.shouldBe(Kontroll_begrunnelser.OVERLAPPENDE_UNNTAK_PERIODER)
        kontrollfeil.type.shouldBe(KontrolldataFeilType.ADVARSEL)
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
            null,
            null
        )


        val kontrollfeil = FerdigbehandlingKontroll.overlappendeMedlemskapsperiode(kontrollData)


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
            lagAktoerFullmektigPerson(),
            null,
            PersonopplysningerObjectFactory.lagPersonopplysninger(),
            null
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
            lagAktoerFullmektigOrganisasjon(),
            lagOrganisasjonDokument("1111", "Testegate 4", "2222", "Testegate 5"),
            null,
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
            lagAktoerFullmektigOrganisasjon(),
            lagOrganisasjonDokument("", "Testegate 4", "", "NO"),
            null,
            null
        )


        val kontrollfeil = FerdigbehandlingKontroll.adresseRegistrert(kontrollData)


        kontrollfeil.kode.shouldBe(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE_REPRESENTANT)
    }

    private fun lagAktoerFullmektigOrganisasjon(): Aktoer {
        val aktoer = Aktoer()
        aktoer.rolle = Aktoersroller.FULLMEKTIG
        aktoer.orgnr = FULLMEKTIG_ORGNR
        return aktoer
    }


    private fun lagAktoerFullmektigPerson(): Aktoer {
        val aktoer = Aktoer()
        aktoer.rolle = Aktoersroller.FULLMEKTIG
        aktoer.personIdent = BRUKER_AKTØRID
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

    private fun lagMottatteOpplysningerdata(): MottatteOpplysningerData? {
        val mottatteOpplysningerData = MottatteOpplysningerData()
        mottatteOpplysningerData.soeknadsland = Soeknadsland(listOf("AT"), false)
        return mottatteOpplysningerData
    }

    private fun lagMedlemskapsperiode(fraOgMed: LocalDate, tilOgMed: LocalDate): Medlemskapsperiode {
        val medlemskapsperiode = Medlemskapsperiode().apply {
            id = 1L
            fom = fraOgMed
            tom = tilOgMed
            arbeidsland = "BR"
            innvilgelsesresultat = InnvilgelsesResultat.DELVIS_INNVILGET
            medlemskapstype = Medlemskapstyper.FRIVILLIG
            trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON
        }
        return medlemskapsperiode
    }

}
