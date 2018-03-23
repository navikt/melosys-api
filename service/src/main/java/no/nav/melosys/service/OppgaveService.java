package no.nav.melosys.service;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.aggregate.OppgaveAG;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Oppgave;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.repository.SaksopplysningRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OppgaveService {

    private GsakFasade gsakFasade;
    private FagsakRepository fagsakRepository;
    private SaksopplysningRepository saksopplysningRepo;
    private SoeknadService soeknadService;
    private BehandlingRepository behandlingRepository;
    private String ansvarligEnhetID="4530";
    private String sorteringselementKode_OPPRETTET_DATO = "OPPRETTET_DATO";
    private String sorteringselementKode_FRIST_DATO = "FRIST_DATO";


    private String sorteringKode_OPPRETTET_DATO = "OPPRETTET_DATO";
    private String sorteringKode_FRIST_DATO = "FRIST_DATO";


    @Autowired
    public OppgaveService(GsakFasade gsakFasade,
                          FagsakRepository fagsakRepository,
                          SaksopplysningRepository saksopplysningRepo,
                          SoeknadService soeknadService,
                          BehandlingRepository behandlingRepository) {
        this.gsakFasade = gsakFasade;
        this.fagsakRepository = fagsakRepository;
        this.saksopplysningRepo = saksopplysningRepo;
        this.soeknadService=soeknadService;
        this.behandlingRepository=behandlingRepository;
    }

    public List<OppgaveAG> hentMineSaker(String ansvarligID) {

        List <Oppgave> oppgaverFraDomain = gsakFasade.finnOppgaveListe(ansvarligEnhetID,ansvarligID,ansvarligID,sorteringselementKode_FRIST_DATO,sorteringKode_FRIST_DATO,ansvarligID);

        List<OppgaveAG> oppgaveAGList = new ArrayList<>();
        oppgaverFraDomain.stream().forEach(oppgave -> oppgaveAGList.add(byggOppgaveAG(oppgave)));
        return oppgaveAGList;
    }

    private OppgaveAG byggOppgaveAG(Oppgave oppgave) {
        OppgaveAG oppgaveAG = new OppgaveAG();

        //Set Oppgave
        oppgaveAG.setOppgave(oppgave);

        Long saksnummer = Long.parseLong(oppgave.getSaksnummer());

        //Hent FagSak
        oppgaveAG.setFagsak(fagsakRepository.findByGsakSaksnummer(saksnummer));

        //Hent Dokumenter for soeknad og personal informasjon
        List<Behandling> behandlinger = behandlingRepository.findBySaksnummer(saksnummer);
        oppgaveAG.setSoeknadDokument((SoeknadDokument) ekstraktSokenadDokument(behandlinger, SaksopplysningType.SØKNAD.getKode()).get());
        oppgaveAG.setPersonDokument((PersonDokument) ekstraktSokenadDokument(behandlinger, SaksopplysningType.PERSONOPPLYSNING.getKode()).get());
        return oppgaveAG;
    }

    private Optional<SaksopplysningDokument> ekstraktSokenadDokument(List<Behandling> behandlinger, String saksopplysningType) {
        return behandlinger.stream().flatMap(behandling -> behandling.getSaksopplysninger().stream()).filter(
                saksopplysning -> saksopplysning.getType().getKode().equals(
                        saksopplysningType)).findFirst().map(saksopplysning -> saksopplysning.getDokument());
    }
}
