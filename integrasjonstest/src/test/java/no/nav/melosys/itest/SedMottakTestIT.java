package no.nav.melosys.itest;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import no.nav.melosys.domain.arkiv.*;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.eessi.Periode;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.Avsender;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.saksflyt.ProsessStatus;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.repository.ProsessinstansRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class SedMottakTestIT extends ComponentTestBase {

    @Autowired
    @Qualifier("system")
    private JoarkFasade joarkFasade;

    @Autowired
    @Qualifier("melosysEessiMelding")
    private KafkaTemplate<String, MelosysEessiMelding> melosysEessiMeldingKafkaTemplate;

    @Autowired
    private ProsessinstansRepository prosessinstansRepository;

    private String rinaSaksnummer;

    private String kafkaTopic = "privat-melosys-eessi-v1-local";

    @BeforeEach
    void setup() {
        rinaSaksnummer = String.valueOf(new Random().nextInt(100_000));
    }

    @Test
    void mottaSED_mottar3SED_blirBehandletEtterHverandre() {
        //Periode på 6 år - fører til et kontrolltreff
        var eessiMeldingA009 = melosysEessiMelding(
            BucType.LA_BUC_04, SedType.A009, new Periode(LocalDate.now(), LocalDate.now().plusYears(6)),
            "12_1", opprettEessiJournalpost(SedType.A009)
        );
        var eessiMeldingX001 = melosysEessiMelding(
            BucType.LA_BUC_04, SedType.X001, null, null, opprettEessiJournalpost(SedType.X001)
        );

        var eessiMeldingX007 = melosysEessiMelding(
            BucType.LA_BUC_04, SedType.X007, null, null, opprettEessiJournalpost(SedType.X007)
        );

        melosysEessiMeldingKafkaTemplate.send(kafkaTopic, eessiMeldingA009);
        melosysEessiMeldingKafkaTemplate.send(kafkaTopic, eessiMeldingX001);
        melosysEessiMeldingKafkaTemplate.send(kafkaTopic, eessiMeldingX007);


        await().timeout(Duration.ofSeconds(20)).pollInterval(Duration.ofSeconds(3))
            .until(() -> prosessinstansRepository.findAllByStatusNotInAndLåsReferanseStartingWith(List.of(ProsessStatus.FERDIG), rinaSaksnummer).isEmpty());

        var prosessinstanserSortert = prosessinstansRepository.findAllByLåsReferanseStartingWith(rinaSaksnummer)
            .stream()
            .sorted(Comparator.comparing(Prosessinstans::getEndretDato))
            .collect(Collectors.toList());

        //Hver SED blir til en mottaksprosess + en behandlingsprosess
        assertThat(prosessinstanserSortert)
            .extracting(Prosessinstans::getLåsReferanse)
            .hasSize(6)
            .containsExactly(
                eessiMeldingA009.lagUnikIdentifikator(),
                eessiMeldingA009.lagUnikIdentifikator(),
                eessiMeldingX001.lagUnikIdentifikator(),
                eessiMeldingX001.lagUnikIdentifikator(),
                eessiMeldingX007.lagUnikIdentifikator(),
                eessiMeldingX007.lagUnikIdentifikator()
            );

    }

    private MelosysEessiMelding melosysEessiMelding(BucType bucType,
                                                    SedType sedType,
                                                    Periode periode,
                                                    String artikkel,
                                                    String journalpostID) {
        var eessiMelding = new MelosysEessiMelding();
        eessiMelding.setAktoerId("1111111111111");
        eessiMelding.setAnmodningUnntak(null);
        eessiMelding.setArbeidssteder(List.of());
        eessiMelding.setBucType(bucType.name());
        eessiMelding.setGsakSaksnummer(null);
        eessiMelding.setArtikkel(artikkel);
        eessiMelding.setAvsender(new Avsender("SE:123", "SE"));
        eessiMelding.setDokumentId(null);
        eessiMelding.setJournalpostId(journalpostID);
        eessiMelding.setLovvalgsland("SE");
        eessiMelding.setPeriode(periode);
        eessiMelding.setSedType(sedType.name());
        eessiMelding.setSedId(sedType.name());
        eessiMelding.setRinaSaksnummer(rinaSaksnummer);
        eessiMelding.setStatsborgerskap(List.of());
        eessiMelding.setSedVersjon("1");
        return eessiMelding;
    }

    private String opprettEessiJournalpost(SedType sedType) {
        OpprettJournalpost request = new OpprettJournalpost();

        FysiskDokument hovedDokument = new FysiskDokument();
        hovedDokument.setDokumentKategori("SED");
        hovedDokument.setTittel(sedType + "-tittel");
        hovedDokument.setBrevkode(sedType.name());
        hovedDokument.setDokumentVarianter(Collections.singletonList(DokumentVariant.lagDokumentVariant(new byte[0])));
        request.setHoveddokument(hovedDokument);
        request.setBrukerId("123123123");
        request.setBrukerIdType(BrukerIdType.FOLKEREGISTERIDENT);

        request.setJournalposttype(Journalposttype.INN);
        request.setJournalførendeEnhet("4530");
        request.setTema("UFM");

        request.setKorrespondansepartId("SE:123");
        request.setKorrespondansepartNavn("Sverige");
        request.setKorrespondansepartLand("SE");
        request.setKorrespondansepartIdType("UTL_ORG");
        request.setMottaksKanal("EESSI");
        request.setJournalposttype(Journalposttype.INN);
        request.setInnhold(sedType + "-tittel");

        return joarkFasade.opprettJournalpost(request, false);
    }
}
