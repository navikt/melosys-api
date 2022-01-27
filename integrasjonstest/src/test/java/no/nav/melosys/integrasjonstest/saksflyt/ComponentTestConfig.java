package no.nav.melosys.integrasjonstest.saksflyt;

import no.finn.unleash.FakeUnleash;
import no.finn.unleash.Unleash;
import no.nav.melosys.melosysmock.config.GraphqlConfig;
import no.nav.melosys.melosysmock.config.SoapConfig;
import no.nav.melosys.melosysmock.journalpost.journalpostapi.JournalpostApi;
import no.nav.melosys.melosysmock.journalpost.saf.SafRestApi;
import no.nav.melosys.melosysmock.sak.SakApi;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.util.SocketUtils;

@TestConfiguration
@Import(
    {
        GraphqlConfig.class,
        JournalpostApi.class,
        SafRestApi.class,
        SakApi.class,
        SoapConfig.class,
        EndpointsListener.class
    }
)
public class ComponentTestConfig {

    static {
        System.setProperty("kafkaPort", String.valueOf(SocketUtils.findAvailableTcpPort(60000, 65535)));
    }

    @Bean
    @Order(1)
    EmbeddedKafkaBroker kafkaEmbedded(Environment env) {
        EmbeddedKafkaBroker kafka = new EmbeddedKafkaBroker(1, true, 1,
            "privat-melosys-eessi-v1-local",
            "privat-melosys-soknad-mottak-local",
            "aapen-melosys-utstedtA1-v1-local",
            "teammelosys.fattetvedtak.v1-local");
        kafka.kafkaPorts(Integer.parseInt(env.getRequiredProperty("kafkaPort")));
        kafka.brokerProperty("offsets.topic.replication.factor", (short) 1);
        kafka.brokerProperty("transaction.state.log.replication.factor", (short) 1);
        kafka.brokerProperty("transaction.state.log.min.isr", 1);

        return kafka;
    }


    @Bean
    @Primary
    public Unleash fakeUnleash() {
        return new FakeUnleash();
    }

//    @Bean
//    @Qualifier("system")
//    public JoarkFasade joarkFasade() {
//        return new JoarkStub();
//    }
//
//    @Bean
//    @Qualifier("system")
//    public EessiConsumer eessiConsumer(){
//        return new EessiConsumerStub();
//    }
//
//    @Bean
//    @Qualifier("system")
//    public SakConsumer sakConsumerStub(){
//        return new SakConsumerStub();
//    }
//
//    @Bean
//    @Qualifier("saksbehandler")
//    public PDLConsumer pdlConsumerForSaksbehandler(){
//        return new PDLConsumerStub();
//    }
//
//    @Bean
//    public ArkivsakService arkivsakService(){
//        return new ArkivsakService(sakConsumerStub());
//    }
//
//    @Bean
//    @Qualifier("system")
//    public EessiService eessiService(SedDataBygger sedDataBygger,
//                                     @Qualifier("system") SedDataGrunnlagFactory dataGrunnlagFactory,
//                                     @Qualifier("system") EessiConsumer eessiConsumer,
//                                     @Qualifier("system") JoarkFasade joarkFasade,
//                                     BehandlingService behandlingService,
//                                     BehandlingsresultatService behandlingsresultatService) {
//        return new EessiService(behandlingService, behandlingsresultatService, eessiConsumer, joarkFasade, sedDataBygger,
//            dataGrunnlagFactory);
//    }


}
