package no.nav.melosys.integrasjonstest.saksflyt;

import com.fasterxml.jackson.databind.JsonNode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Avsendertyper;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_barn_begrunnelser_ftrl;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_ektefelle_samboer_begrunnelser_ftrl;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.repository.VedtakMetadataRepository;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.TrygdeavtaleService;
import no.nav.melosys.service.avklartefakta.AvklarteMedfolgendeFamilieService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.dokument.brev.KopiMottaker;
import no.nav.melosys.service.felles.dto.SoeknadslandDto;
import no.nav.melosys.service.journalforing.JournalfoeringService;
import no.nav.melosys.service.journalforing.dto.DokumentDto;
import no.nav.melosys.service.journalforing.dto.FagsakDto;
import no.nav.melosys.service.journalforing.dto.JournalfoeringOpprettDto;
import no.nav.melosys.service.journalforing.dto.PeriodeDto;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.service.vedtak.FattTrygdeavtaleVedtakRequest;
import no.nav.melosys.service.vedtak.FattVedtakRequest;
import no.nav.melosys.service.vedtak.VedtakServiceFasade;
import no.nav.melosys.tjenester.gui.TrygdeavtaleTjeneste;
import no.nav.melosys.tjenester.gui.dto.trygdeavtale.TrygdeavtaleResultatDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.util.List;

import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper.FASTSATT_LOVVALGSLAND;

@ExtendWith(SpringExtension.class)
@SpringBootTest(properties = "spring.profiles.active:local-mock")
@Disabled
class TrygdeavtaleTjenesteIT {

    @Autowired
    private TrygdeavtaleService trygdeavtaleService;
    @Autowired
    private BehandlingService behandlingService;
    @Autowired
    private Aksesskontroll aksesskontroll;
    @Autowired
    private BehandlingsgrunnlagService behandlingsgrunnlagService;
    @Autowired
    private AvklarteMedfolgendeFamilieService avklarteMedfolgendeFamilieService;
    @Autowired
    private JournalfoeringService journalfoeringService;
    @Autowired
    VedtakServiceFasade vedtakServiceFasade;
    @Autowired
    private AvklarteVirksomheterService avklarteVirksomheterService;
    @Autowired
    private LovvalgsperiodeService lovvalgsperiodeService;
    @Autowired
    private VedtakMetadataRepository vedtakMetadataRepository;

    private TrygdeavtaleTjeneste trygdeavtaleTjeneste;

    @AfterEach
    void after() {
        vedtakMetadataRepository.deleteAll();
    }

    @BeforeEach
    void setup() throws InterruptedException {
        trygdeavtaleTjeneste = new TrygdeavtaleTjeneste(
            trygdeavtaleService,
            behandlingService,
            aksesskontroll,
            behandlingsgrunnlagService,
            avklarteMedfolgendeFamilieService,
            avklarteVirksomheterService,
            lovvalgsperiodeService);

        // Vi må slette dette for å kunne kjøre vedtak på nytt
        vedtakMetadataRepository.deleteAll();

        // Trenger bare kjøre denne første gangen så kan data gjenbrukes
//        lagData();

        //
        // Barn - 02112199996
        // Kone - 02112199805

//        SoeknadFtrl behandlingsgrunnlagdata = (SoeknadFtrl) behandlingsgrunnlag.getBehandlingsgrunnlagdata();
//        behandlingsgrunnlagdata.periode = new Periode(
//            LocalDate.of(2021, 1, 1),
//            LocalDate.of(2021, 2, 1)
//        );
//        behandlingsgrunnlagdata.soeknadsland.landkoder = List.of("GB");
//        List<MedfolgendeFamilie> medfolgendeFamilie = new ArrayList<>();
//        medfolgendeFamilie.add(MedfolgendeFamilie.tilMedfolgendeFamilie(
//            "0bad5c70-8a3f-4fc7-9031-d3aebd6b68de", "fnr", "role",
//            MedfolgendeFamilie.Relasjonsrolle.BARN
//        ));
//        medfolgendeFamilie.add(MedfolgendeFamilie.tilMedfolgendeFamilie(
//            "1212121212121-4fc7-9031-ab34332121ff", "fnr", "role",
//            MedfolgendeFamilie.Relasjonsrolle.EKTEFELLE_SAMBOER
//        ));
//        behandlingsgrunnlagdata.personOpplysninger.medfolgendeFamilie = medfolgendeFamilie;
//
//        behandlingsgrunnlagService.oppdaterBehandlingsgrunnlag(behandlingsgrunnlag);

//        String jsonData = behandlingsgrunnlag.getJsonData();
//        System.out.println(jsonData);
    }

