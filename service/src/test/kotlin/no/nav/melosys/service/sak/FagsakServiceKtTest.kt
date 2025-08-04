package no.nav.melosys.service.sak

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.FagsakTestFactory.SAKSNUMMER
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.Sakstemaer.MEDLEMSKAP_LOVVALG
import no.nav.melosys.domain.kodeverk.Sakstyper.EU_EOS
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.ARBEID_FLERE_LAND
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.UTSENDT_ARBEIDSTAKER
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.FØRSTEGANG
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.NY_VURDERING
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.service.SaksbehandlingDataFactory
import no.nav.melosys.service.aktoer.KontaktopplysningService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.lovligekombinasjoner.LovligeKombinasjonerSaksbehandlingService
import no.nav.melosys.service.persondata.PersondataFasade
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.Instant
import java.util.*

@ExtendWith(MockKExtension::class)
class FagsakServiceKtTest {

    @RelaxedMockK
    lateinit var fagsakRepo: FagsakRepository

    @RelaxedMockK
    lateinit var behandlingService: BehandlingService

    @RelaxedMockK
    lateinit var kontaktopplysningService: KontaktopplysningService

    @RelaxedMockK
    lateinit var persondataFasade: PersondataFasade

    @RelaxedMockK
    lateinit var lovligeKombinasjonerSaksbehandlingService: LovligeKombinasjonerSaksbehandlingService

    private lateinit var fagsakService: FagsakService

