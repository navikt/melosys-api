package no.nav.melosys.service.dokument.brev.mapper

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.Årsavregning
import no.nav.melosys.domain.brev.InnhentingAvInntektsopplysningerBrevbestilling
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.exception.FunksjonellException
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

    @Test
    fun `hent inntektsopplysninger for årsavregning skal kaste feil når årsavregning er null`() {
        every { mockDokgenMapperDatahenter.hentBehandlingsresultat(ofType()) } returns Behandlingsresultat().apply { årsavregning = null }

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

        shouldThrow<FunksjonellException> {
            innhentingAvInntektsopplysningerMapper.map(brevbestilling)
        }.message.shouldBe("Årsavregningsår er ikke valgt")
    }

    private fun lagBehandling() =
        Behandling.forTest {
            id = 1L
            fagsak {
                type = Sakstyper.FTRL
            }
            tema = Behandlingstema.YRKESAKTIV
        }

    private fun lagBehandlingsResultat(): Behandlingsresultat {
        return Behandlingsresultat().apply {
            årsavregning = Årsavregning.forTest {
                aar = 2023
                medlemskapsperioder = mutableSetOf(
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
