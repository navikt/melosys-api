package no.nav.melosys.service.behandling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avklartefakta.Avklartefakta
import no.nav.melosys.domain.kodeverk.Utfallregistreringunntak
import no.nav.melosys.domain.kodeverk.begrunnelser.Henleggelsesgrunner
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.repository.BehandlingsresultatRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(MockKExtension::class)
class BehandlingsresultatServiceTest {

    @MockK
    private lateinit var behandlingsresultatRepo: BehandlingsresultatRepository

    private lateinit var behandlingsresultatService: BehandlingsresultatService
    private val behandlingsresultatCaptor = slot<Behandlingsresultat>()

    @BeforeEach
    fun setUp() {
        behandlingsresultatService = BehandlingsresultatService(behandlingsresultatRepo)
    }

    @Test
    fun tømBehandlingsresultat() {
        val behandlingID = 1L
        val behandlingsresultat = Behandlingsresultat().apply {
            id = behandlingID
            avklartefakta = hashSetOf(Avklartefakta())
            lovvalgsperioder = hashSetOf(Lovvalgsperiode())
            vilkaarsresultater = hashSetOf(Vilkaarsresultat())
            utfallRegistreringUnntak = Utfallregistreringunntak.GODKJENT
            innledningFritekst = "Innledning fritekst"
            begrunnelseFritekst = "Begrunnelse fritekst"
            nyVurderingBakgrunn = "ny vurdering bakgrunn"
            trygdeavgiftFritekst = "trygdeavgift fritekst"
            behandling = Behandling.forTest {
                id = behandlingID
                fagsak = Fagsak.forTest()
            }
        }

        every { behandlingsresultatRepo.findById(any()) } returns Optional.of(behandlingsresultat)
        every { behandlingsresultatRepo.save(behandlingsresultat) } returns behandlingsresultat


        behandlingsresultatService.tømBehandlingsresultat(1L)


        behandlingsresultat.run {
            avklartefakta.shouldBeEmpty()
            lovvalgsperioder.shouldBeEmpty()
            utfallRegistreringUnntak.shouldBeNull()
            innledningFritekst.shouldBeNull()
            begrunnelseFritekst.shouldBeNull()
            nyVurderingBakgrunn.shouldBeNull()
            trygdeavgiftFritekst.shouldBeNull()
            vilkaarsresultater.shouldBeEmpty()
        }
        verify(exactly = 1) { behandlingsresultatRepo.save(behandlingsresultat) }
    }

    @Test
    fun hentBehandlingsresultat_medTomtResultat_forventerException() {
        every { behandlingsresultatRepo.findById(any()) } returns Optional.empty()

        shouldThrow<IkkeFunnetException> {
            behandlingsresultatService.hentBehandlingsresultat(4L)
        }.message shouldContain "Kan ikke finne"
    }

    @Test
    fun hentBehandlingsresultat_returnererBehandlingsresultat() {
        val resultat = Behandlingsresultat().apply {
            behandlingsresultatBegrunnelser = mutableSetOf(BehandlingsresultatBegrunnelse().apply {
                kode = Henleggelsesgrunner.ANNET.kode
            })
        }
        every { behandlingsresultatRepo.findById(any()) } returns Optional.of(resultat)


        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(4L)


        val begrunnelse = behandlingsresultat.behandlingsresultatBegrunnelser.first()
        begrunnelse.kode shouldBe Henleggelsesgrunner.ANNET.kode
    }

    @Test
    fun lagreNyttBehandlingsresultat_lagresKorrekt() {
        val behandling = Behandling.forTest()

        every { behandlingsresultatRepo.save(capture(behandlingsresultatCaptor)) } returns Behandlingsresultat()


        behandlingsresultatService.lagreNyttBehandlingsresultat(behandling)


        with(behandlingsresultatCaptor.captured) {
            behandling shouldBe behandling
            behandlingsmåte shouldBe Behandlingsmaate.MANUELT
            type shouldBe Behandlingsresultattyper.IKKE_FASTSATT
        }
    }

    @Test
    fun lagreBehandlingsResultat_godkjent_erRegistrertUnntak() {
        val behandlingID = 123L
        val utfallUtpeking = Utfallregistreringunntak.GODKJENT

        val mockBehandlingsresultat = Behandlingsresultat()
        every { behandlingsresultatRepo.findById(behandlingID) } returns Optional.of(mockBehandlingsresultat)
        every { behandlingsresultatRepo.findWithKontrollresultaterById(behandlingID) } returns Optional.of(mockBehandlingsresultat)
        every { behandlingsresultatRepo.save(any()) } returns mockBehandlingsresultat


        behandlingsresultatService.settUtfallRegistreringUnntakOgType(behandlingID, utfallUtpeking)


        mockBehandlingsresultat.type shouldBe Behandlingsresultattyper.REGISTRERT_UNNTAK
    }

    @Test
    fun lagreBehandlingsResultat_ikkeGodkjent_erFerdigbehandlet() {
        val behandlingID = 123L
        val utfallUtpeking = Utfallregistreringunntak.IKKE_GODKJENT

        val mockBehandlingsresultat = Behandlingsresultat()
        every { behandlingsresultatRepo.findById(behandlingID) } returns Optional.of(mockBehandlingsresultat)
        every { behandlingsresultatRepo.findWithKontrollresultaterById(behandlingID) } returns Optional.of(mockBehandlingsresultat)
        every { behandlingsresultatRepo.save(any()) } returns mockBehandlingsresultat


        behandlingsresultatService.settUtfallRegistreringUnntakOgType(behandlingID, utfallUtpeking)


        mockBehandlingsresultat.type shouldBe Behandlingsresultattyper.FERDIGBEHANDLET
    }

