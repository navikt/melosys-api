package no.nav.melosys.saksflyt.steg.faktureringskomponenten

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.finn.unleash.FakeUnleash
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Trygdeavgiftsgrunnlag
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.saksflyt.ProsessDataKey
import no.nav.melosys.domain.saksflyt.Prosessinstans
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.integrasjon.faktureringskomponenten.FaktureringskomponentenConsumer
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FaktureringsIntervall
import no.nav.melosys.saksflyt.faktureringskomponenten.OpprettBetalingsplan
import no.nav.melosys.service.aktoer.KontaktopplysningService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.persondata.PersondataService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*

@ExtendWith(MockKExtension::class)
class OpprettBetalingsplanTest {

    @RelaxedMockK
    lateinit var behandlingService: BehandlingService

    @RelaxedMockK
    lateinit var behandlingsresultatService: BehandlingsresultatService

    @RelaxedMockK
    lateinit var faktureringskomponentenConsumer: FaktureringskomponentenConsumer

    @RelaxedMockK
    lateinit var kontaktopplysningService: KontaktopplysningService

    @RelaxedMockK
    lateinit var pdlService: PersondataService

    private val unleash = FakeUnleash()

    lateinit var opprettBetalingsplan: OpprettBetalingsplan

    private lateinit var behandling: Behandling
    private lateinit var fagsak: Fagsak
    private lateinit var prosessinstans: Prosessinstans
    private lateinit var fastsattTrygdeavgift: FastsattTrygdeavgift
    private lateinit var behandlingsresultat: Behandlingsresultat

    @BeforeEach
    internal fun setUp() {
        unleash.enable("melosys.folketrygden.mvp")

        opprettBetalingsplan = OpprettBetalingsplan(
            behandlingService,
            behandlingsresultatService,
            faktureringskomponentenConsumer,
            kontaktopplysningService,
            pdlService,
            unleash
        )
    }

    private fun lagTestData(fagsak: Fagsak = lagFagsak(), behandling: Behandling = lagBehandling(fagsak)) {
        this.fagsak = fagsak
        this.behandling = behandling
        prosessinstans = Prosessinstans().apply {
            setData(ProsessDataKey.BETALINGSINTERVALL, FaktureringsIntervall.KVARTAL)
            this.behandling = behandling
        }
        behandlingsresultat = lagBehandlingsresultat()
        fastsattTrygdeavgift = behandlingsresultat.medlemAvFolketrygden.fastsattTrygdeavgift
    }

    @Test
    fun `Opprett betalingsplan med riktige verdier`() {
        lagTestData(lagFagsak().apply {
            aktører = setOf(
                lagAktoerOrg(Aktoersroller.REPRESENTANT, "123456789"),
                lagAktoerPerson("11111111111")
            )
        })

        every {
            behandlingsresultatService.hentBehandlingsresultat(prosessinstans.behandling.id)
        } returns behandlingsresultat

        every {
            behandlingService.hentBehandling(behandlingsId)
        } returns behandling

        every {
            kontaktopplysningService.hentKontaktopplysning(
                fagsak.saksnummer,
                fastsattTrygdeavgift.betalesAv.orgnr
            )
        } returns Optional.of(lagKontaktOpplysning())

        every {
            pdlService.finnFolkeregisterident("11111111111")
        } returns Optional.of("12345678911")

        opprettBetalingsplan.utfør(prosessinstans)

        verify(exactly = 1) {
            faktureringskomponentenConsumer.lagFakturaSerie(any())
        }
    }

    @Test
    fun `Opprett betalingsplan uten kontaktopplysning skal fungere`() {
        lagTestData(lagFagsak().apply {
            aktører = setOf(
                lagAktoerOrg(Aktoersroller.REPRESENTANT, "123456789"),
                lagAktoerPerson("11111111111")
            )
        })

        every {
            behandlingsresultatService.hentBehandlingsresultat(prosessinstans.behandling.id)
        } returns behandlingsresultat

        every {
            behandlingService.hentBehandling(behandlingsId)
        } returns behandling

        every {
            kontaktopplysningService.hentKontaktopplysning(
                fagsak.saksnummer,
                fastsattTrygdeavgift.betalesAv.orgnr
            )
        } returns Optional.empty()

        every {
            pdlService.finnFolkeregisterident("11111111111")
        } returns Optional.of("12345678911")

        opprettBetalingsplan.utfør(prosessinstans)

        verify(exactly = 1) {
            faktureringskomponentenConsumer.lagFakturaSerie(any())
        }
    }

    @Test
    fun `Opprett betalingsplan uten aktoer feiler`() {
        lagTestData(lagFagsak().apply {
            aktører = setOf(lagAktoerOrg(Aktoersroller.REPRESENTANT, "123456789"))
        })

        every {
            behandlingService.hentBehandling(behandlingsId)
        } returns behandling


        shouldThrow<FunksjonellException> {
            opprettBetalingsplan.utfør(prosessinstans)
        }.message.shouldContain("Finner ikke bruker på fagsak MEL-100")
    }

