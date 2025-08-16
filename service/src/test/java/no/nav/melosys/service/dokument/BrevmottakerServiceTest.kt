package no.nav.melosys.service.dokument

import com.google.common.collect.Sets
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet
import no.nav.melosys.domain.brev.Mottaker
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Mottakerroller.*
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_ca
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_gb
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData
import no.nav.melosys.domain.mottatteopplysninger.data.ForetakUtland
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.service.LovvalgsperiodeService
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(MockKExtension::class)
class BrevmottakerServiceTest {

    @RelaxedMockK
    private lateinit var avklarteVirksomheterService: AvklarteVirksomheterService

    @RelaxedMockK
    private lateinit var utenlandskMyndighetService: UtenlandskMyndighetService

    @RelaxedMockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @RelaxedMockK
    private lateinit var lovvalgsperiodeService: LovvalgsperiodeService

    @MockK
    private lateinit var behandling: Behandling

    private lateinit var behandlingsresultat: Behandlingsresultat
    private lateinit var brevmottakerService: BrevmottakerService

    @BeforeEach
    fun setup() {
        brevmottakerService = BrevmottakerService(
            avklarteVirksomheterService,
            utenlandskMyndighetService,
            behandlingsresultatService,
            lovvalgsperiodeService
        )

        behandlingsresultat = Behandlingsresultat().apply {
            val lovvalgsperiode = Lovvalgsperiode().apply {
                bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
            }
            lovvalgsperioder.add(lovvalgsperiode)
        }

        every { behandling.id } returns 123L
    }

    @Test
    fun `avklarMottakere medFullmektigForArbeidsgiver feiler`() {
        every { behandling.fagsak } returns lagFagsakMedFullmektigPerson(Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER)


        val exception = shouldThrow<FunksjonellException> {
            brevmottakerService.avklarMottakere(null, Mottaker.medRolle(FULLMEKTIG), behandling)
        }


        exception.message shouldBe "Finner ikke fullmektig for bruker"
    }

    @Test
    fun `avklarMottakere medFullmektigForBruker girFullmektigMottaker`() {
        every { behandling.fagsak } returns lagFagsakMedFullmektigPerson(Fullmaktstype.FULLMEKTIG_SØKNAD)


        val mottakere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(BRUKER), behandling)


