package no.nav.melosys.itest

import no.finn.unleash.Unleash
import no.nav.melosys.domain.arkiv.*
import no.nav.melosys.domain.eessi.BucType
import no.nav.melosys.domain.eessi.Periode
import no.nav.melosys.domain.eessi.SedType
import no.nav.melosys.domain.eessi.melding.Avsender
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.saksflyt.ProsessStatus
import no.nav.melosys.domain.saksflyt.Prosessinstans
import no.nav.melosys.integrasjon.joark.JoarkFasade
import no.nav.melosys.repository.ProsessinstansRepository
import org.assertj.core.api.Assertions
import org.awaitility.Awaitility
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.kafka.core.KafkaTemplate
import java.time.Duration
import java.time.LocalDate
import java.util.*
import java.util.List
import java.util.stream.Collectors

internal class SedMottakTestIT : ComponentTestBase() {
    @Autowired
    @Qualifier("system")
    private val joarkFasade: JoarkFasade? = null

    @Autowired
    @Qualifier("melosysEessiMelding")
    private val melosysEessiMeldingKafkaTemplate: KafkaTemplate<String, MelosysEessiMelding>? = null

    @Autowired
    private val prosessinstansRepository: ProsessinstansRepository? = null

    lateinit var rinaSaksnummer: String
    private val kafkaTopic = "privat-melosys-eessi-v1-local"

    @BeforeEach
    fun setup() {
        rinaSaksnummer = Random().nextInt(100000).toString()
    }

    @Test
    fun mottaSED_mottar3SED_blirBehandletEtterHverandre() {
        //Periode på 6 år - fører til et kontrolltreff
        val eessiMeldingA009 = melosysEessiMelding(
            BucType.LA_BUC_04, SedType.A009, Periode(LocalDate.now(), LocalDate.now().plusYears(6)),
            "12_1", opprettEessiJournalpost(SedType.A009)
        )
        val eessiMeldingX001 = melosysEessiMelding(
            BucType.LA_BUC_04, SedType.X001, null, null, opprettEessiJournalpost(SedType.X001)
        )
        val eessiMeldingX007 = melosysEessiMelding(
            BucType.LA_BUC_04, SedType.X007, null, null, opprettEessiJournalpost(SedType.X007)
        )
        melosysEessiMeldingKafkaTemplate!!.send(kafkaTopic, eessiMeldingA009)
        melosysEessiMeldingKafkaTemplate.send(kafkaTopic, eessiMeldingX001)
        melosysEessiMeldingKafkaTemplate.send(kafkaTopic, eessiMeldingX007)
        Awaitility.await().timeout(Duration.ofSeconds(20)).pollInterval(Duration.ofSeconds(3))
            .until {
                prosessinstansRepository!!.findAllByStatusNotInAndLåsReferanseStartingWith(
                    List.of(ProsessStatus.FERDIG),
                    rinaSaksnummer
                ).isEmpty()
            }
        val prosessinstanserSortert = prosessinstansRepository!!.findAllByLåsReferanseStartingWith(rinaSaksnummer)
            .stream()
            .sorted(Comparator.comparing { obj: Prosessinstans -> obj.endretDato })
            .collect(Collectors.toList())

//        Hver SED blir til en mottaksprosess + en behandlingsprosess
//        Assertions.assertThat(prosessinstanserSortert)
//            .extracting<String, RuntimeException> { obj: Prosessinstans -> obj.låsReferanse }
//            .hasSize(6)
//            .containsExactly(
//                eessiMeldingA009.lagUnikIdentifikator(),
//                eessiMeldingA009.lagUnikIdentifikator(),
//                eessiMeldingX001.lagUnikIdentifikator(),
//                eessiMeldingX001.lagUnikIdentifikator(),
//                eessiMeldingX007.lagUnikIdentifikator(),
//                eessiMeldingX007.lagUnikIdentifikator()
//            )
    }

    private fun melosysEessiMelding(
        bucType: BucType,
        sedType: SedType,
        periode: Periode?,
        artikkel: String?,
        journalpostID: String
    ): MelosysEessiMelding {
        val eessiMelding = MelosysEessiMelding()
        eessiMelding.aktoerId = "1111111111111"
        eessiMelding.anmodningUnntak = null
        eessiMelding.arbeidssteder = List.of()
        eessiMelding.bucType = bucType.name
        eessiMelding.gsakSaksnummer = null
        eessiMelding.artikkel = artikkel
        eessiMelding.avsender = Avsender("SE:123", "SE")
        eessiMelding.dokumentId = null
        eessiMelding.journalpostId = journalpostID
        eessiMelding.lovvalgsland = "SE"
        eessiMelding.periode = periode
        eessiMelding.sedType = sedType.name
        eessiMelding.sedId = sedType.name
        eessiMelding.rinaSaksnummer = rinaSaksnummer
        eessiMelding.statsborgerskap = List.of()
        eessiMelding.sedVersjon = "1"
        return eessiMelding
    }

    private fun opprettEessiJournalpost(sedType: SedType): String {
        val request = OpprettJournalpost()
        val hovedDokument = FysiskDokument()
        hovedDokument.dokumentKategori = "SED"
        hovedDokument.tittel = "$sedType-tittel"
        hovedDokument.brevkode = sedType.name
        hovedDokument.dokumentVarianter = listOf(
            DokumentVariant.lagDokumentVariant(
                ByteArray(0)
            )
        )
        request.setHoveddokument(hovedDokument)
        request.brukerId = "123123123"
        request.brukerIdType = BrukerIdType.FOLKEREGISTERIDENT
        request.journalposttype = Journalposttype.INN
        request.journalførendeEnhet = "4530"
        request.tema = "UFM"
        request.korrespondansepartId = "SE:123"
        request.korrespondansepartNavn = "Sverige"
        request.korrespondansepartLand = "SE"
        request.setKorrespondansepartIdType(OpprettJournalpost.KorrespondansepartIdType.UTENLANDSK_ORGANISASJON)
        request.mottaksKanal = "EESSI"
        request.journalposttype = Journalposttype.INN
        request.innhold = "$sedType-tittel"
        return joarkFasade!!.opprettJournalpost(request, false)
    }
}