    @Test
    fun `Opprett betalingsplan med to BRUKER aktoer feiler`() {
        lagTestData(lagFagsak().apply {
            aktører = setOf(
                lagAktoerOrg(Aktoersroller.BRUKER, "123456789"),
                lagAktoerOrg(Aktoersroller.BRUKER, "123456781")
            )
        })

        every {
            behandlingService.hentBehandling(behandlingsId)
        } returns behandling


        shouldThrow<TekniskException> {
            opprettBetalingsplan.utfør(prosessinstans)
        }.message.shouldContain("Det finnes mer enn en aktør med rollen Bruker for sak MEL-100")
    }

    private fun lagBehandling(fagsak: Fagsak = lagFagsak()): Behandling {
        val behandling = Behandling()
        behandling.id = behandlingsId
        behandling.tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
        behandling.type = Behandlingstyper.FØRSTEGANG
        behandling.status = Behandlingsstatus.AVSLUTTET
        behandling.fagsak = fagsak
        return behandling
    }

    private fun lagFagsak(): Fagsak = Fagsak().apply {
        saksnummer = "MEL-100"
        type = Sakstyper.EU_EOS
        tema = Sakstemaer.MEDLEMSKAP_LOVVALG
        status = Saksstatuser.OPPRETTET
    }

    fun lagBehandlingsresultat(): Behandlingsresultat {
        val behandlingsresultat = Behandlingsresultat()
        behandlingsresultat.id = 1L
        behandlingsresultat.type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
        val vedtakMetadata = VedtakMetadata()
        vedtakMetadata.vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK
        vedtakMetadata.vedtaksdato = Instant.now().minus(3, ChronoUnit.DAYS)
        behandlingsresultat.vedtakMetadata = vedtakMetadata
        behandlingsresultat.medlemAvFolketrygden = lagMedlemAvFolketrygden()
        return behandlingsresultat
    }

    private fun lagMedlemAvFolketrygden(): MedlemAvFolketrygden {
        return MedlemAvFolketrygden().apply {
            medlemskapsperioder = lagMedlemskapsperioder()
            fastsattTrygdeavgift = lagFastsattTrygdeavgift()
            bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
        }
    }

    private fun lagMedlemskapsperioder(): List<Medlemskapsperiode> {
        return listOf(Medlemskapsperiode().apply {
            trygdedekning = Trygdedekninger.HELSE_OG_PENSJONSDEL_MED_SYKE_OG_FORELDREPENGER
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            medlemskapstype = Medlemskapstyper.FRIVILLIG
            fom = LocalDate.of(2022, 1, 1)
            tom = LocalDate.of(2023, 5, 31)
        })
    }

    private fun lagFastsattTrygdeavgift(): FastsattTrygdeavgift {
        return FastsattTrygdeavgift().apply {
            avgiftspliktigNorskInntektMnd = 50000L
            avgiftspliktigUtenlandskInntektMnd = 50000L
            betalesAv = lagBetalesAv()
            representantNr = "1234"
            trygdeavgift = setOf(lagTrygdeavgift(this))
            trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag().apply {
                inntektsperioder = setOf(lagInntektsperiode())
            }
        }
    }

    private fun lagTrygdeavgift(fastsattTrygdeavgift: FastsattTrygdeavgift): Trygdeavgiftsperiode {
        return Trygdeavgiftsperiode().apply {
            periodeFra = LocalDate.of(2023, 1, 1)
            periodeTil = LocalDate.of(2023, 5, 1)
            trygdeavgiftsbeløpMd = Penger(5000.0)
            trygdesats = BigDecimal(3.5)
            this.fastsattTrygdeavgift = fastsattTrygdeavgift
        }
    }

    private fun lagInntektsperiode(): Inntektsperiode {
        return Inntektsperiode().apply {
            fomDato = LocalDate.of(2023, 1, 1)
            tomDato = LocalDate.of(2023, 5, 1)
            avgiftspliktigInntektMnd = Penger(5000.0)
        }
    }

    fun lagKontaktOpplysning(): Kontaktopplysning {
        val kontaktopplysning = Kontaktopplysning()
        kontaktopplysning.kontaktNavn = "Donald Duck"
        return kontaktopplysning
    }


    private fun lagBetalesAv(): Aktoer {
        val aktoer = Aktoer()
        aktoer.rolle = Aktoersroller.BRUKER
        return aktoer
    }

    private fun lagAktoerOrg(aktoersroller: Aktoersroller, orgNummer: String): Aktoer {
        val aktoer = Aktoer()
        aktoer.rolle = aktoersroller
        aktoer.orgnr = orgNummer
        return aktoer
    }

    private fun lagAktoerPerson(aktørId: String): Aktoer {
        val aktoer = Aktoer()
        aktoer.rolle = Aktoersroller.BRUKER
        aktoer.aktørId = aktørId
        return aktoer
    }

    companion object {
        const val behandlingsId = 1L
    }
}