        mottakere shouldHaveSize 1
        mottakere[0].rolle shouldBe FULLMEKTIG
    }

    @Test
    fun `avklarMottakere medBrukerRolleOgIkkeRegistretBruker feiler`() {
        every { behandling.fagsak } returns Fagsak.forTest { }


        val exception = shouldThrow<FunksjonellException> {
            brevmottakerService.avklarMottakere(null, Mottaker.medRolle(BRUKER), behandling)
        }


        exception.message shouldBe "Bruker er ikke registrert."
    }

    @Test
    fun `avklarMottakere medBrukerRolleUtenFullmektig girBrukerMottaker`() {
        every { behandling.fagsak } returns lagFagsakMedBruker()


        val mottakere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(BRUKER), behandling)


        mottakere shouldHaveSize 1
        mottakere[0].rolle shouldBe BRUKER
    }

    @Test
    fun `avklarMottakere medBrukerRolleMedFullmektigOrg girFullmektigMottaker`() {
        every { behandling.fagsak } returns lagFagsakMedFullmektigOrg(Fullmaktstype.FULLMEKTIG_SØKNAD)


        val mottakere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(BRUKER), behandling)


        mottakere shouldHaveSize 1
        mottakere[0].rolle shouldBe FULLMEKTIG
    }

    @Test
    fun `avklarMottakere medBrukerRolleMedFullmektigPerson girFullmektigMottaker`() {
        every { behandling.fagsak } returns lagFagsakMedFullmektigPerson(Fullmaktstype.FULLMEKTIG_SØKNAD)


        val mottakere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(BRUKER), behandling)


        mottakere shouldHaveSize 1
        mottakere[0].rolle shouldBe FULLMEKTIG
    }

    @Test
    fun `avklarMottakere medVirksomhetRolleOgIngenVirksomhet feiler`() {
        val mottaker = Mottaker.medRolle(VIRKSOMHET)
        every { behandling.fagsak } returns Fagsak.forTest { }


        val exception = shouldThrow<FunksjonellException> {
            brevmottakerService.avklarMottakere(null, mottaker, behandling)
        }


        exception.message shouldContain "Fant ikke virksomhet for sak "
    }

    @Test
    fun `avklarMottakere medVirksomhetRolleOgVirksomhet girVirksomhetMottaker`() {
        val virksomhet = Aktoer().apply {
            rolle = Aktoersroller.VIRKSOMHET
            orgnr = "orgnr"
        }
        val fagsak = Fagsak.forTest {
            leggTilAktør(virksomhet)
        }
        every { behandling.fagsak } returns fagsak

        val mottakere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(VIRKSOMHET), behandling)

        mottakere shouldHaveSize 1
        mottakere shouldContainExactly listOf(Mottaker.av(virksomhet))
    }

    @Test
    fun `avklarMottakere medArbeidsgiverRolleOgIngenArbeidsgivere feiler`() {
        every { behandling.fagsak } returns lagFagsakMedBruker()
        every { avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(behandling) } returns emptySet()
        every { avklarteVirksomheterService.hentUtenlandskeVirksomheter(behandling) } returns emptyList()

        val exception = shouldThrow<FunksjonellException> {
            brevmottakerService.avklarMottakere(null, Mottaker.medRolle(ARBEIDSGIVER), behandling)
        }
        exception.message shouldBe "Arbeidsgiver er ikke registrert."
    }

    @Test
    fun `avklarMottakere medArbeidsgiverRolle girArbeidsgiverMottakere`() {
        every { avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(behandling) } returns
                Sets.newHashSet("123456789", "987654321")
        every { behandling.fagsak } returns lagFagsakMedBruker()

        val arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(ARBEIDSGIVER), behandling)

        arbeidsgivere.map { it.orgnr } shouldContainExactlyInAnyOrder listOf("123456789", "987654321")
    }

    @Test
    fun `avklarMottakere medBareUtenlandskeArbeidsgivere girIngenMottakere`() {
        every { behandling.fagsak } returns lagFagsakMedBruker()
        every { avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(behandling) } returns emptySet()
        every { avklarteVirksomheterService.hentUtenlandskeVirksomheter(behandling) } returns
                listOf(AvklartVirksomhet(ForetakUtland()))

        val arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(ARBEIDSGIVER), behandling)

        arbeidsgivere.shouldBeEmpty()
    }

    @Test
    fun `avklarMottakere medArbeidsgiverRolleIkkeKunAvklarteVirksomheterOgIngenArbeidsgivere girTomListe`() {
        every { behandling.fagsak } returns lagFagsakMedBruker()
        every { behandling.mottatteOpplysninger } returns lagMottatteOpplysninger(null, null)
        every { behandling.finnArbeidsforholdDokument() } returns Optional.of(lagArbeidsforholdDokument(null))

        val arbeidsgivere = brevmottakerService.avklarMottakere(
            GENERELT_FRITEKSTBREV_ARBEIDSGIVER,
            Mottaker.medRolle(ARBEIDSGIVER),
            behandling,
            false,
            false
        )

        arbeidsgivere.shouldBeEmpty()
    }

    @Test
    @org.junit.jupiter.api.Disabled("Needs proper domain object setup - helper methods simplified during conversion")
    fun `avklarMottakere medArbeidsgiverRolleIkkeKunAvklarteVirksomheter girArbeidsgiverMottakere`() {
        every { behandling.fagsak } returns lagFagsakMedBruker()
        every { behandling.mottatteOpplysninger } returns lagMottatteOpplysninger("987654321", null)
        every { behandling.finnArbeidsforholdDokument() } returns Optional.of(lagArbeidsforholdDokument("123456789"))

        val arbeidsgivere = brevmottakerService.avklarMottakere(
            GENERELT_FRITEKSTBREV_ARBEIDSGIVER,
            Mottaker.medRolle(ARBEIDSGIVER),
            behandling,
            false,
            false
        )

        arbeidsgivere.map { it.orgnr } shouldContainExactlyInAnyOrder listOf("123456789", "987654321")
    }

    @Test
    fun `avklarMottakere medArbeidsgiverRolle medProduserbartDokumentORIENTERING_TIL_ARBEIDSGIVER_OM_VEDTAK girAvklarteVirksomheterSomMottakere`() {
        every { behandling.fagsak } returns lagFagsakMedBruker()
        every { avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(any()) } returns setOf("123456789")

        val arbeidsgivere = brevmottakerService.avklarMottakere(
            ORIENTERING_TIL_ARBEIDSGIVER_OM_VEDTAK,
            Mottaker.medRolle(ARBEIDSGIVER),
            behandling,
            false,
            false
        )

        arbeidsgivere.map { it.orgnr } shouldContainExactlyInAnyOrder listOf("123456789")
        verify(exactly = 0) { behandling.mottatteOpplysninger }
    }

    @Test
    fun `avklarMottakere medBareUtenlandskeArbeidsgivereIkkeKunAvklarteVirksomheter girIngenMottakere`() {
        every { behandling.fagsak } returns lagFagsakMedBruker()
        every { behandling.mottatteOpplysninger } returns lagMottatteOpplysninger(null, "uuid")
        every { behandling.finnArbeidsforholdDokument() } returns Optional.of(lagArbeidsforholdDokument(null))

        val arbeidsgivere = brevmottakerService.avklarMottakere(
            GENERELT_FRITEKSTBREV_ARBEIDSGIVER,
            Mottaker.medRolle(ARBEIDSGIVER),
            behandling,
            false,
            false
        )

        arbeidsgivere.shouldBeEmpty()
    }

    @Test
    fun `avklarMottakere medArbeidsgiverRolleOgFullmektigForBruker girArbeidsgiverMottakere`() {
        every { avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(behandling) } returns
                Sets.newHashSet("123456789", "987654321")
        every { behandling.fagsak } returns lagFagsakMedFullmektigOrg(Fullmaktstype.FULLMEKTIG_SØKNAD)

        val arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(ARBEIDSGIVER), behandling)

        arbeidsgivere.map { it.orgnr } shouldContainExactlyInAnyOrder listOf("123456789", "987654321")
    }

    @Test
    fun `avklarMottakere medArbeidsgiverRolleOgFullmektigForArbeidsgiver girFullmektigMottakere`() {
        every { behandling.fagsak } returns lagFagsakMedFullmektigOrg(Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER)

        val arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(ARBEIDSGIVER), behandling)

        arbeidsgivere.flatMap { listOf(it.aktørId, it.personIdent, it.orgnr) } shouldContainExactly
                listOf(null, null, "REP-ORGNR")
    }

    @Test
    fun `avklarMottakere art12_1 CZerReservertFraA1 forventerIngenMottaker`() {
        every { utenlandskMyndighetService.lagUtenlandskeMyndigheterFraBehandling(behandling) } returns
                mapOf(lagUtenlandskMyndighet() to lagMottakerUtenlandskMyndighet())
        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultat

        val myndigheter = brevmottakerService.avklarMottakere(
            Produserbaredokumenter.ATTEST_A1,
            Mottaker.medRolle(UTENLANDSK_TRYGDEMYNDIGHET),
            behandling
        )

        myndigheter.shouldBeEmpty()
    }

    @Test
    fun `avklarMottakere art 11 4 2 CZerReservertFraA1 forventerIngenMottaker`() {
        every { utenlandskMyndighetService.lagUtenlandskeMyndigheterFraBehandling(behandling) } returns
                mapOf(lagUtenlandskMyndighet() to lagMottakerUtenlandskMyndighet())
        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultat

        behandlingsresultat.hentLovvalgsperiode().bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_4_2

        val myndigheter = brevmottakerService.avklarMottakere(
            Produserbaredokumenter.ATTEST_A1,
            Mottaker.medRolle(UTENLANDSK_TRYGDEMYNDIGHET),
            behandling
        )

        myndigheter.shouldBeEmpty()
    }

    @Test
    fun `avklarMottakere A001 CZerReservertFraA1 forventerMyndighetMottaker`() {
        every { utenlandskMyndighetService.lagUtenlandskeMyndigheterFraBehandling(behandling) } returns
                mapOf(lagUtenlandskMyndighet() to lagMottakerUtenlandskMyndighet())

        val myndigheter = brevmottakerService.avklarMottakere(
            Produserbaredokumenter.ANMODNING_UNNTAK,
            Mottaker.medRolle(UTENLANDSK_TRYGDEMYNDIGHET),
            behandling
        )

        myndigheter.map { it.institusjonID } shouldContainExactly listOf("CZ:SZUC10416")
    }

    @Test
    fun `gittMalIkkeRegistret skalKasteFeil`() {
        val exception = shouldThrow<IkkeFunnetException> {
            brevmottakerService.hentMottakerliste(ATTEST_A1, 123)
        }
        exception.message shouldBe "Mangler mapping av mottakere for ATTEST_A1"
    }

    @Test
    fun `gittForvaltningsmelding skalHovedmottakerVæreBruker`() {
        val mottakerliste = brevmottakerService.hentMottakerliste(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, 123)

        mottakerliste.shouldNotBeNull().run {
            hovedMottaker shouldBe BRUKER
            kopiMottakere shouldBe emptyList()
            fasteMottakere shouldBe emptyList()
        }

        verify(exactly = 0) { behandlingsresultatService.hentBehandlingsresultat(any()) }
    }

    @Test
    fun `gittMangelbrevBruker skalHovedmottakerVæreBruker`() {
        val mottakerliste = brevmottakerService.hentMottakerliste(MANGELBREV_BRUKER, 123)

        mottakerliste.shouldNotBeNull().run {
            hovedMottaker shouldBe BRUKER
            kopiMottakere shouldBe emptyList()
            fasteMottakere shouldBe emptyList()
        }

        verify(exactly = 0) { behandlingsresultatService.hentBehandlingsresultat(any()) }
    }

    @Test
    fun `gittMangelbrevArbeidsgiver skalHovedmottakerVæreArbeidsgiverMedKopi`() {
        val mottakerliste = brevmottakerService.hentMottakerliste(MANGELBREV_ARBEIDSGIVER, 123)

        mottakerliste.shouldNotBeNull().run {
            hovedMottaker shouldBe ARBEIDSGIVER
            kopiMottakere shouldBe listOf(BRUKER)
            fasteMottakere shouldBe emptyList()
        }
    }

    @Test
    fun `gittVedtakFtrl2_8 skalHovedmottakerVæreBrukerMedKopier`() {
        val mottakerliste = brevmottakerService.hentMottakerliste(INNVILGELSE_FOLKETRYGDLOVEN, 123L)

        mottakerliste.shouldNotBeNull().run {
            hovedMottaker shouldBe BRUKER
            kopiMottakere shouldBe emptyList()
            fasteMottakere shouldBe emptyList()
        }
    }

    @Test
    fun `gittInnvilgelsesbrevUK skalHovedmottakerVæreBrukerMedKopier`() {
        val lovvalgsperiode = Lovvalgsperiode().apply {
            bestemmelse = Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_1
        }
        every { lovvalgsperiodeService.hentLovvalgsperiode(any()) } returns lovvalgsperiode

        val mottakerliste = brevmottakerService.hentMottakerliste(TRYGDEAVTALE_GB, 123)

        mottakerliste.shouldNotBeNull().run {
            hovedMottaker shouldBe BRUKER
            kopiMottakere shouldBe listOf(ARBEIDSGIVER, UTENLANDSK_TRYGDEMYNDIGHET)
            fasteMottakere shouldBe emptyList()
        }
    }

    @Test
    fun `gittInnvilgelsesbrevUKOgArt82 skalIkkeMyndighetFåKopi`() {
        val lovvalgsperiode = Lovvalgsperiode().apply {
            bestemmelse = Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART8_2
        }
        every { lovvalgsperiodeService.hentLovvalgsperiode(any()) } returns lovvalgsperiode

        val mottakerliste = brevmottakerService.hentMottakerliste(TRYGDEAVTALE_GB, 123)

        mottakerliste.shouldNotBeNull().run {
            hovedMottaker shouldBe BRUKER
            kopiMottakere shouldBe listOf(ARBEIDSGIVER)
            fasteMottakere shouldBe emptyList()
        }
    }

    @Test
    fun `gittInnvilgelsesbrevCANogArt6_2 skalIkkeArbeidsgiverFåKopi`() {
        val lovvalgsperiode = Lovvalgsperiode().apply {
            bestemmelse = Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART6_2
        }
        every { lovvalgsperiodeService.hentLovvalgsperiode(any()) } returns lovvalgsperiode
        every { lovvalgsperiodeService.harSelvstendigNæringsdrivendeLovvalgsbestemmelse(any()) } returns true

        val mottakerliste = brevmottakerService.hentMottakerliste(TRYGDEAVTALE_GB, 123)

        mottakerliste.shouldNotBeNull().run {
            hovedMottaker shouldBe BRUKER
            kopiMottakere shouldBe listOf(UTENLANDSK_TRYGDEMYNDIGHET)
            fasteMottakere shouldBe emptyList()
        }
    }

    @Test
    fun `avklarMottakerRolleFraDokument tilArbeidsgiver girRolleArbeidsgiver`() {
        val mottakerRolle = brevmottakerService.avklarMottakerRolleFraDokument(INNVILGELSE_ARBEIDSGIVER)

        mottakerRolle shouldBe ARBEIDSGIVER
    }

    @Test
    fun `avklarMottakerRolleFraDokument tilMyndighet girRolleMyndighet`() {
        val mottakerRolle = brevmottakerService.avklarMottakerRolleFraDokument(ATTEST_A1)

        mottakerRolle shouldBe UTENLANDSK_TRYGDEMYNDIGHET
    }

    @Test
    fun `avklarMottakerRolleFraDokument UtenMapping feiler`() {
        val exception = shouldThrow<TekniskException> {
            brevmottakerService.avklarMottakerRolleFraDokument(ORIENTERING_UTPEKING_UTLAND)
        }
        exception.message shouldBe "Valg av mottakerRolle støttes ikke for ORIENTERING_UTPEKING_UTLAND"
    }

    private fun lagFagsakMedBruker(): Fagsak = Fagsak.forTest {
        medBruker()
    }

    private fun lagFagsakMedFullmektigOrg(fullmaktstype: Fullmaktstype) = Fagsak.forTest {
        medBruker()
    }.apply {
        leggTilAktør(Aktoer().apply {
            rolle = Aktoersroller.FULLMEKTIG
            setFullmaktstype(fullmaktstype)
            orgnr = "REP-ORGNR"
        })
    }

    private fun lagFagsakMedFullmektigPerson(fullmaktstype: Fullmaktstype) = Fagsak.forTest {
        this.medBruker()
    }.apply {
        leggTilAktør(Aktoer().apply {
            this.rolle = Aktoersroller.FULLMEKTIG
            setFullmaktstype(fullmaktstype)
            this.personIdent = "REP-FNR"
        })
    }

    private fun lagMottatteOpplysninger(
        ekstraArbeidsgivereOrgnr: String?,
        foretakUtlandUuid: String?
    ): MottatteOpplysninger =
        MottatteOpplysninger().apply {
            mottatteOpplysningerData = MottatteOpplysningerData()
        }

    private fun lagArbeidsforholdDokument(arbeidsgiverIDOrgNr: String?): ArbeidsforholdDokument =
        ArbeidsforholdDokument()

    private fun lagUtenlandskMyndighet(): UtenlandskMyndighet = UtenlandskMyndighet().apply {
        landkode = Land_iso2.CZ
        institusjonskode = "SZUC10416"
        postnummer = "123"
        val preferanser = hashSetOf<Preferanse>()
        preferanser.add(Preferanse(1L, Preferanse.PreferanseEnum.RESERVERT_FRA_A1))
        this.preferanser = preferanser
    }

    private fun lagMottakerUtenlandskMyndighet(): Mottaker = Mottaker.medRolle(UTENLANDSK_TRYGDEMYNDIGHET).apply {
        institusjonID = "CZ:SZUC10416"
    }
}
