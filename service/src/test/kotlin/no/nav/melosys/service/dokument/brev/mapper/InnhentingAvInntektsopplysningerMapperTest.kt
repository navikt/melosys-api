package no.nav.melosys.service.dokument.brev.mapper

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.brev.InnhentingAvInntektsopplysningerBrevbestilling
import no.nav.melosys.domain.dokument.personDokumentForTest
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.medlemskapsperiode
import no.nav.melosys.domain.årsavregning
import no.nav.melosys.exception.FunksjonellException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
internal class InnhentingAvInntektsopplysningerMapperTest {

    @MockK
    private lateinit var mockDokgenMapperDatahenter: DokgenMapperDatahenter

    private val innhentingAvInntektsopplysningerMapper by lazy {
        InnhentingAvInntektsopplysningerMapper(mockDokgenMapperDatahenter)
    }

    @Test
    fun `hent inntektsopplysninger for årsavregning`() {
        every { mockDokgenMapperDatahenter.hentBehandlingsresultat(ofType()) } returns lagBehandlingsResultat()

        val personDokument = personDokumentForTest { sammensattNavn = "Hei Test" }

        val brevbestilling =
            InnhentingAvInntektsopplysningerBrevbestilling.Builder()
                .medBehandling(lagBehandling())
                .medPersonDokument(personDokument)
                .medPersonMottaker(personDokument)
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
        every { mockDokgenMapperDatahenter.hentBehandlingsresultat(ofType()) } returns Behandlingsresultat.forTest { }

        val personDokument = personDokumentForTest { sammensattNavn = "Hei Test" }

        val brevbestilling =
            InnhentingAvInntektsopplysningerBrevbestilling.Builder()
                .medBehandling(lagBehandling())
                .medPersonDokument(personDokument)
                .medPersonMottaker(personDokument)
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

    private fun lagBehandlingsResultat() =
        Behandlingsresultat.forTest {
            årsavregning { aar = 2023 }
            medlemskapsperiode {
                fom = LocalDate.of(2022, 5, 17)
                tom = LocalDate.of(2022, 8, 17)
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            }
            medlemskapsperiode {
                fom = LocalDate.of(2022, 8, 18)
                tom = LocalDate.of(2023, 8, 17)
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            }
            medlemskapsperiode {
                fom = LocalDate.of(2023, 8, 18)
                tom = LocalDate.of(2023, 9, 1)
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            }
        }
}
