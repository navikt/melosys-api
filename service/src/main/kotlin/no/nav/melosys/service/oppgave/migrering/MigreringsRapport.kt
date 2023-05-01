package no.nav.melosys.service.oppgave.migrering

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val log = KotlinLogging.logger { }

@Component
class MigreringsRapport(private val environment: Environment) {
    @Volatile
    internal var antallSakerFunnet: Int = 0

    @Volatile
    internal var antallSakerErRedigerbar: Int = 0

    @Volatile
    internal var antallSakerProssessert: Int = 0

    @Volatile
    private var antallSakerMigrert: Int = 0

    @Volatile
    private var sakSomMangerOppgaveAntall: Int = 0

    private val sakerManglerOppgave = mutableListOf<String>()
    private val sakerMedFlereOppgaver = mutableListOf<String>()
    private val sakHvorViSkalHaSedMenSomIkkeFinnes = mutableListOf<String>()
    private val sakHvorMappingFeiler = mutableListOf<String>()
    private val migreringsSakListe = mutableListOf<MigreringsSak>()

    fun html(action: (List<MigreringsSak>) -> List<MigreringsSak>): String {
        val migreringsSakerHtml = action(
            migreringsSakListe.sortedWith(
                compareBy(
                    { it.sak.sakstype },
                    { it.sak.sakstema },
                    { it.sak.behandlingstype },
                    { it.sak.behandlingstema },
                    { it.sak.behandlingstatus },
                )
            )
        ).joinToString("") { it.htmlTableRow() }

        return """
            <!DOCTYPE html>
            <html>
            <style>
                table, th, td {
                    border: 1px solid black;
                    border-collapse: collapse;
                }
                .sak {
                    background-color:#fff0b3
                }
                .ny {
                    background-color:#abf5d1
                }
                .oppgave {
                    background-color:#ffebe6
                }
                .feil {
                    background-color: #ff5656
                }
                .sakfeil {
                    background-color: #ffceb3
                }
                .forskjell {
                    background-color: rgba(255, 33, 0, 0.61)
                }
            </style>
                <body>
                   <table>
                        <tr>
                            <th class="sak" colspan=6>melosys sak</th>
                            <th class="ny" colspan=4>migrert oppgave</th>
                            <th class="oppgave" colspan=8>orginal oppgave</th>
                        </tr>
                        <tr>
                            <th class="sak">sakstype</th>
                            <th class="sak">sakstema</th>
                            <th class="sak">behandlingstype</th>
                            <th class="sak">behandlingstema</th>
                            <th class="sak">behandlingstatus</th>
                            <th class="sak">saksnummer</th>
                            <th class="ny">behandlings<br>tema</th>
                            <th class="ny">tema</th>
                            <th class="ny">oppgavetype</th>
                            <th class="ny">beskrivelse</th>
                            <th class="oppgave">behandlings<br>tema</th>
                            <th class="oppgave">tema</th>
                            <th class="oppgave">oppgavetype</th>
                            <th class="oppgave">behandlings<br>type</th>
                            <th class="oppgave">beskrivelse</th>
                            <th class="oppgave">bruker</th>
                            <th class="oppgave">opprettet tidspunkt</th>
                            <th class="oppgave">frist</th>
                        </tr>
                    $migreringsSakerHtml
                   </table>
                </body>
            </html>
            """

    }

    fun status(): Map<String, Any> {
        return mapOf(
            "antallSakerFunnet" to antallSakerFunnet,
            "antallSakerErRedigerbar" to antallSakerErRedigerbar,
            "antallSakerProssessert" to antallSakerProssessert,
            "antallSakerMigrert" to antallSakerMigrert,
            "harIkkeÅpenOppgave" to sakSomMangerOppgaveAntall,
        )
    }

    internal fun mappingFeiler(message: String) {
        sakHvorMappingFeiler.add(message)
    }

    internal fun migrertSak(migreringsSak: MigreringsSak) {
        migreringsSakListe.add(migreringsSak)
    }

    internal fun sakManglerOppgave(saksnummer: String) {
        sakSomMangerOppgaveAntall++
        sakerManglerOppgave.add(saksnummer)
    }

    internal fun sakerMedFlereOppgaver(message: String) {
        log.warn(message)
        sakerMedFlereOppgaver.add(message)
    }

    internal fun finnesIkkeSedForSak(msg: String) {
        sakHvorViSkalHaSedMenSomIkkeFinnes.add(msg)
    }

    internal fun sakMedOppgave(migreringsSak: MigreringsSak) {
        antallSakerMigrert++
    }

    internal fun statusEtterKjøring(): String {
        val sb = StringBuilder()
        sb.appendLine()
        sb.appendLine("sakerMedOppgave: $antallSakerMigrert")
        sb.appendLine("sakerManglerOppgave: ${sakerManglerOppgave.size}")
        sb.appendLine("sakerMedFlereOppgaver: ${sakerMedFlereOppgaver.size}")
        sb.appendLine("sakHvorMappingFeiler: ${sakHvorMappingFeiler.size}")
        sb.appendLine("sakHvorViSkalHaSedMenSomIkkeFinnes: ${sakHvorViSkalHaSedMenSomIkkeFinnes.size}")
        return sb.toString()
    }

    internal fun saveStatusFiles(status: String) {
        val profil = environment.activeProfiles.first()
        log.info("Profile:$profil")
        if (profil !in listOf("local-mock", "local-q1", "local-q2")) return // kun lag profiler ved lokal kjøring
        val timeForRun =
            "oppgave-migrering/$profil-${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))}"
        File(timeForRun).mkdirs()

        File("$timeForRun/diff.json").writeText(migreringsSakListeSomJsonString())

        File("$timeForRun/status.txt").writeText(status)
        File("$timeForRun/saker-mangler-oppgave.txt").writeText(sakerManglerOppgave.joinToString(","))
        File("$timeForRun/saker-med-flere-oppgave.txt").writeText(sakerMedFlereOppgaver.joinToString("\n"))
        File("$timeForRun/sak-hvor-mapping-feiler.txt").writeText(sakHvorMappingFeiler.joinToString("\n"))
        File("$timeForRun/sak-hvor-vi-skal-ha-sed-men-som-ikke-finnes.txt").writeText(
            sakHvorViSkalHaSedMenSomIkkeFinnes.joinToString("\n")
        )
    }

    fun migreringsSakListeSomJsonString(): String = migreringsSakListe.toJsonNode.toPrettyString()

    private val Any.toJsonNode: JsonNode
        get() {
            return jacksonObjectMapper()
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .registerModule(JavaTimeModule())
                .valueToTree(this)
        }

}
