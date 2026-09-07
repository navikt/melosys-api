package no.nav.melosys.service.avgift.aarsavregning

import io.getunleash.Unleash
import jakarta.transaction.Transactional
import mu.KotlinLogging
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.avgift.aarsavregning.Skattehendelse
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger { }

@Profile("!local-q1 & !local-q2")
@Service
class SkattehendelserConsumer(
    @Autowired private val unleash: Unleash,
    @Autowired private val opprettelseService: SkattepliktigAarsavregningOpprettelseService,
) {

    @KafkaListener(
        clientIdPrefix = "aiven-melosys-skattehendelser-consumer",
        topics = ["\${kafka.aiven.skattehendelser.topic}"],
        containerFactory = "aivenSkattehendelserListenerContainerFactory"
    )
    @Transactional
    fun lesSkattehendelser(consumerRecord: ConsumerRecord<String, Skattehendelse>) {
        ThreadLocalAccessInfo.executeProcess("skattehendelser-consumer") {
            log.info { "Mottok skattehendelse med key: ${consumerRecord.key()}" }
            if (unleash.isEnabled(ToggleName.MELOSYS_SKATTEHENDELSE_CONSUMER)) {
                val skattehendelse = consumerRecord.value()
                val sakerMedTrygdeavgift =
                    opprettelseService.finnSakerMedTrygdeavgift(skattehendelse.identifikator, skattehendelse.gjelderPeriode.toInt())
                if (sakerMedTrygdeavgift.isEmpty()) {
                    log.warn { "Fant ingen sak med trygdeavgift for aktør: ${skattehendelse.identifikator}" }
                }
                for (fagsak in sakerMedTrygdeavgift) {
                    if (skalOpprettArsavregningsBehandlingProsessflyt(fagsak, skattehendelse.gjelderPeriode.toInt())) {
                        opprettelseService.opprettProsessinstans(fagsak.saksnummer, skattehendelse.gjelderPeriode)
                    }
                }
            } else {
                log.info { "Skattehendelsemelding med key: ${consumerRecord.key()}" }
            }
        }
    }

    private fun skalOpprettArsavregningsBehandlingProsessflyt(sakMedTrygdeavgift: Fagsak, gjelderÅr: Int): Boolean {
        val behandling = opprettelseService.finnAktivÅrsavregningBehandling(sakMedTrygdeavgift, gjelderÅr) ?: return true

        log.info { "Årsavregning behandling(${behandling.id}) for sak: ${sakMedTrygdeavgift.saksnummer} og år: $gjelderÅr er allerede opprettet" }
        if (behandling.status != Behandlingsstatus.OPPRETTET) {
            opprettelseService.settStatusVurderDokument(behandling.id, behandling.status)
        }
        return false
    }
}