    @Test
    fun oppdaterBehandlingsresultattype_idEksisterer_oppdatererBehandlingsresultattype() {
        val behandlingsresultat = Behandlingsresultat().apply {
            type = Behandlingsresultattyper.ANMODNING_OM_UNNTAK
        }
        every { behandlingsresultatRepo.findById(1L) } returns Optional.of(behandlingsresultat)
        every { behandlingsresultatRepo.save(any()) } returns behandlingsresultat


        behandlingsresultatService.oppdaterBehandlingsresultattype(1L, Behandlingsresultattyper.IKKE_FASTSATT)


        behandlingsresultat.type shouldBe Behandlingsresultattyper.IKKE_FASTSATT
        verify { behandlingsresultatRepo.save(behandlingsresultat) }
    }

    @Test
    fun oppdaterBehandlingsresultattype_idEksistererIkke_gjørIngenting() {
        every { behandlingsresultatRepo.findById(1L) } returns Optional.empty()


        behandlingsresultatService.oppdaterBehandlingsresultattype(1L, Behandlingsresultattyper.IKKE_FASTSATT)


        verify { behandlingsresultatRepo.findById(1L) }
        verify(exactly = 0) { behandlingsresultatRepo.save(any()) }
    }

    @Test
    fun oppdaterBehandlingsmaate_bhmåteUdefinert_verifiserOppdatert() {
        val behandlingsresultat = Behandlingsresultat().apply {
            behandlingsmåte = Behandlingsmaate.MANUELT
        }
        every { behandlingsresultatRepo.findById(any()) } returns Optional.of(behandlingsresultat)
        every { behandlingsresultatRepo.save(any()) } returns behandlingsresultat


        behandlingsresultatService.oppdaterBehandlingsMaate(1L, Behandlingsmaate.AUTOMATISERT)


        verify { behandlingsresultatRepo.save(behandlingsresultat) }
        behandlingsresultat.behandlingsmåte shouldBe Behandlingsmaate.AUTOMATISERT
    }

    @Test
    fun settUtfallRegistreringUnntakOgType_ikkeSatt_lagres() {
        val behandlingsresultat = Behandlingsresultat()
        every { behandlingsresultatRepo.findById(1L) } returns Optional.of(behandlingsresultat)
        every { behandlingsresultatRepo.findWithKontrollresultaterById(1L) } returns Optional.of(behandlingsresultat)
        every { behandlingsresultatRepo.save(any()) } returns behandlingsresultat


        behandlingsresultatService.settUtfallRegistreringUnntakOgType(1, Utfallregistreringunntak.GODKJENT)


        verify { behandlingsresultatRepo.save(behandlingsresultat) }
    }

    @Test
    fun settUtfallRegistreringUnntakOgType_alleredeSatt_kasterException() {
        val behandlingsresultat = Behandlingsresultat().apply {
            utfallRegistreringUnntak = Utfallregistreringunntak.GODKJENT
        }
        every { behandlingsresultatRepo.findById(1L) } returns Optional.of(behandlingsresultat)


        shouldThrow<FunksjonellException> {
            behandlingsresultatService.settUtfallRegistreringUnntakOgType(1, Utfallregistreringunntak.GODKJENT)
        }.message shouldContain "Utfall for registrering av unntak er allerede satt for behandlingsresultat"
    }

    @Test
    fun oppdaterUtfallRegistreringUnntak_alleredeSatt_oppdaterer() {
        val behandlingsresultat = Behandlingsresultat().apply {
            utfallRegistreringUnntak = Utfallregistreringunntak.GODKJENT
        }
        every { behandlingsresultatRepo.findWithKontrollresultaterById(1L) } returns Optional.of(behandlingsresultat)
        every { behandlingsresultatRepo.save(any()) } returns behandlingsresultat


        behandlingsresultatService.oppdaterUtfallRegistreringUnntak(1, Utfallregistreringunntak.DELVIS_GODKJENT)


        verify { behandlingsresultatRepo.save(behandlingsresultat) }
    }

    @Test
    fun oppdaterBegrunnelser_enBegrunnelse_blirLagret() {
        val behandlingsresultatBegrunnelse = BehandlingsresultatBegrunnelse().apply {
            kode = "koden"
        }
        val behandlingsresultat = Behandlingsresultat()
        every { behandlingsresultatRepo.findById(1L) } returns Optional.of(behandlingsresultat)
        every { behandlingsresultatRepo.save(any()) } returns behandlingsresultat


        behandlingsresultatService.oppdaterBegrunnelser(1L, setOf(behandlingsresultatBegrunnelse), "fri")


        verify { behandlingsresultatRepo.save(behandlingsresultat) }
        behandlingsresultatBegrunnelse.behandlingsresultat shouldBe behandlingsresultat
    }

    @Test
    fun oppdaterFritekster_altOk_blirLagret() {
        val behandlingsresultat = Behandlingsresultat().apply {
            id = 1L
        }
        every { behandlingsresultatRepo.findById(1L) } returns Optional.of(behandlingsresultat)
        every { behandlingsresultatRepo.save(any()) } returns behandlingsresultat


        behandlingsresultatService.oppdaterFritekster(
            1L, "fritekst for begrunnelse", "fritekst for innledning", "fritekst for trygdeavgift"
        )


        verify { behandlingsresultatRepo.save(capture(behandlingsresultatCaptor)) }
        with(behandlingsresultatCaptor.captured) {
            begrunnelseFritekst shouldBe "fritekst for begrunnelse"
            innledningFritekst shouldBe "fritekst for innledning"
            trygdeavgiftFritekst shouldBe "fritekst for trygdeavgift"
        }
    }
}
