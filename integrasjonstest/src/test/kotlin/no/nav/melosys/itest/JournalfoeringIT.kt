package no.nav.melosys.itest

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equality.FieldsEqualityCheckConfig
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.melosys.domain.Fagsystem
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.mottatteopplysninger.Soeknad
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.melosysmock.journalpost.JournalpostRepo
import no.nav.melosys.repository.BehandlingRepository
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.saksflytapi.domain.ProsessType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JournalfoeringIT(
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val behandlingsresultatRepository: BehandlingsresultatRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val journalpostRepo: JournalpostRepo
) : JournalfoeringBase() {

    @AfterEach
    fun afterEach() {
        journalpostRepo.repo.clear()
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
            mapOf(
                ProsessType.JFR_NY_SAK_BRUKER to 1,
                ProsessType.OPPRETT_OG_DISTRIBUER_BREV to 1
            )
        )


        val behandling = journalføringProsess.behandling.shouldNotBeNull()
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
        behandling.mottatteOpplysninger!!.mottatteOpplysningerData.shouldBeInstanceOf<Soeknad>()
            .shouldBeEqualToComparingFields(Soeknad().apply {
                soeknadsland.apply {
                    landkoder = listOf(Landkoder.IE.kode)
                    isFlereLandUkjentHvilke = false
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
            mapOf(
                ProsessType.JFR_NY_SAK_BRUKER to 1,
                ProsessType.OPPRETT_OG_DISTRIBUER_BREV to 1
            )

        )
        val behandling = prosessinstans.behandling.shouldNotBeNull()

        behandling.status = Behandlingsstatus.AVSLUTTET
        behandlingRepository.save(behandling)

        val journalfoeringTilordneDto = lagJournalfoeringOppgaveOgTilordneDto(
            saksnummer = behandling.fagsak.saksnummer,
            journalfoeringTilordneDto = defaultJournalfoeringTilordneDto().apply {
                behandlingstemaKode = Behandlingstema.UTSENDT_ARBEIDSTAKER.kode
                behandlingstypeKode = Behandlingstyper.NY_VURDERING.kode
            }
        )


        executeAndWait(
            mapOf(
                ProsessType.OPPRETT_OG_DISTRIBUER_BREV to 1,
                ProsessType.JFR_ANDREGANG_REPLIKER_BEHANDLING to 1
            )
        ) {
            journalføringService.journalførOgOpprettAndregangsBehandling(journalfoeringTilordneDto)
        }


        val fagsak = fagsakRepository.findBySaksnummer(behandling.fagsak.saksnummer).get()
        fagsak.behandlinger
            .shouldHaveSize(2)
            .maxBy { it.id }
            .apply {
                type.shouldBe(Behandlingstyper.NY_VURDERING)
                opprinneligBehandling
                    .shouldNotBeNull()
                    .id shouldBe behandling.id
                initierendeJournalpostId.shouldBe(journalfoeringOpprettDto.journalpostID)
            }
            .mottatteOpplysninger!!.mottatteOpplysningerData.shouldBeInstanceOf<Soeknad>()
            .shouldBeEqualToComparingFields(Soeknad().apply {
                soeknadsland.apply {
                    landkoder = listOf(Landkoder.IE.kode)
                    isFlereLandUkjentHvilke = false
                }
                periode = Periode(
                    periodeFOM,
                    periodeTOM
                )
            }, FieldsEqualityCheckConfig(ignorePrivateFields = false))
    }

    @Test
    fun journalførOgKnyttTilEksisterendeSak() {
        val journalfoeringOpprettDto = defaultJournalføringDto().apply {
            fagsak.sakstype = Sakstyper.EU_EOS.kode
            fagsak.sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG.kode
            behandlingstypeKode = Behandlingstyper.FØRSTEGANG.kode
            behandlingstemaKode = Behandlingstema.UTSENDT_ARBEIDSTAKER.kode
        }

        val prosessinstans = journalførOgVentTilProsesserErFerdige(
            journalfoeringOpprettDto,
            mapOf(
                ProsessType.JFR_NY_SAK_BRUKER to 1,
                ProsessType.OPPRETT_OG_DISTRIBUER_BREV to 1
            )
        )
        val behandling = prosessinstans.behandling.shouldNotBeNull()


        val eksisterendeJournalpostIds = journalpostRepo.repo.values.map { it.journalpostId }

        val journalfoeringTilordneDto = lagJournalfoeringOppgaveOgTilordneDto(
            saksnummer = behandling.fagsak.saksnummer,
            journalfoeringTilordneDto = defaultJournalfoeringTilordneDto().apply {
                behandlingstemaKode = Behandlingstema.UTSENDT_ARBEIDSTAKER.kode
                behandlingstypeKode = Behandlingstyper.NY_VURDERING.kode
            }
        )


        executeAndWait(
            mapOf(
                ProsessType.JFR_KNYTT to 1,
                ProsessType.OPPRETT_OG_DISTRIBUER_BREV to 1
            )
        ) {
            journalføringService.journalførOgKnyttTilEksisterendeSak(journalfoeringTilordneDto)
        }

        val fagsak = fagsakRepository.findBySaksnummer(behandling.fagsak.saksnummer).get()
        fagsak.behandlinger
            .single()
            .apply {
                status.shouldBe(Behandlingsstatus.VURDER_DOKUMENT)
                type.shouldBe(Behandlingstyper.FØRSTEGANG)
            }

        val tilKnyttetJournalpost = journalpostRepo.repo.values.filterNot { it.journalpostId in eksisterendeJournalpostIds }

        tilKnyttetJournalpost
            .shouldHaveSize(2)
            .onEach {
                it.avsenderMottaker.navn.shouldNotBeNull()
                it.sakId.shouldBe(fagsak.saksnummer)
            }
        tilKnyttetJournalpost.any {
            it.tittel == "Tittel til dokument"
        }
        tilKnyttetJournalpost.any {
            it.tittel == "Melding om forventet saksbehandlingstid"
        }
    }

    @Test
    fun journalførOgOpprettAndregangsBehandling_fraAvslåttFlyt_flytMedPeriodeOgLand() {
        val journalfoeringOpprettDto = defaultJournalføringDto().apply {
            fagsak.sakstype = Sakstyper.EU_EOS.kode
            fagsak.sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG.kode
            behandlingstypeKode = Behandlingstyper.FØRSTEGANG.kode
            behandlingstemaKode = Behandlingstema.UTSENDT_ARBEIDSTAKER.kode
            forvaltningsmeldingMottaker = ForvaltningsmeldingMottaker.BRUKER
        }
        val prosessinstans = journalførOgVentTilProsesserErFerdige(
            journalfoeringOpprettDto,
            mapOf(
                ProsessType.JFR_NY_SAK_BRUKER to 1,
                ProsessType.OPPRETT_OG_DISTRIBUER_BREV to 1
            )
        )
        val behandling = prosessinstans.behandling.shouldNotBeNull()

        behandling.status = Behandlingsstatus.AVSLUTTET
        behandlingRepository.save(behandling)

        val behandlingsresultat = behandlingsresultatRepository.findById(behandling.id).get()
        behandlingsresultat.type = Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL
        behandlingsresultatRepository.save(behandlingsresultat)

        val journalfoeringTilordneDto = lagJournalfoeringOppgaveOgTilordneDto(
            saksnummer = behandling.fagsak.saksnummer,
            journalfoeringTilordneDto = defaultJournalfoeringTilordneDto().apply {
                behandlingstemaKode = Behandlingstema.UTSENDT_ARBEIDSTAKER.kode
                behandlingstypeKode = Behandlingstyper.NY_VURDERING.kode
            }
        )


        executeAndWait(
            mapOf(
                ProsessType.JFR_ANDREGANG_NY_BEHANDLING to 1,
                ProsessType.OPPRETT_OG_DISTRIBUER_BREV to 1
            )
        ) {
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
            .mottatteOpplysninger!!.mottatteOpplysningerData.shouldBeInstanceOf<Soeknad>()
            .shouldBeEqualToComparingFields(Soeknad().apply {
                soeknadsland.apply {
                    landkoder = listOf()
                    isFlereLandUkjentHvilke = false
                }
                periode = Periode()
            }, FieldsEqualityCheckConfig(ignorePrivateFields = false))
    }

    @Test
    fun journalførOgOpprettAndregangsBehandling_fraIngenFlyt_flytMedPeriodeOgLand() {
        fakeUnleash.disableAll()

        val journalfoeringOpprettDto = defaultJournalføringDto().apply {
            fagsak.sakstype = Sakstyper.EU_EOS.kode
            fagsak.sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG.kode
            behandlingstypeKode = Behandlingstyper.FØRSTEGANG.kode
            behandlingstemaKode = Behandlingstema.PENSJONIST.kode
            forvaltningsmeldingMottaker = ForvaltningsmeldingMottaker.INGEN
        }
        val prosessinstans = journalførOgVentTilProsesserErFerdige(
            journalfoeringOpprettDto,
            mapOf(
                ProsessType.JFR_NY_SAK_BRUKER to 1
            )
        )
        val behandling = prosessinstans.behandling.shouldNotBeNull()
        behandling.mottatteOpplysninger.shouldBeNull()

        behandling.status = Behandlingsstatus.AVSLUTTET
        behandlingRepository.save(behandling)

        val behandlingsresultat = behandlingsresultatRepository.findById(behandling.id).get()
        behandlingsresultat.type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
        behandlingsresultatRepository.save(behandlingsresultat)

        val journalfoeringTilordneDto = lagJournalfoeringOppgaveOgTilordneDto(
            saksnummer = behandling.fagsak.saksnummer,
            journalfoeringTilordneDto = defaultJournalfoeringTilordneDto().apply {
                behandlingstemaKode = Behandlingstema.UTSENDT_ARBEIDSTAKER.kode
                behandlingstypeKode = Behandlingstyper.NY_VURDERING.kode
            }
        )


        executeAndWait(
            mapOf(
                ProsessType.JFR_ANDREGANG_NY_BEHANDLING to 1,
                ProsessType.OPPRETT_OG_DISTRIBUER_BREV to 1
            )
        ) {
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
            .mottatteOpplysninger!!.mottatteOpplysningerData.shouldBeInstanceOf<Soeknad>()
            .shouldBeEqualToComparingFields(Soeknad().apply {
                soeknadsland.apply {
                    landkoder = listOf()
                    isFlereLandUkjentHvilke = false
                }
                periode = Periode()
            }, FieldsEqualityCheckConfig(ignorePrivateFields = false))
    }
}
