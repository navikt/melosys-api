package no.nav.melosys.service.dokument.brev.mapper

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avklartefakta.AvklartYrkesgruppeType
import no.nav.melosys.domain.avklartefakta.Avklartefakta
import no.nav.melosys.domain.brev.OrienteringAnmodningUnntakBrevbestilling
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.begrunnelser.Anmodning_begrunnelser.ERSTATTER_EN_ANNEN_UNDER_5_AAR
import no.nav.melosys.domain.kodeverk.begrunnelser.Utsendt_arbeidstaker_begrunnelser.UTSENDELSE_OVER_24_MN
import no.nav.melosys.domain.kodeverk.begrunnelser.Utsendt_naeringsdrivende_begrunnelser.IKKE_LIGNENDE_VIRKSOMHET
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_konv_efta_storbritannia
import no.nav.melosys.service.LandvelgerService
import no.nav.melosys.service.behandling.VilkaarsresultatService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate


@ExtendWith(MockKExtension::class)
internal class OrienteringAnmodningUnntakMapperTest {

    @MockK
    private lateinit var mockDokgenMapperDatahenter: DokgenMapperDatahenter

    @MockK
    private lateinit var mockVilkaarsresultatService: VilkaarsresultatService

    @MockK
    private lateinit var mockLandvelgerService: LandvelgerService

    private lateinit var orienteringAnmodningUnntakMapper: OrienteringAnmodningUnntakMapper

    @BeforeEach
    fun setup() {
        orienteringAnmodningUnntakMapper = OrienteringAnmodningUnntakMapper(
            mockDokgenMapperDatahenter,
            mockVilkaarsresultatService,
            mockLandvelgerService
        )
    }

    @Test
    fun `hent orientering anmodning unntak brevbestilling`() {
        val vilkaarsresultatArbeidstaker = lagVilkaarsresultat(Vilkaar.FO_883_2004_ART12_1, true, UTSENDELSE_OVER_24_MN)
        val vilkaarsresultatNæringsdrivende = lagVilkaarsresultat(Vilkaar.KONV_EFTA_STORBRITANNIA_ART14_1, true, IKKE_LIGNENDE_VIRKSOMHET)
        val unntaksVilkaar = lagVilkaarsresultat(Vilkaar.FO_883_2004_ART16_1, true, ERSTATTER_EN_ANNEN_UNDER_5_AAR)


        every { mockDokgenMapperDatahenter.hentBehandlingsresultat(ofType()) } returns lagBehandlingsResultat()
        every {
            mockVilkaarsresultatService.harVilkaar(
                ofType(), listOf(
                    Vilkaar.FO_883_2004_ART12_1,
                    Vilkaar.KONV_EFTA_STORBRITANNIA_ART14_1,
                    Vilkaar.KONV_EFTA_STORBRITANNIA_ART16_1
                )
            )
        } returns true
        every {
            mockVilkaarsresultatService.harVilkaar(
                ofType(), listOf(
                    Vilkaar.FO_883_2004_ART12_2,
                    Vilkaar.KONV_EFTA_STORBRITANNIA_ART14_2,
                    Vilkaar.KONV_EFTA_STORBRITANNIA_ART16_3
                )
            )
        } returns false
        every { mockVilkaarsresultatService.finnUnntaksVilkaarsresultat(ofType()) } returns unntaksVilkaar
        every { mockVilkaarsresultatService.finnUtsendingArbeidstakerVilkaarsresultat(ofType()) } returns vilkaarsresultatArbeidstaker
        every { mockVilkaarsresultatService.finnUtsendingNæringsdrivendeVilkaarsresultat(ofType()) } returns vilkaarsresultatNæringsdrivende
        every { mockVilkaarsresultatService.finnVilkaarsresultat(ofType(), ofType()) } returns vilkaarsresultatNæringsdrivende
        every { mockLandvelgerService.hentArbeidsland(ofType()) } returns Land_iso2.NO

        val brevbestilling =
            OrienteringAnmodningUnntakBrevbestilling.Builder()
                .medBehandling(lagBehandling())
                .medPersonDokument(PersonDokument().apply {
                    sammensattNavn = "Hei Test"
                })
                .medPersonMottaker(PersonDokument().apply {
                    sammensattNavn = "Hei Test"
                })
                .build()

        orienteringAnmodningUnntakMapper.map(brevbestilling).run {
            periodeFom.shouldBe(LocalDate.now().minusMonths(4))
            periodeTom.shouldBe(LocalDate.now().plusMonths(4))
            arbeidsland.shouldBe(Land_iso2.NO.beskrivelse)
            erDirekteTilAnmodningOmUnntak.shouldBe(true)
            erAnmodningOmUnntakViaArbeidstaker.shouldBe(true)
            erAnmodningOmUnntakViaNæringsdrivende.shouldBe(false)
            lovvalgsbestemmelse.shouldBe(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_1.name)
            begrunnelser.shouldBe(listOf(UTSENDELSE_OVER_24_MN.name, IKKE_LIGNENDE_VIRKSOMHET.name))
            direkteTilAnmodningBegrunnelser.shouldBe(listOf(ERSTATTER_EN_ANNEN_UNDER_5_AAR.name))
            anmodningBegrunnelser.shouldBe(listOf())
            fritekst.shouldBeNull()
        }
    }

    private fun lagBehandling(block: Behandling.() -> Unit = {}) = Behandling.forTest {
        id = 1L
        fagsak {
            type = Sakstyper.FTRL
        }
        tema = Behandlingstema.YRKESAKTIV
    }.apply { block() }

    private fun lagBehandlingsResultat(): Behandlingsresultat {
        return Behandlingsresultat().apply {
            behandling = lagBehandling()
            avklartefakta = mutableSetOf(Avklartefakta().apply {
                fakta = AvklartYrkesgruppeType.ORDINAER_UTEN_ART12.name
            })
            lovvalgsperioder = mutableSetOf(Lovvalgsperiode().apply {
                fom = LocalDate.of(2020, 1, 1)
                tom = LocalDate.of(2021, 2, 1)
                lovvalgsland = Land_iso2.NO
                bestemmelse = Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_1
            })
            anmodningsperioder = mutableSetOf(lagAnmodningsperiode())

        }
    }

    private fun lagAnmodningsperiode(): Anmodningsperiode {
        return Anmodningsperiode(
            LocalDate.now().minusMonths(4),
            LocalDate.now().plusMonths(4),
            Land_iso2.NO, Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_1, null,
            Land_iso2.SE, Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1, Trygdedekninger.FULL_DEKNING_EOSFO
        )
    }

    fun lagVilkaarsresultat(vilkaar: Vilkaar?, oppfylt: Boolean, vararg vilkårbegrunnelser: Kodeverk): Vilkaarsresultat {
        val vilkaarsresultat = Vilkaarsresultat()
        vilkaarsresultat.isOppfylt = oppfylt
        vilkaarsresultat.vilkaar = vilkaar
        vilkaarsresultat.begrunnelser = HashSet()
        for (begrunnelseKode in vilkårbegrunnelser) {
            val begrunnelse = VilkaarBegrunnelse()
            begrunnelse.kode = begrunnelseKode.kode
            vilkaarsresultat.begrunnelser.add(begrunnelse)
        }
        return vilkaarsresultat
    }
}
