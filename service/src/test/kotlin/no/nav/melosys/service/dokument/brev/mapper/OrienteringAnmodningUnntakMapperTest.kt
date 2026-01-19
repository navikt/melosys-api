package no.nav.melosys.service.dokument.brev.mapper

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.BehandlingTestFactory
import no.nav.melosys.domain.anmodningsperiode
import no.nav.melosys.domain.avklartefakta
import no.nav.melosys.domain.avklartefakta.AvklartYrkesgruppeType
import no.nav.melosys.domain.behandling
import no.nav.melosys.domain.begrunnelse
import no.nav.melosys.domain.brev.OrienteringAnmodningUnntakBrevbestilling
import no.nav.melosys.domain.dokument.personDokumentForTest
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Kodeverk
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.domain.kodeverk.begrunnelser.Anmodning_begrunnelser.ERSTATTER_EN_ANNEN_UNDER_5_AAR
import no.nav.melosys.domain.kodeverk.begrunnelser.Utsendt_arbeidstaker_begrunnelser.UTSENDELSE_OVER_24_MN
import no.nav.melosys.domain.kodeverk.begrunnelser.Utsendt_naeringsdrivende_begrunnelser.IKKE_LIGNENDE_VIRKSOMHET
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_konv_efta_storbritannia
import no.nav.melosys.domain.lovvalgsperiode
import no.nav.melosys.domain.vilkaarsresultatForTest
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
        val vilkaarsresultatNaeringsdrivende = lagVilkaarsresultat(Vilkaar.KONV_EFTA_STORBRITANNIA_ART14_1, true, IKKE_LIGNENDE_VIRKSOMHET)
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
        every { mockVilkaarsresultatService.finnUtsendingNæringsdrivendeVilkaarsresultat(ofType()) } returns vilkaarsresultatNaeringsdrivende
        every { mockVilkaarsresultatService.finnVilkaarsresultat(ofType(), ofType()) } returns vilkaarsresultatNaeringsdrivende
        every { mockLandvelgerService.hentArbeidsland(ofType()) } returns Land_iso2.NO

        val personDokument = personDokumentForTest { sammensattNavn = "Hei Test" }

        val brevbestilling =
            OrienteringAnmodningUnntakBrevbestilling.Builder()
                .medBehandling(lagBehandling())
                .medPersonDokument(personDokument)
                .medPersonMottaker(personDokument)
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

    private fun lagBehandling(
        init: no.nav.melosys.domain.BehandlingTestFactory.BehandlingTestBuilder.() -> Unit = {}
    ) = Behandling.forTest {
        id = 1L
        fagsak {
            type = Sakstyper.FTRL
        }
        tema = Behandlingstema.YRKESAKTIV
        init()
    }

    private fun lagBehandlingsResultat() = Behandlingsresultat.forTest {
        behandling {
            id = 1L
            fagsak { type = Sakstyper.FTRL }
            tema = Behandlingstema.YRKESAKTIV
        }
        avklartefakta { fakta = AvklartYrkesgruppeType.ORDINAER_UTEN_ART12.name }
        lovvalgsperiode {
            fom = LocalDate.of(2020, 1, 1)
            tom = LocalDate.of(2021, 2, 1)
            lovvalgsland = Land_iso2.NO
            bestemmelse = Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_1
        }
        anmodningsperiode {
            fom = LocalDate.now().minusMonths(4)
            tom = LocalDate.now().plusMonths(4)
            lovvalgsland = Land_iso2.NO
            bestemmelse = Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_1
            unntakFraLovvalgsland = Land_iso2.SE
            unntakFraBestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
            dekning = Trygdedekninger.FULL_DEKNING_EOSFO
        }
    }

    private fun lagVilkaarsresultat(vilkaar: Vilkaar?, oppfylt: Boolean, vararg vilkaarbegrunnelser: Kodeverk) =
        vilkaarsresultatForTest {
            this.vilkaar = vilkaar ?: Vilkaar.FO_883_2004_ART12_1
            isOppfylt = oppfylt
            vilkaarbegrunnelser.forEach { begrunnelse(it.kode) }
        }
}
