package no.nav.melosys.saksflyt.steg.melding

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.BehandlingTestFactory
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.behandling
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Vedtakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.lovvalgsperiode
import no.nav.melosys.domain.medlemskapsperiode
import no.nav.melosys.domain.vedtakMetadata
import no.nav.melosys.integrasjon.hendelser.KafkaMelosysHendelseProducer
import no.nav.melosys.integrasjon.hendelser.MelosysHendelse
import no.nav.melosys.integrasjon.hendelser.Periode
import no.nav.melosys.integrasjon.hendelser.VedtakHendelseMelding
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.forTest as prosessinstansForTest
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.persondata.PersondataService
import no.nav.melosys.service.vedtak.VedtakHendelseMeldingBygger
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.Optional

class SendMeldingOmVedtakTest {

    private val kafkaMelosysHendelseProducer: KafkaMelosysHendelseProducer = mockk()
    private val persondataService: PersondataService = mockk()
    private val behandlingsresultatService: BehandlingsresultatService = mockk()

    private val sendMeldingOmVedtak = SendMeldingOmVedtak(
        kafkaMelosysHendelseProducer,
        persondataService,
        VedtakHendelseMeldingBygger(behandlingsresultatService)
    )

    private val sendtHendelse = slot<MelosysHendelse>()

    @BeforeEach
    fun setup() {
        every { kafkaMelosysHendelseProducer.produserBestillingsmelding(capture(sendtHendelse)) } just Runs
    }

    @Test
    fun `sender vedtaksmelding med alle perioder fra behandlingsresultatet`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            behandling {
                fagsak {
                    type = Sakstyper.FTRL
                    tema = Sakstemaer.MEDLEMSKAP_LOVVALG
                    medBruker()
                }
            }
            vedtakMetadata {
                vedtakstype = Vedtakstyper.ENDRINGSVEDTAK
            }
            medlemskapsperiode {
                fom = LocalDate.of(2024, 1, 1)
                tom = LocalDate.of(2024, 12, 31)
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            }
            medlemskapsperiode {
                fom = LocalDate.of(2025, 1, 1)
                tom = LocalDate.of(2025, 6, 30)
                innvilgelsesresultat = InnvilgelsesResultat.AVSLAATT
            }
            lovvalgsperiode {
                fom = LocalDate.of(2024, 1, 1)
                tom = LocalDate.of(2025, 6, 30)
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            }
        }
        every { persondataService.finnFolkeregisterident(FagsakTestFactory.BRUKER_AKTØR_ID) } returns Optional.of(FOLKEREGISTERIDENT)
        every { behandlingsresultatService.hentBehandlingsresultat(BehandlingTestFactory.BEHANDLING_ID) } returns behandlingsresultat

        sendMeldingOmVedtak.utfør(Prosessinstans.prosessinstansForTest { behandling = behandlingsresultat.hentBehandling() })

        val melding = sendtHendelse.captured.melding.shouldBeInstanceOf<VedtakHendelseMelding>()
        melding.folkeregisterIdent shouldBe FOLKEREGISTERIDENT
        melding.sakstype shouldBe Sakstyper.FTRL
        melding.sakstema shouldBe Sakstemaer.MEDLEMSKAP_LOVVALG
        melding.behandligsresultatType shouldBe Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
        melding.vedtakstype shouldBe Vedtakstyper.ENDRINGSVEDTAK
        melding.medlemskapsperioder shouldContainExactlyInAnyOrder listOf(
            Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), InnvilgelsesResultat.INNVILGET),
            Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 6, 30), InnvilgelsesResultat.AVSLAATT)
        )
        melding.lovvalgsperioder shouldBe listOf(
            Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2025, 6, 30), InnvilgelsesResultat.INNVILGET)
        )
    }

    @Test
    fun `sender vedtaksmelding uten vedtakstype når behandlingsresultatet mangler vedtakMetadata`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            behandling { fagsak { medBruker() } }
        }
        every { persondataService.finnFolkeregisterident(FagsakTestFactory.BRUKER_AKTØR_ID) } returns Optional.of(FOLKEREGISTERIDENT)
        every { behandlingsresultatService.hentBehandlingsresultat(BehandlingTestFactory.BEHANDLING_ID) } returns behandlingsresultat

        sendMeldingOmVedtak.utfør(Prosessinstans.prosessinstansForTest { behandling = behandlingsresultat.hentBehandling() })

        val melding = sendtHendelse.captured.melding.shouldBeInstanceOf<VedtakHendelseMelding>()
        melding.vedtakstype shouldBe null
        melding.medlemskapsperioder shouldBe emptyList()
        melding.lovvalgsperioder shouldBe emptyList()
    }

    @Test
    fun `sender ikke når folkeregisterident mangler`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            behandling { fagsak { medBruker() } }
        }
        every { persondataService.finnFolkeregisterident(FagsakTestFactory.BRUKER_AKTØR_ID) } returns Optional.empty()

        sendMeldingOmVedtak.utfør(Prosessinstans.prosessinstansForTest { behandling = behandlingsresultat.hentBehandling() })

        verify(exactly = 0) { behandlingsresultatService.hentBehandlingsresultat(any()) }
        verify(exactly = 0) { kafkaMelosysHendelseProducer.produserBestillingsmelding(any()) }
    }

    private companion object {
        const val FOLKEREGISTERIDENT = "30056928150"
    }
}