    @BeforeEach
    fun setUp() {
        every { fagsakRepo.save(any<Fagsak>()) } answers { firstArg<Fagsak>() }
        every { fagsakRepo.findBySaksnummer(any()) } returns Optional.of(FagsakTestFactory.lagFagsak())
        every { fagsakRepo.findByRolleAndAktør(any(), any()) } returns emptyList()
        every {
            behandlingService.nyBehandling(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns BehandlingTestFactory.builderWithDefaults().build()
        every { behandlingService.endreBehandling(any(), any(), any(), any(), any()) } returns Unit
        every {
            kontaktopplysningService.lagEllerOppdaterKontaktopplysning(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns Kontaktopplysning.av("123456789", "Test Navn", "12345678", "987654321")
        every {
            lovligeKombinasjonerSaksbehandlingService.validerOpprettelseOgEndring(any(), any(), any(), any(), any(), any())
        } returns Unit
        every { persondataFasade.hentAktørIdForIdent(any()) } returns "AKTOER_ID"

        fagsakService = FagsakService(
            fagsakRepo,
            behandlingService,
            kontaktopplysningService,
            persondataFasade,
            lovligeKombinasjonerSaksbehandlingService
        )
    }

    @Test
    fun `hentFagsak`() {
        every { fagsakRepo.findBySaksnummer(any()) } returns Optional.of(FagsakTestFactory.lagFagsak())
        fagsakService.hentFagsak(SAKSNUMMER)
        verify { fagsakRepo.findBySaksnummer(SAKSNUMMER) }
    }

    @Test
    fun `hentFagsakerMedAktør`() {
        every { persondataFasade.hentAktørIdForIdent(any()) } returns "AKTOER_ID"
        fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, "FNR")
        verify { fagsakRepo.findByRolleAndAktør(Aktoersroller.BRUKER, "AKTOER_ID") }
    }

    @Test
    fun `lagre`() {
        val fagsak = FagsakTestFactory.lagFagsak()
        fagsakService.lagre(fagsak)
        verify { fagsakRepo.save(fagsak) }
        fagsak shouldNotBe null
        fagsak.saksnummer shouldNotBe ""
    }

    @Test
    fun `nyFagsakOgBehandling`() {
        val behandling = mockk<Behandling>()
        val initierendeJournalpostId = "234"
        val initierendeDokumentId = "221234"
        every {
            behandlingService.nyBehandling(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns behandling

        val opprettSakRequest = OpprettSakRequest.Builder()
            .medAktørID("123456789")
            .medSakstype(EU_EOS)
            .medSakstema(MEDLEMSKAP_LOVVALG)
            .medBehandlingstype(FØRSTEGANG)
            .medBehandlingstema(UTSENDT_ARBEIDSTAKER)
            .medInitierendeJournalpostId(initierendeJournalpostId)
            .medInitierendeDokumentId(initierendeDokumentId)
            .medArbeidsgiver("arbeidsgiver")
            .medFullmektig(FullmektigDto("orgnr", null, listOf(Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER)))
            .medBehandlingsårsaktype(Behandlingsaarsaktyper.FRITEKST)
            .medBehandlingsårsakFritekst("Fritekst")
            .build()

        val fagsak = fagsakService.nyFagsakOgBehandling(opprettSakRequest)

        verify { fagsakRepo.save(any<Fagsak>()) }
        verify {
            behandlingService.nyBehandling(
                any(),
                eq(Behandlingsstatus.OPPRETTET),
                eq(FØRSTEGANG),
                eq(UTSENDT_ARBEIDSTAKER),
                eq(initierendeJournalpostId),
                eq(initierendeDokumentId),
                any(),
                eq(Behandlingsaarsaktyper.FRITEKST),
                eq("Fritekst")
            )
        }
        fagsak.behandlinger shouldNotBe emptyList<Behandling>()
        fagsak.type shouldBe EU_EOS
        fagsak.tema shouldBe MEDLEMSKAP_LOVVALG
        val lagretFullmektig = fagsak.finnFullmektig(Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER).shouldNotBeNull()
        lagretFullmektig shouldNotBe null
        lagretFullmektig.fagsak shouldBe fagsak
        lagretFullmektig.rolle shouldBe Aktoersroller.FULLMEKTIG
        lagretFullmektig.orgnr shouldBe "orgnr"
        lagretFullmektig.fullmakter.map { it.type } shouldContain Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER
    }

    @Test
    fun `nyFagsakOgBehandling kontaktPersonFinnes KontaktOpplysningOpprettes`() {
        every {
            behandlingService.nyBehandling(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns BehandlingTestFactory.builderWithDefaults().build()

        val kontaktopplysning = Kontaktopplysning.av("FullmektigOrgnr", "Kontaktperson", "Telefon", "Orgnr")
        val opprettSakRequest = OpprettSakRequest.Builder()
            .medAktørID("123456789")
            .medSakstype(EU_EOS)
            .medSakstema(MEDLEMSKAP_LOVVALG)
            .medBehandlingstype(FØRSTEGANG)
            .medKontaktopplysninger(listOf(kontaktopplysning))
            .build()

        fagsakService.nyFagsakOgBehandling(opprettSakRequest)

        verify {
            kontaktopplysningService.lagEllerOppdaterKontaktopplysning(
                any(),
                eq("FullmektigOrgnr"),
                eq("Orgnr"),
                eq("Kontaktperson"),
                eq("Telefon")
            )
        }
    }

    @Test
    fun `oppdaterFagsakOgBehandling`() {
        val fagsak = lagFagsakMedBruker()
        fagsak.leggTilBehandling(SaksbehandlingDataFactory.lagBehandling())
        every { fagsakRepo.findBySaksnummer(SAKSNUMMER) } returns Optional.of(fagsak)

        fagsakService.oppdaterFagsakOgBehandling(
            fagsak.saksnummer,
            Sakstyper.TRYGDEAVTALE,
            MEDLEMSKAP_LOVVALG,
            Behandlingstema.ARBEID_FLERE_LAND,
            NY_VURDERING,
            null,
            null
        )

        verify { fagsakRepo.save(fagsak) }
        verify {
            lovligeKombinasjonerSaksbehandlingService.validerOpprettelseOgEndring(
                fagsak.hovedpartRolle,
                Sakstyper.TRYGDEAVTALE,
                MEDLEMSKAP_LOVVALG,
                Behandlingstema.ARBEID_FLERE_LAND,
                NY_VURDERING
            )
        }
        verify {
            behandlingService.endreBehandling(
                fagsak.finnAktivBehandlingIkkeÅrsavregning()!!.id,
                NY_VURDERING,
                ARBEID_FLERE_LAND,
                null,
                null
            )
        }
    }

    @Test
    fun `oppdaterFagsakOgBehandling ingenEndringPåTypeTema validererIkke`() {
        val fagsak = lagFagsakMedBruker()
        val behandling = SaksbehandlingDataFactory.lagBehandling()
        fagsak.leggTilBehandling(behandling)
        every { fagsakRepo.findBySaksnummer(SAKSNUMMER) } returns Optional.of(fagsak)

        fagsakService.oppdaterFagsakOgBehandling(
            fagsak.saksnummer,
            fagsak.type,
            fagsak.tema,
            behandling.tema,
            behandling.type,
            null,
            null
        )

        verify(exactly = 0) { lovligeKombinasjonerSaksbehandlingService.validerOpprettelseOgEndring(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `leggTilFjernAktørerForMyndighet`() {
        val eksisterendeFagsak = lagFagsakMedAktørforMyndighet()
        every { fagsakRepo.findBySaksnummer(SAKSNUMMER) } returns Optional.of(eksisterendeFagsak)

        val nyeInstitusjonsIder = listOf("Ny institusjonsid")
        fagsakService.oppdaterMyndigheterForEuEos(SAKSNUMMER, nyeInstitusjonsIder)

        verify { fagsakRepo.save(any<Fagsak>()) }
        val oppdaterFagsak = slot<Fagsak>()
        verify { fagsakRepo.save(capture(oppdaterFagsak)) }
        oppdaterFagsak.captured.aktører.map { it.institusjonID } shouldContain "Ny institusjonsid"
    }

    @Test
    fun `oppdaterMyndigheter harBruker fjernerIkkeBruker`() {
        val eksisterendeFagsak = lagFagsakMedAktørforMyndighet()
        every { fagsakRepo.findBySaksnummer(SAKSNUMMER) } returns Optional.of(eksisterendeFagsak)

        val bruker = Aktoer().apply {
            fagsak = eksisterendeFagsak
            rolle = Aktoersroller.BRUKER
            aktørId = "1234"
        }
        eksisterendeFagsak.leggTilAktør(bruker)

        val nyeInstitusjonsIder = listOf("Ny institusjonsid")
        fagsakService.oppdaterMyndigheterForEuEos(SAKSNUMMER, nyeInstitusjonsIder)

        val oppdaterFagsak = slot<Fagsak>()
        verify { fagsakRepo.save(capture(oppdaterFagsak)) }

        val aktørers = oppdaterFagsak.captured.aktører
        aktørers.any { it.rolle == Aktoersroller.BRUKER && it.aktørId == "1234" } shouldBe true
        aktørers.any { it.rolle == Aktoersroller.TRYGDEMYNDIGHET && it.institusjonID == "Ny institusjonsid" } shouldBe true
    }

    @Test
    fun `avsluttFagsakOgBehandling erAktiv blirAvsluttet`() {
        val fagsak = FagsakTestFactory.lagFagsak()
        val behandling = BehandlingTestFactory.builderWithDefaults()
            .medId(1L)
            .medStatus(Behandlingsstatus.UNDER_BEHANDLING)
            .medFagsak(fagsak)
            .build()

        fagsak.leggTilBehandling(behandling)
        fagsakService.avsluttFagsakOgBehandling(fagsak, Saksstatuser.LOVVALG_AVKLART)
        fagsak.status shouldBe Saksstatuser.LOVVALG_AVKLART
        verify { fagsakRepo.save(fagsak) }
        verify { behandlingService.avsluttBehandling(behandling.id) }
    }

    @Test
    fun `avsluttFagsakOgBehandling behandlingTilhørerAnnenFagsak kasterException`() {
        val fagsak = FagsakTestFactory.lagFagsak()

        val behandling = BehandlingTestFactory.builderWithDefaults()
            .medId(1L)
            .medStatus(Behandlingsstatus.UNDER_BEHANDLING)
            .medFagsak(fagsak)
            .build()

        fagsak.leggTilBehandling(behandling)
        behandling.fagsak = FagsakTestFactory.builder().saksnummer("MEL-annenId").build()

        val exception = assertThrows<FunksjonellException> {
            fagsakService.avsluttFagsakOgBehandling(fagsak, behandling, Saksstatuser.LOVVALG_AVKLART)
        }
        exception.message shouldContain "tilhører ikke fagsak"
    }

    @Test
    fun `avsluttFagsakOgBehandling manglerBehandling avslutterFagsak`() {
        val fagsak = FagsakTestFactory.lagFagsak()

        fagsakService.avsluttFagsakOgBehandling(fagsak, Saksstatuser.AVSLUTTET)

        fagsak.status shouldBe Saksstatuser.AVSLUTTET
        verify { fagsakRepo.save(fagsak) }
        verify(exactly = 0) { behandlingService.avsluttBehandling(any()) }
    }

    @Test
    fun `hentFagsakerMedOrgNr riktigSortering`() {
        val behandlingAktivRegistrertNaa = lagBehandling(1L, FØRSTEGANG, Behandlingsstatus.UNDER_BEHANDLING, Instant.now())
        val behandlingInaktivRegistretNaa = lagBehandling(2L, FØRSTEGANG, Behandlingsstatus.AVSLUTTET, Instant.now())
        val behandlingAktivRegistrertFoer = lagBehandling(3L, FØRSTEGANG, Behandlingsstatus.UNDER_BEHANDLING, Instant.now().minusSeconds(3600))
        val behandlingInaktivRegistrertFoer = lagBehandling(4L, FØRSTEGANG, Behandlingsstatus.AVSLUTTET, Instant.now().minusSeconds(3600))
        val fagsak1 = lagFagsakMedAktørForVirksomhet()
        val fagsak2 = lagFagsakMedAktørForVirksomhet()
        val fagsak3 = lagFagsakMedAktørForVirksomhet()
        val fagsak4 = lagFagsakMedAktørForVirksomhet()
        fagsak1.leggTilBehandling(behandlingAktivRegistrertNaa)
        fagsak2.leggTilBehandling(behandlingAktivRegistrertFoer)
        fagsak3.leggTilBehandling(behandlingInaktivRegistretNaa)
        fagsak4.leggTilBehandling(behandlingInaktivRegistrertFoer)

        every { fagsakRepo.findByRolleAndOrgnr(Aktoersroller.VIRKSOMHET, "12345") } returns listOf(fagsak2, fagsak4, fagsak1, fagsak3)

        val fagsakList = fagsakService.hentFagsakerMedOrgnr(Aktoersroller.VIRKSOMHET, "12345")

        fagsakList shouldHaveSize 4
        fagsakList[0].hentSistRegistrertBehandling() shouldBe behandlingAktivRegistrertNaa
        fagsakList[1].hentSistRegistrertBehandling() shouldBe behandlingAktivRegistrertFoer
        fagsakList[2].hentSistRegistrertBehandling() shouldBe behandlingInaktivRegistretNaa
        fagsakList[3].hentSistRegistrertBehandling() shouldBe behandlingInaktivRegistrertFoer
    }

    @ParameterizedTest
    @EnumSource(Betalingstype::class)
    fun `lagreBetalingsvalgForPensjonisterIFagsak`(betalingsvalg: Betalingstype) {
        val fagsak = FagsakTestFactory.lagFagsak()

        every { fagsakRepo.findBySaksnummer(SAKSNUMMER) } returns Optional.of(fagsak)

        fagsakService.lagreBetalingsvalg(SAKSNUMMER, betalingsvalg)

        fagsak.betalingsvalg shouldBe betalingsvalg
    }

    private fun lagFagsakMedAktørforMyndighet(): Fagsak {
        val fagsak = FagsakTestFactory.lagFagsak()

        val aktoer = Aktoer().apply {
            institusjonID = "Gammel institusjonsid"
            this.fagsak = fagsak
            rolle = Aktoersroller.TRYGDEMYNDIGHET
        }
        fagsak.leggTilAktør(aktoer)
        return fagsak
    }

    private fun lagFagsakMedAktørForVirksomhet(): Fagsak {
        val fagsak = FagsakTestFactory.lagFagsak()

        val aktoer = Aktoer().apply {
            orgnr = "12345"
            rolle = Aktoersroller.VIRKSOMHET
        }
        fagsak.leggTilAktør(aktoer)
        return fagsak
    }

    private fun lagFagsakMedBruker(): Fagsak {
        val fagsak = FagsakTestFactory.lagFagsak()

        val aktoer = Aktoer().apply {
            aktørId = "12312"
            rolle = Aktoersroller.BRUKER
        }
        fagsak.leggTilAktør(aktoer)
        return fagsak
    }

    private fun lagBehandling(id: Long, type: Behandlingstyper, status: Behandlingsstatus, registrertDato: Instant): Behandling {
        return BehandlingTestFactory.builderWithDefaults()
            .medId(id)
            .medType(type)
            .medStatus(status)
            .medEndretDato(registrertDato)
            .medRegistrertDato(registrertDato)
            .build()
    }
}
