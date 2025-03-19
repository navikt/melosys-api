package no.nav.melosys.service.soknad;

import no.nav.melosys.integrasjon.SoknadMottatt;
import no.nav.melosys.saksflytapi.ProsessinstansService;
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Profile("!local-q1 & !local-q2")
public class SoknadMottattConsumer {
    private static final Logger log = LoggerFactory.getLogger(SoknadMottattConsumer.class);

    private final ProsessinstansService prosessinstansService;

    private final MottatteOpplysningerService mottatteOpplysningerService;

    public SoknadMottattConsumer(ProsessinstansService prosessinstansService, MottatteOpplysningerService mottatteOpplysningerService) {
        this.prosessinstansService = prosessinstansService;
        this.mottatteOpplysningerService = mottatteOpplysningerService;
    }

    @KafkaListener(clientIdPrefix = "melosys-soknad-mottak-consumer", topics = "${kafka.aiven.soknad-mottak.topic}",
        containerFactory = "aivenSoknadMottattContainerFactory")
    public void mottaAivenMelding(ConsumerRecord<String, SoknadMottatt> consumerRecord) {
        SoknadMottatt søknadMottatt = consumerRecord.value();
        log.info("Mottatt ny søknadMottatt fra altinn via aiven: {}", søknadMottatt);

        String soknadID = søknadMottatt.getSoknadID();
        boolean forGammelTilForvaltningsmelding = søknadMottatt.erForGammelTilForvaltningsmelding();
        boolean erMottattSøknadTidligere = mottatteOpplysningerService.harMottattSøknadMedEksternReferanseID(søknadMottatt.getSoknadID());

        try {
            prosessinstansService.opprettProsessinstansSøknadMottatt(soknadID, forGammelTilForvaltningsmelding, erMottattSøknadTidligere);
        } catch (Exception e) {
            log.error("Feil ved mottak av søknad fra altinn via aiven! SoknadID: {} ConsumerRecord.key: {}",
                consumerRecord.value().getSoknadID(), consumerRecord.key());
            throw e;
        }
    }
}
