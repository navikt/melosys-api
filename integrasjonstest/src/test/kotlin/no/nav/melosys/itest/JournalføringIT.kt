package no.nav.melosys.itest

import no.nav.melosys.domain.FellesKodeverk
import no.nav.melosys.domain.arkiv.ArkivDokument
import no.nav.melosys.domain.kodeverk.Avsendertyper
import no.nav.melosys.domain.saksflyt.ProsessStatus
import no.nav.melosys.domain.saksflyt.ProsessType
import no.nav.melosys.integrasjon.kodeverk.Kode
import no.nav.melosys.integrasjon.kodeverk.KodeOppslag
import no.nav.melosys.integrasjon.kodeverk.Kodeverk
import no.nav.melosys.integrasjon.kodeverk.KodeverkRegister
import no.nav.melosys.melosysmock.oppgave.Oppgave
import no.nav.melosys.melosysmock.testdata.TestDataGenerator
import no.nav.melosys.repository.ProsessinstansRepository
import no.nav.melosys.service.felles.dto.SoeknadslandDto
import no.nav.melosys.service.journalforing.JournalfoeringService
import no.nav.melosys.service.journalforing.dto.DokumentDto
import no.nav.melosys.service.journalforing.dto.FagsakDto
import no.nav.melosys.service.journalforing.dto.JournalfoeringOpprettDto
import no.nav.melosys.service.journalforing.dto.PeriodeDto
import no.nav.melosys.service.kodeverk.KodeverkService
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.jvm.optionals.getOrNull

@Import(JournalføringIT.TestConfig::class)
class JournalføringIT(
    @Autowired private val testDataGenerator: TestDataGenerator,
    @Autowired private val journalføringService: JournalfoeringService,
    @Autowired private val oppgaveService: OppgaveService,
    @Autowired private val prosessinstansRepository: ProsessinstansRepository,
) : ComponentTestBase() {

    @Test
    fun test() {
        val jfrOppgave: Oppgave = lagJfrOppgave()
        val now = LocalDateTime.now()
        val journalfoeringOpprettDto = lagJournalfoeringOpprettDto(jfrOppgave)

        ThreadLocalAccessInfo.executeProcess("journalførOgOpprettSak") {
            journalføringService.journalførOgOpprettSak(journalfoeringOpprettDto)
            oppgaveService.ferdigstillOppgave(journalfoeringOpprettDto.oppgaveID)
        }

        listOf(
            finnProssesID(ProsessType.JFR_NY_SAK_BRUKER, now),
            finnProssesID(ProsessType.OPPRETT_OG_DISTRIBUER_BREV, now)
        ).forEach {
            sjekkAtProssessHarStatusFerdig(it)
        }
    }

    private fun lagJournalfoeringOpprettDto(jfrOppgave: Oppgave): JournalfoeringOpprettDto {
        var journalføringDto: JournalfoeringOpprettDto? = null
        ThreadLocalAccessInfo.executeProcess("journalførOgOpprettSak") {
            val hentJournalpost = journalføringService.hentJournalpost(jfrOppgave.journalpostId)
            journalføringDto = createJournalføringDto(jfrOppgave.journalpostId, hentJournalpost.hoveddokument)
        }
        return journalføringDto!!
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun sjekkAtProssessHarStatusFerdig(idJornalførProsess: UUID) =
        await.until {
            prosessinstansRepository.findById(idJornalførProsess)
                .getOrNull()?.status == ProsessStatus.FERDIG
        }

    private fun finnProssesID(prosessType: ProsessType, now: LocalDateTime): UUID =
        await.untilNotNull {
            prosessinstansRepository.findAll()
                .find { it.registrertDato > now && it.type == prosessType }?.id
        }

    private fun lagJfrOppgave(): Oppgave =
        testDataGenerator.opprettJfrOppgave(tilordnetRessurs = "Z123456", forVirksomhet = false)

    private fun createJournalføringDto(journalpostID: String?, dokument: ArkivDokument) =
        JournalfoeringOpprettDto().apply {
            this.journalpostID = journalpostID
            avsenderID = "30056928150"
            avsenderNavn = "KARAFFEL TRIVIELL"
            brukerID = "30056928150"
            virksomhetOrgnr = null
            oppgaveID = "2"
            vedlegg = emptyList()
            mottattDato = LocalDate.now()
            arbeidsgiverID = null
            behandlingstemaKode = "UTSENDT_ARBEIDSTAKER"
            representantID = null
            representantKontaktPerson = null
            representererKode = null
            isIkkeSendForvaltingsmelding = false
            avsenderType = Avsendertyper.PERSON
            isSkalTilordnes = true
            fagsak = FagsakDto().apply {
                sakstype = "EU_EOS"
                soknadsperiode = PeriodeDto(
                    LocalDate.of(2001, 1, 1),
                    LocalDate.of(2001, 1, 2),
                )
                land = SoeknadslandDto(
                    listOf("IE"), false
                )
            }
            hoveddokument = DokumentDto(dokument.dokumentId, dokument.tittel).apply {
                logiskeVedlegg = emptyList()
            }
        }

    @TestConfiguration
    class TestConfig {
        @Bean
        @Primary
        fun kodeverkRegisterStub(): KodeverkRegister? = KodeverkRegister {
            Kodeverk(
                "DUMMY", mapOf(
                    Pair(
                        "DUMMY",
                        listOf(Kode("DUMMY", "DUMMY", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1)))
                    )
                )
            )
        }

        @Bean
        @Primary
        fun kodeOppslagStub(): KodeOppslag? {
            open class KodeOppslagImpl : KodeOppslag {
                override fun getTermFraKodeverk(kodeverk: FellesKodeverk, kode: String): String = "DUMMY"

                override fun getTermFraKodeverk(kodeverk: FellesKodeverk, kode: String, dato: LocalDate): String =
                    "DUMMY"

                override fun getTermFraKodeverk(
                    kodeverk: FellesKodeverk,
                    kode: String,
                    dato: LocalDate,
                    kodeperioder: List<Kode>?
                ): String = "DUMMY"
            }

            return KodeOppslagImpl()
        }

        @Bean
        @Primary
        fun kodeverkServiceStub(kodeverkRegister: KodeverkRegister?, kodeOppslag: KodeOppslag?): KodeverkService? {
            return KodeverkService(kodeverkRegister, kodeOppslag)
        }
    }
}
