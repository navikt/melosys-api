package no.nav.melosys.integrasjonstest.saksflyt;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.SoeknadFtrl;
import no.nav.melosys.domain.behandlingsgrunnlag.data.MedfolgendeFamilie;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Periode;
import no.nav.melosys.domain.kodeverk.Avsendertyper;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_barn_begrunnelser_ftrl;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_ektefelle_samboer_begrunnelser_ftrl;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.TrygdeavtaleService;
import no.nav.melosys.service.avklartefakta.AvklarteMedfolgendeFamilieService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
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
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.tjenester.gui.TrygdeavtaleTjeneste;
import no.nav.melosys.tjenester.gui.dto.trygdeavtale.TrygdeAvtaleDataForVedtakDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@ExtendWith(SpringExtension.class)
@SpringBootTest(properties = "spring.profiles.active:local-mock")
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

    private TrygdeavtaleTjeneste trygdeavtaleTjeneste;

    @BeforeEach
    void setup() {
        trygdeavtaleTjeneste = new TrygdeavtaleTjeneste(
            trygdeavtaleService,
            behandlingService,
            aksesskontroll,
            behandlingsgrunnlagService,
            avklarteMedfolgendeFamilieService,
            avklarteVirksomheterService,
            lovvalgsperiodeService);

        // TODO: bruk http://localhost:8083/testdata/jfr-oppgave til å lage oppgave og kjør så opprettOgJournalfør
        Journalpost journalpost = journalfoeringService.hentJournalpost("526345");
        if (!journalpost.isErFerdigstilt()) {
            System.out.println("opprettOgJournalfør");
            JournalfoeringOpprettDto journalfoeringDto = lagJournalfoeringOpprettDto();
            journalfoeringService.opprettOgJournalfør(journalfoeringDto);
        }

        // kan slå opp i behandling for å finne behandling id fra INITIERENDE_JOURNALPOST_ID
        Behandling behandling = behandlingService.hentBehandling(2L);
        behandlingService.endreBehandlingsstatusFraOpprettetTilUnderBehandling(behandling);
        Behandlingsgrunnlag behandlingsgrunnlag = behandling.getBehandlingsgrunnlag();

        SoeknadFtrl behandlingsgrunnlagdata = (SoeknadFtrl) behandlingsgrunnlag.getBehandlingsgrunnlagdata();
        behandlingsgrunnlagdata.periode = new Periode(
            LocalDate.of(2021, 1, 1),
            LocalDate.of(2021, 2, 1)
        );
        behandlingsgrunnlagdata.soeknadsland.landkoder = List.of("GB");
        List<MedfolgendeFamilie> medfolgendeFamilie = new ArrayList<>();
        medfolgendeFamilie.add(MedfolgendeFamilie.tilMedfolgendeFamilie(
            "0bad5c70-8a3f-4fc7-9031-d3aebd6b68de", "fnr", "role",
            MedfolgendeFamilie.Relasjonsrolle.BARN
        ));
        medfolgendeFamilie.add(MedfolgendeFamilie.tilMedfolgendeFamilie(
            "1212121212121-4fc7-9031-ab34332121ff", "fnr", "role",
            MedfolgendeFamilie.Relasjonsrolle.EKTEFELLE_SAMBOER
        ));
        behandlingsgrunnlagdata.personOpplysninger.medfolgendeFamilie = medfolgendeFamilie;

        behandlingsgrunnlagService.oppdaterBehandlingsgrunnlag(behandlingsgrunnlag);

//        String jsonData = behandlingsgrunnlag.getJsonData();
//        System.out.println(jsonData);
    }

    private JournalfoeringOpprettDto lagJournalfoeringOpprettDto() {
        JournalfoeringOpprettDto journalfoeringDto = new JournalfoeringOpprettDto();
        journalfoeringDto.setAvsenderID("30056928150");
        journalfoeringDto.setAvsenderNavn("KARAFFEL TRIVIELL");
        journalfoeringDto.setBrukerID("30056928150");
        journalfoeringDto.setAvsenderType(Avsendertyper.PERSON);
        DokumentDto dokumentDto = new DokumentDto("85937", "Søknad om A1 for utsendte arbeidstakere i EØS/Sveits");
        journalfoeringDto.setHoveddokument(dokumentDto);
        journalfoeringDto.setJournalpostID("526345");
        journalfoeringDto.setOppgaveID("1");
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
        TrygdeAvtaleDataForVedtakDto trygdeAvtaleDataForVedtakDto = new TrygdeAvtaleDataForVedtakDto.Builder()
            .virksomheter(List.of("11111111111"))
            .vedtak("JA_FATTE_VEDTAK")
            .innvilgelse("JA")
            .bestemmelse("UK_ART6_1")
            .addBarn("0bad5c70-8a3f-4fc7-9031-d3aebd6b68de",
                false, Medfolgende_barn_begrunnelser_ftrl.OVER_18_AR.getKode(),
                "begrunnelse barn")
            .ektefelle("1212121212121-4fc7-9031-ab34332121ff",
                false,  Medfolgende_ektefelle_samboer_begrunnelser_ftrl.EGEN_INNTEKT.getKode(),
                "begrunnelse samboer")
            .build();

        trygdeavtaleTjeneste.overforDataForVedtak(2L, trygdeAvtaleDataForVedtakDto);

        FattVedtakRequest fattVedtakRequest = new FattTrygdeavtaleVedtakRequest
            .Builder()
            .medBestillersId(SubjectHandler.getInstance().getUserID())
            .medVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK)
            .medFritekstBegrunnelse("trygdeavtale begrunnelse")
            .build();

        vedtakServiceFasade.fattVedtak(2L, fattVedtakRequest);
        Thread.sleep(3000);
    }
}