    private void lagData() throws InterruptedException {
        TestDataForTrygdeavtale testDataForTrygdeavtale = new TestDataForTrygdeavtale();
//        testDataForTrygdeavtale.lagJfrOppgave();
        JsonNode journalpostNode = testDataForTrygdeavtale.hentFørsteOppgave();
        String journalpostID = journalpostNode.get("journalpostId").asText();
        String oppgaveId = journalpostNode.get("id").asText();
        JsonNode saksreferanseNode = journalpostNode.get("saksreferanse");
        System.out.println(saksreferanseNode);
        assert saksreferanseNode.isNull();

        Journalpost journalpost = journalfoeringService.hentJournalpost(journalpostID);
        if (!journalpost.isErFerdigstilt()) {
            System.out.println("opprettOgJournalfør");
            JournalfoeringOpprettDto journalfoeringDto = lagJournalfoeringOpprettDto(journalpost, oppgaveId);

            int lastId = testDataForTrygdeavtale.hentNyesteOppgave().get("id").asInt();
            journalfoeringService.opprettOgJournalfør(journalfoeringDto); // Det lages en ny Journalpost

            // Må vente på at prosses har kjørt igennom alle steg
            while (saksreferanseNode.isNull()) {
                System.out.println("Sleeping 1s");
                Thread.sleep(1000);

                JsonNode jsonNode = testDataForTrygdeavtale.hentNyesteOppgave();
                System.out.println(jsonNode.get("id").asInt());
                System.out.println(jsonNode.get("saksreferanse"));
                if (jsonNode.get("id").asInt() == lastId) continue;

                saksreferanseNode = jsonNode.get("saksreferanse");
                System.out.println(saksreferanseNode.asText());
            }
        }
        if (saksreferanseNode.isNull()) {
            System.out.println("saksreferanseNode.isNull()");
            return;
        }

        long behandlingId = Long.parseLong(saksreferanseNode.asText().split("-")[1]);
        Behandling behandling = behandlingService.hentBehandling(behandlingId);
        behandlingService.endreBehandlingsstatusFraOpprettetTilUnderBehandling(behandling);
//        Behandlingsgrunnlag behandlingsgrunnlag = behandling.getBehandlingsgrunnlag();
//        String jsonData = behandlingsgrunnlag.getJsonData();
//        System.out.println(jsonData);
    }

    private JournalfoeringOpprettDto lagJournalfoeringOpprettDto(Journalpost journalpost, String oppgaveId) {
        JournalfoeringOpprettDto journalfoeringDto = new JournalfoeringOpprettDto();
        journalfoeringDto.setAvsenderID("30056928150");
        journalfoeringDto.setAvsenderNavn("KARAFFEL TRIVIELL");
        journalfoeringDto.setBrukerID("30056928150");
        journalfoeringDto.setAvsenderType(Avsendertyper.PERSON);
        DokumentDto dokumentDto = new DokumentDto(journalpost.getHoveddokument().getDokumentId(), "Søknad om A1 for utsendte arbeidstakere i EØS/Sveits");
        journalfoeringDto.setHoveddokument(dokumentDto);
        journalfoeringDto.setJournalpostID(journalpost.getJournalpostId());
        journalfoeringDto.setOppgaveID(oppgaveId);
        journalfoeringDto.setSkalTilordnes(true);
        journalfoeringDto.setMottattDato(LocalDate.of(2021, 10, 13));
        journalfoeringDto.setBehandlingstemaKode("TRYGDEAVTALE_UK");
        FagsakDto fagsak = new FagsakDto();
        fagsak.setSakstype("TRYGDEAVTALE");
        fagsak.setSoknadsperiode(new PeriodeDto());
        fagsak.setLand(new SoeknadslandDto());
        journalfoeringDto.setFagsak(fagsak);
        return journalfoeringDto;
    }

    @Test
    void test() throws ValideringException, InterruptedException {
        TrygdeavtaleResultatDto trygdeavtaleResultatDto = new TrygdeavtaleResultatDto.Builder()
            .virksomheter(List.of("999999999"))
            .bestemmelse("UK_ART6_1")
            .addBarn("f922d0c8-269e-4c83-a12d-7b29e33ccf75",
                false, Medfolgende_barn_begrunnelser_ftrl.OVER_18_AR.getKode(),
                "begrunnelse barn")
            .ektefelle("4ffa867c-e645-4389-8f08-24a6e564889c",
                true, "",
                "")
            .build();

//        trygdeavtaleTjeneste.overforDataForVedtak(1L, trygdeavtaleResultatDto);
//
        FattVedtakRequest fattVedtakRequest = new FattTrygdeavtaleVedtakRequest
            .Builder()
            .medBehandlingsresultat(FASTSATT_LOVVALGSLAND)
            .medVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK)
            .medFritekstInnledning("Innledning")
            .medFritekstBegrunnelse("Begrunnelse")
            .medFritekstEktefelle("Ektefelle omfattet")
            .medFritekstBarn("Barn ikke omfattet")
            .medKopiMottakere(List.of(new KopiMottaker(Aktoersroller.ARBEIDSGIVER, "999999999", null)))
            .medBestillersId("Z123456") //
            .build();

        vedtakServiceFasade.fattVedtak(1L, fattVedtakRequest);
        Thread.sleep(100000000);

//        System.out.println("done!");
    }
}
