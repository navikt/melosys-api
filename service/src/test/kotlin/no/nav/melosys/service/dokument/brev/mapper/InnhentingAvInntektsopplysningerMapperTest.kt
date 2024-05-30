package no.nav.melosys.service.dokument.brev.mapper

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.Aarsavregning
import no.nav.melosys.domain.brev.InnhentingAvInntektsopplysningerBrevbestilling
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
internal class InnhentingAvInntektsopplysningerMapperTest {

    @MockK
    private lateinit var mockDokgenMapperDatahenter: DokgenMapperDatahenter

    private lateinit var innhentingAvInntektsopplysningerMapper: InnhentingAvInntektsopplysningerMapper

    @BeforeEach
    fun setup() {
        innhentingAvInntektsopplysningerMapper = InnhentingAvInntektsopplysningerMapper(
            mockDokgenMapperDatahenter,
        )
    }

    @Test
    fun `hent inntektsopplysninger for årsavregning`() {
        every { mockDokgenMapperDatahenter.hentBehandlingsresultat(ofType()) } returns lagBehandlingsResultat()

        val brevbestilling =
            InnhentingAvInntektsopplysningerBrevbestilling.Builder()
                .medBehandling(lagBehandling())
                .medPersonDokument(PersonDokument().apply {
                    sammensattNavn = "Hei Test"
                })
                .medPersonMottaker(PersonDokument().apply {
                        sammensattNavn = "Hei Test"
                    })
                .build()

        innhentingAvInntektsopplysningerMapper.map(brevbestilling).run {
            årsavregningsår.shouldBe(2023)
            fristdato.shouldBe(LocalDate.now().plusWeeks(4))
            fritekst.shouldBeNull()
            medlemskapsperiodeFom.shouldBe(LocalDate.of(2023, 1, 1))
            medlemskapsperiodeTom.shouldBe(LocalDate.of(2023, 9, 1))
        }
    }

    private fun lagBehandling(block: Behandling.() -> Unit = {}): Behandling = Behandling().apply behandling@{
        id = 1L
        fagsak = FagsakTestFactory.builder().apply {
            type = Sakstyper.FTRL
            leggTilBehandling(this@behandling)
        }.build()
        tema = Behandlingstema.YRKESAKTIV
        block()
    }

    private fun lagBehandlingsResultat(): Behandlingsresultat {
        return Behandlingsresultat().apply {
            behandling = lagBehandling()
            medlemAvFolketrygden = MedlemAvFolketrygden().apply {
                aarsavregning = Aarsavregning().apply {
                    aar = 2023
                }
                medlemskapsperioder = listOf(
                    Medlemskapsperiode().apply {
                        fom = LocalDate.of(2022, 5, 17)
                        tom = LocalDate.of(2022, 8, 17)
                        trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    },
                    Medlemskapsperiode().apply {
                        fom = LocalDate.of(2022, 8, 18)
                        tom = LocalDate.of(2023, 8, 17)
                        trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    },
                    Medlemskapsperiode().apply {
                        fom = LocalDate.of(2023, 8, 18)
                        tom = LocalDate.of(2023, 9, 1)
                        trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    })
            }
        }
    }
}
