package no.nav.melosys.itest

import io.getunleash.FakeUnleash
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equality.FieldsEqualityCheckConfig
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.melosys.domain.Fagsystem
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.mottatteopplysninger.Soeknad
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.saksflyt.ProsessType
import no.nav.melosys.melosysmock.testdata.TestDataGenerator
import no.nav.melosys.repository.BehandlingRepository
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.repository.ProsessinstansRepository
import no.nav.melosys.service.journalforing.JournalfoeringService
import no.nav.melosys.service.oppgave.OppgaveService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import

@Import(OAuthMockServer::class)
class JournalfoeringIT(
    @Autowired testDataGenerator: TestDataGenerator,
    @Autowired journalføringService: JournalfoeringService,
    @Autowired oppgaveService: OppgaveService,
    @Autowired prosessinstansRepository: ProsessinstansRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val behandlingsresultatRepository: BehandlingsresultatRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val unleash: FakeUnleash,
    @Autowired private val oAuthMockServer: OAuthMockServer
) : JournalfoeringBase(testDataGenerator, journalføringService, oppgaveService, prosessinstansRepository) {

    @BeforeEach
    fun setup() {
        oAuthMockServer.start()
        unleash.enableAll()
    }

    @AfterEach
    fun afterEach() {
        oAuthMockServer.stop()
    }

    @Test
    fun journalførOgOpprettSak_EU_EOS_prosesserKjørerAlleSteg() {
        val journalfoeringOpprettDto = defaultJournalføringDto().apply {
            fagsak.sakstype = Sakstyper.EU_EOS.kode
            fagsak.sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG.kode
            behandlingstypeKode = Behandlingstyper.FØRSTEGANG.kode
            behandlingstemaKode = Behandlingstema.UTSENDT_ARBEIDSTAKER.kode
        }


        val journalføringProsess = journalførOgVentTilProsesserErFerdige(
            journalfoeringOpprettDto,
            waitFor = ProsessType.JFR_NY_SAK_BRUKER,
            alsoWaitForprosessType = listOf(ProsessType.OPPRETT_OG_DISTRIBUER_BREV)
        )


        val behandling = journalføringProsess.behandling
        behandling.apply {
            status.shouldBe(Behandlingsstatus.OPPRETTET)
            type.shouldBe(Behandlingstyper.FØRSTEGANG)
            tema.shouldBe(Behandlingstema.UTSENDT_ARBEIDSTAKER)
        }
        behandling.fagsak.apply {
            type.shouldBe(Sakstyper.EU_EOS)
            status.shouldBe(Saksstatuser.OPPRETTET)
            registrertAv.shouldBe(Fagsystem.MELOSYS.toString())
            tema.shouldBe(Sakstemaer.MEDLEMSKAP_LOVVALG)
        }
        behandling.mottatteOpplysninger.mottatteOpplysningerData.shouldBeInstanceOf<Soeknad>()
            .shouldBeEqualToComparingFields(Soeknad().apply {
                soeknadsland.apply {
                    landkoder = listOf(Landkoder.IE.kode)
                    erUkjenteEllerAlleEosLand = false
                }
                periode = Periode(
                    periodeFOM,
                    periodeTOM
                )
            }, FieldsEqualityCheckConfig(ignorePrivateFields = false))
    }

    @Test
    fun journalførOgOpprettAndregangsBehandling_replikerBehandling_replikerBehandlingProsessStegBlirKjørt() {
        val journalfoeringOpprettDto = defaultJournalføringDto().apply {
            fagsak.sakstype = Sakstyper.EU_EOS.kode
            fagsak.sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG.kode
            behandlingstypeKode = Behandlingstyper.FØRSTEGANG.kode
            behandlingstemaKode = Behandlingstema.UTSENDT_ARBEIDSTAKER.kode
        }

        val prosessinstans = journalførOgVentTilProsesserErFerdige(
            journalfoeringOpprettDto,
            waitFor = ProsessType.JFR_NY_SAK_BRUKER,
            alsoWaitForprosessType = listOf(ProsessType.OPPRETT_OG_DISTRIBUER_BREV)
        )
        val behandling = prosessinstans.behandling

        behandling.status = Behandlingsstatus.AVSLUTTET
        behandlingRepository.save(behandling)

        val journalfoeringTilordneDto = lagJournalfoeringTilordneDto(
            saksnummer = behandling.fagsak.saksnummer,
            journalfoeringTilordneDto = defaultJournalfoeringTilordneDto().apply {
                behandlingstemaKode = Behandlingstema.UTSENDT_ARBEIDSTAKER.kode
                behandlingstypeKode = Behandlingstyper.NY_VURDERING.kode
            }
        )


        executeAndWait(ProsessType.JFR_ANDREGANG_REPLIKER_BEHANDLING) {
            journalføringService.journalførOgOpprettAndregangsBehandling(journalfoeringTilordneDto)
        }


        val fagsak = fagsakRepository.findBySaksnummer(behandling.fagsak.saksnummer).get()
        fagsak.behandlinger
            .shouldHaveSize(2)
            .maxBy { it.id }
            .apply {
                type.shouldBe(Behandlingstyper.NY_VURDERING)
                opprinneligBehandling.id.shouldBe(behandling.id)
                initierendeJournalpostId.shouldBe(journalfoeringOpprettDto.journalpostID)
            }
            .mottatteOpplysninger.mottatteOpplysningerData.shouldBeInstanceOf<Soeknad>()
            .shouldBeEqualToComparingFields(Soeknad().apply {
                soeknadsland.apply {
                    landkoder = listOf(Landkoder.IE.kode)
                    erUkjenteEllerAlleEosLand = false
                }
                periode = Periode(
                    periodeFOM,
                    periodeTOM
                )
            }, FieldsEqualityCheckConfig(ignorePrivateFields = false))
    }

    @Test
    fun journalførOgOpprettAndregangsBehandling_fraAvslåttFlyt_flytMedPeriodeOgLand() {
        val journalfoeringOpprettDto = defaultJournalføringDto().apply {
            fagsak.sakstype = Sakstyper.EU_EOS.kode
            fagsak.sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG.kode
            behandlingstypeKode = Behandlingstyper.FØRSTEGANG.kode
            behandlingstemaKode = Behandlingstema.UTSENDT_ARBEIDSTAKER.kode
            isIkkeSendForvaltingsmelding = false
        }
        val prosessinstans = journalførOgVentTilProsesserErFerdige(
            journalfoeringOpprettDto,
            waitFor = ProsessType.JFR_NY_SAK_BRUKER,
        )
        val behandling = prosessinstans.behandling

        behandling.status = Behandlingsstatus.AVSLUTTET
        behandlingRepository.save(behandling)

        val behandlingsresultat = behandlingsresultatRepository.findById(behandling.id).get()
        behandlingsresultat.type = Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL
        behandlingsresultatRepository.save(behandlingsresultat)

        val journalfoeringTilordneDto = lagJournalfoeringTilordneDto(
            saksnummer = behandling.fagsak.saksnummer,
            journalfoeringTilordneDto = defaultJournalfoeringTilordneDto().apply {
                behandlingstemaKode = Behandlingstema.UTSENDT_ARBEIDSTAKER.kode
                behandlingstypeKode = Behandlingstyper.NY_VURDERING.kode
            }
        )


        executeAndWait(ProsessType.JFR_ANDREGANG_NY_BEHANDLING) {
            journalføringService.journalførOgOpprettAndregangsBehandling(journalfoeringTilordneDto)
        }


        val fagsak = fagsakRepository.findBySaksnummer(behandling.fagsak.saksnummer).get()
        fagsak.behandlinger
            .shouldHaveSize(2)
            .maxBy { it.id }
            .apply {
                type.shouldBe(Behandlingstyper.NY_VURDERING)
                opprinneligBehandling.shouldBeNull()
                initierendeJournalpostId.shouldBe(journalfoeringTilordneDto.journalpostID)
            }
            .mottatteOpplysninger.mottatteOpplysningerData.shouldBeInstanceOf<Soeknad>()
            .shouldBeEqualToComparingFields(Soeknad().apply {
                soeknadsland.apply {
                    landkoder = listOf()
                    erUkjenteEllerAlleEosLand = false
                }
                periode = Periode()
            }, FieldsEqualityCheckConfig(ignorePrivateFields = false))
    }

    @Test
    fun journalførOgOpprettAndregangsBehandling_fraIngenFlyt_flytMedPeriodeOgLand() {
        val journalfoeringOpprettDto = defaultJournalføringDto().apply {
            fagsak.sakstype = Sakstyper.EU_EOS.kode
            fagsak.sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG.kode
            behandlingstypeKode = Behandlingstyper.FØRSTEGANG.kode
            behandlingstemaKode = Behandlingstema.ARBEID_KUN_NORGE.kode
            isIkkeSendForvaltingsmelding = true
        }
        val prosessinstans = journalførOgVentTilProsesserErFerdige(
            journalfoeringOpprettDto,
            waitFor = ProsessType.JFR_NY_SAK_BRUKER,
        )
        val behandling = prosessinstans.behandling
        behandling.mottatteOpplysninger.shouldBeNull()

        behandling.status = Behandlingsstatus.AVSLUTTET
        behandlingRepository.save(behandling)

        val behandlingsresultat = behandlingsresultatRepository.findById(behandling.id).get()
        behandlingsresultat.type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
        behandlingsresultatRepository.save(behandlingsresultat)

        val journalfoeringTilordneDto = lagJournalfoeringTilordneDto(
            saksnummer = behandling.fagsak.saksnummer,
            journalfoeringTilordneDto = defaultJournalfoeringTilordneDto().apply {
                behandlingstemaKode = Behandlingstema.UTSENDT_ARBEIDSTAKER.kode
                behandlingstypeKode = Behandlingstyper.NY_VURDERING.kode
            }
        )


        executeAndWait(ProsessType.JFR_ANDREGANG_NY_BEHANDLING) {
            journalføringService.journalførOgOpprettAndregangsBehandling(journalfoeringTilordneDto)
        }


        val fagsak = fagsakRepository.findBySaksnummer(behandling.fagsak.saksnummer).get()
        fagsak.behandlinger
            .shouldHaveSize(2)
            .maxBy { it.id }
            .apply {
                type.shouldBe(Behandlingstyper.NY_VURDERING)
                opprinneligBehandling.shouldBeNull()
                initierendeJournalpostId.shouldBe(journalfoeringTilordneDto.journalpostID)
            }
            .mottatteOpplysninger.mottatteOpplysningerData.shouldBeInstanceOf<Soeknad>()
            .shouldBeEqualToComparingFields(Soeknad().apply {
                soeknadsland.apply {
                    landkoder = listOf()
                    erUkjenteEllerAlleEosLand = false
                }
                periode = Periode()
            }, FieldsEqualityCheckConfig(ignorePrivateFields = false))
    }
}
