package no.nav.melosys.saksflyt.steg.sed

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.eessi.BucInformasjon
import no.nav.melosys.domain.eessi.BucType
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.saksflytapi.domain.*
import no.nav.melosys.service.LandvelgerService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.dokument.sed.EessiService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.util.*

@ExtendWith(MockKExtension::class)
class HentMottakerinstitusjonerForkortetPeriodeTest {

    private lateinit var hentMottakerinstitusjonerForkortetPeriode: HentMottakerinstitusjonerForkortetPeriode

    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @MockK
    private lateinit var eessiService: EessiService

    @MockK
    private lateinit var landvelgerService: LandvelgerService

    private val behandlingId = 34L

    @BeforeEach
    fun setUp() {
        hentMottakerinstitusjonerForkortetPeriode = HentMottakerinstitusjonerForkortetPeriode(
            behandlingsresultatService,
            eessiService,
            landvelgerService
        )

        val behandlingsresultat = Behandlingsresultat().apply {
            id = behandlingId
            lovvalgsperioder = setOf(
                Lovvalgsperiode().apply {
                    bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
                }
            )
        }
        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultat
        every { landvelgerService.hentUtenlandskTrygdemyndighetsland(any()) } returns emptySet()
    }

    @Test
    fun `utfør skal sette mottaker institusjoner når har tidligere BUC`() {
        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            behandling {
                id = behandlingId
                fagsak {
                    gsakSaksnummer = 123456789L
                }
            }
        }
        val mottakerInstitusjoner = setOf("SE:123")

        every { eessiService.landErEessiReady(any<String>(), any<Collection<Land_iso2>>()) } returns true
        every { eessiService.hentTilknyttedeBucer(any<Long>(), any<List<String>>()) } returns listOf(
            BucInformasjon("123", true, BucType.LA_BUC_04.name, LocalDate.now(), mottakerInstitusjoner, Collections.emptyList())
        )


        hentMottakerinstitusjonerForkortetPeriode.utfør(prosessinstans)


        prosessinstans.hentData<Set<String>>(ProsessDataKey.EESSI_MOTTAKERE) shouldBe mottakerInstitusjoner
    }

    @Test
    fun `utfør skal ikke sette mottaker institusjoner når ikke EESSI ready`() {
        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            behandling {
                id = behandlingId
                fagsak {
                    gsakSaksnummer = 123456789L
                }
            }
        }
        val mottakerInstitusjoner = emptySet<String>()

        every { eessiService.landErEessiReady(any<String>(), any<Collection<Land_iso2>>()) } returns true
        every { eessiService.hentTilknyttedeBucer(any<Long>(), any<List<String>>()) } returns listOf(
            BucInformasjon("123", true, BucType.LA_BUC_04.name, LocalDate.now(), mottakerInstitusjoner, Collections.emptyList())
        )


        hentMottakerinstitusjonerForkortetPeriode.utfør(prosessinstans)


        prosessinstans.hentData<Set<String>>(ProsessDataKey.EESSI_MOTTAKERE) shouldBe mottakerInstitusjoner
    }

    @Test
    fun `utfør skal kaste exception når er EESSI ready men finner ingen BUC`() {
        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            behandling {
                id = behandlingId
                fagsak {
                    gsakSaksnummer = 123456789L
                }
            }
        }

        every { eessiService.landErEessiReady(any<String>(), any<Collection<Land_iso2>>()) } returns true
        every { eessiService.hentTilknyttedeBucer(any<Long>(), any<List<String>>()) } returns emptyList()


        shouldThrow<TekniskException> {
            hentMottakerinstitusjonerForkortetPeriode.utfør(prosessinstans)
        }.message shouldContain "er EESSI-ready, men har ingen tidligere buc tilknyttet seg"
    }
}
