package no.nav.melosys.service.avgift.aarsavregning

import io.getunleash.Unleash
import mu.KotlinLogging
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.avgift.aarsavregning.Skattehendelse
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.avgift.TrygdeavgiftService
import no.nav.melosys.service.sak.FagsakService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger { }

@Profile("!local-q1 & !local-q2")
@Service
class SkattehendelserConsumer(
    @Autowired private val prosessinstansService: ProsessinstansService,
    @Autowired private val unleash: Unleash,
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val trygdeavgiftService: TrygdeavgiftService,
) {

    @KafkaListener(
        clientIdPrefix = "aiven-melosys-skattehendelser-consumer",
        topics = ["\${kafka.aiven.skattehendelser.topic}"],
        containerFactory = "aivenSkattehendelserListenerContainerFactory"
    )
    fun lesSkattehendelser(consumerRecord: ConsumerRecord<String, Skattehendelse>) {
        if (unleash.isEnabled(ToggleName.MELOSYS_SKATTEHENDELSE_CONSUMER)) {
            val skattehendelse = consumerRecord.value()
            val sakerMedTrygdeavgift = finnSakMedTrygdeavgift(skattehendelse.identifikator).also {
                check(it.isNotEmpty()) { "Fant ingen sak med trygdeavgift for aktør: $skattehendelse.identifikator" }
            }
            for (fagsak in sakerMedTrygdeavgift) {
                prosessinstansService.opprettArsavregningsBehandlingProsessflyt(fagsak.saksnummer, skattehendelse.gjelderPeriode)
            }
        } else {
            log.info { "Skattehendelsemelding med key: ${consumerRecord.key()}" }
        }
    }


    private fun finnSakMedTrygdeavgift(aktørId: String): List<Fagsak> {
        return fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, aktørId)
            .filter {
                //FIXME MELOSYS-6862
                trygdeavgiftService.harFagsakBehandlingerMedTrygdeavgift(it.saksnummer)
            }
    }
}


