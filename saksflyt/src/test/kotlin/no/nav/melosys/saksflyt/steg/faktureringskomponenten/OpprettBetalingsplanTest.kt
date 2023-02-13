package no.nav.melosys.saksflyt.steg.faktureringskomponenten;

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.finn.unleash.FakeUnleash
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.Trygdeavgift
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

    companion object {
        val behandlingsId = 1L
    }

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

    @Test
    fun `Opprett betalingsplan med riktige verdier`() {
        val behandling = lagBehandling().apply {
            fagsak.aktører = setOf(
                lagAktoerOrg(Aktoersroller.REPRESENTANT, "123456789"),
                lagAktoerPerson(Aktoersroller.BRUKER, "11111111111")
            )
        }
        val prosessinstans = Prosessinstans()
        prosessinstans.setData(ProsessDataKey.BETALINGSINTERVALL, FaktureringsIntervall.KVARTAL)
        prosessinstans.behandling = behandling
        val behandlingsresultat = lagBehandlingsresultat()
        val fastsattTrygdeavgift = behandlingsresultat.medlemAvFolketrygden.fastsattTrygdeavgift
        val fagsak = prosessinstans.behandling.fagsak

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

        verify {
            faktureringskomponentenConsumer.lagFakturaSerie(any())
        }
    }

    @Test
    fun `Opprett betalingsplan uten aktoer feiler`() {
        val behandling = lagBehandling().apply {
            fagsak.apply {
                aktører = setOf(lagAktoerOrg(Aktoersroller.REPRESENTANT, "123456789"))
            }
        }
        val prosessinstans = Prosessinstans()
        prosessinstans.setData(ProsessDataKey.BETALINGSINTERVALL, FaktureringsIntervall.KVARTAL)
        prosessinstans.behandling = behandling

        every {
            behandlingService.hentBehandling(behandlingsId)
        } returns behandling


        shouldThrow<FunksjonellException> {
            opprettBetalingsplan.utfør(prosessinstans)
        }.message.shouldContain("Kunne ikke opprette betalingsplan, det finnes 0 aktører")
    }

    @Test
    fun `Opprett betalingsplan med to BRUKER aktoer feiler`() {
        val behandling = lagBehandling().apply {
            fagsak.apply {
                aktører = setOf(
                    lagAktoerOrg(Aktoersroller.BRUKER, "123456789"),
                    lagAktoerOrg(Aktoersroller.BRUKER, "123456781")
                )
            }
        }
        val prosessinstans = Prosessinstans()
        prosessinstans.setData(ProsessDataKey.BETALINGSINTERVALL, FaktureringsIntervall.KVARTAL)
        prosessinstans.behandling = behandling

        every {
            behandlingService.hentBehandling(behandlingsId)
        } returns behandling


        shouldThrow<FunksjonellException> {
            opprettBetalingsplan.utfør(prosessinstans)
        }.message.shouldContain("Kunne ikke opprette betalingsplan, det finnes 2 aktører med rolle BRUKER")
    }

    private fun lagBehandling(): Behandling {
        val behandling = Behandling()
        behandling.id = behandlingsId
        behandling.tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
        behandling.type = Behandlingstyper.FØRSTEGANG
        behandling.status = Behandlingsstatus.AVSLUTTET
        behandling.fagsak = lagFagsak()
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
        val medlemAvFolketrygden = MedlemAvFolketrygden()
        medlemAvFolketrygden.medlemskapsperioder = lagMedlemskapsperioder()
        medlemAvFolketrygden.fastsattTrygdeavgift = lagFastsattTrygdeavgift()
        return medlemAvFolketrygden
    }


    private fun lagMedlemskapsperioder(): List<Medlemskapsperiode>? {
        val periode1 = Medlemskapsperiode()

        periode1.bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
        periode1.innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        periode1.medlemskapstype = Medlemskapstyper.FRIVILLIG
        periode1.fom = LocalDate.of(2022, 1, 1)
        periode1.tom = LocalDate.of(2023, 5, 31)
        val trygdeAvgift = lagTrygdeAvgift(periode1)
        periode1.trygdeavgift = listOf(trygdeAvgift)
        periode1.setTrygdedekning(Trygdedekninger.HELSE_OG_PENSJONSDEL_MED_SYKE_OG_FORELDREPENGER)
        return java.util.List.of(periode1)
    }

    private fun lagTrygdeAvgift(medlemskapsperiode: Medlemskapsperiode): Trygdeavgift {
        val trygdeavgift = Trygdeavgift(
            medlemskapsperiode,
            BigDecimal(5000),
            BigDecimal(3.5),
            "Trygda",
            true,
            LocalDate.of(2023, 1, 1),
            LocalDate.of(2023, 5, 1)
        )
        return trygdeavgift
    }

    private fun lagFastsattTrygdeavgift(): FastsattTrygdeavgift? {
        val fastsattTrygdeavgift = FastsattTrygdeavgift()
        fastsattTrygdeavgift.avgiftspliktigNorskInntektMnd = 50000L
        fastsattTrygdeavgift.avgiftspliktigUtenlandskInntektMnd = 50000L
        fastsattTrygdeavgift.betalesAv = lagBetalesAv()
        fastsattTrygdeavgift.representantNr = "1234"
        return fastsattTrygdeavgift
    }

    fun lagKontaktOpplysning(): Kontaktopplysning {
        val kontaktopplysning = Kontaktopplysning()
        kontaktopplysning.kontaktNavn = "Donald Duck"
        return kontaktopplysning
    }


    private fun lagBetalesAv(): Aktoer? {
        val aktoer = Aktoer()
        aktoer.rolle = Aktoersroller.REPRESENTANT_TRYGDEAVGIFT
        return aktoer
    }


    private fun lagAktoerOrg(aktoersroller: Aktoersroller, orgNummer: String): Aktoer? {
        val aktoer = Aktoer()
        aktoer.rolle = aktoersroller
        aktoer.orgnr = orgNummer
        return aktoer
    }

    private fun lagAktoerPerson(aktoersroller: Aktoersroller, aktørId: String): Aktoer? {
        val aktoer = Aktoer()
        aktoer.rolle = aktoersroller
        aktoer.aktørId = aktørId
        return aktoer
    }

}
