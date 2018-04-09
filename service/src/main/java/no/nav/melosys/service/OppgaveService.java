package no.nav.melosys.service;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.melosys.domain.BehandlingStatus;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Oppgave;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.service.oppgave.dto.Behandling;
import no.nav.melosys.service.oppgave.dto.KodeverdiDto;
import no.nav.melosys.service.oppgave.dto.PeriodeDto;
import no.nav.melosys.service.oppgave.dto.SakOgOppgaveDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OppgaveService {

    private GsakFasade gsakFasade;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;

    @Autowired
    public OppgaveService(GsakFasade gsakFasade,
                          FagsakRepository fagsakRepository,
                          BehandlingRepository behandlingRepository) {
        this.gsakFasade = gsakFasade;
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
    }

    private static Behandling mappeSaksTypeOgBehandling(Fagsak fagsak) {
        Behandling behandling = new Behandling();
        behandling.setStatus(new KodeverdiDto(fagsak.getStatus().getKode(), fagsak.getStatus().getBeskrivelse()));
        List<no.nav.melosys.domain.Behandling> aktivBehandlinger = fagsak.getBehandlinger().stream().
                filter(varBehandling -> !varBehandling.getStatus().equals(BehandlingStatus.AVSLUTTET)).
                collect(Collectors.toList());
        if (aktivBehandlinger.size() > 1) {
            throw new RuntimeException("Det finnes mer enn en aktive behandlinger");
        } else if (aktivBehandlinger.size() == 1) {
            behandling.setType((new KodeverdiDto(aktivBehandlinger.get(0).getStatus().getKode(), aktivBehandlinger.get(0).getStatus().getBeskrivelse())));
        } else{
            throw new RuntimeException("Det finnes ingen aktive behandlinger");
        }
        return behandling;
    }

    private static PeriodeDto mappeDato(SoeknadDokument soeknadDokument) {
        Optional<Periode> arbeidsperiode = Optional.ofNullable(soeknadDokument.arbeidUtland.arbeidsperiode);
        Optional<Periode> oppholdsPeriode = Optional.ofNullable(soeknadDokument.oppholdUtland.oppholdsPeriode);
        if (arbeidsperiode.isPresent()) {
            return new PeriodeDto(arbeidsperiode.get().getFom(), arbeidsperiode.get().getTom());
        } else if (oppholdsPeriode.isPresent()) {
            return new PeriodeDto(oppholdsPeriode.get().getFom(), oppholdsPeriode.get().getTom());
        }
        throw new RuntimeException("Det finnes ikke noen arbeidsperiode eller oppholdsPeriode");
    }

    private static List<String> mappeLand(SoeknadDokument soeknadDokument) {
        List<String> landkoder = new ArrayList<String>();
        Optional<List<Land>> landListe = Optional.ofNullable(soeknadDokument.arbeidUtland.arbeidsland);
        landListe.ifPresent(lands -> landkoder.addAll(soeknadDokument.arbeidUtland.arbeidsland.stream().filter(Objects::nonNull).map(Land::getKode).collect(Collectors.toList())));
        landListe = Optional.ofNullable(soeknadDokument.oppholdUtland.oppholdsland);
        landListe.ifPresent(lands -> landkoder.addAll(soeknadDokument.oppholdUtland.oppholdsland.stream().filter(Objects::nonNull).map(Land::getKode).collect(Collectors.toList())));
        return landkoder;
    }


    public List<SakOgOppgaveDto> hentMineSaker(String ansvarligID) {
        List<Oppgave> oppgaverFraDomain = gsakFasade.finnOppgaveListe(ansvarligID);
        return mappeOppgaveDtoTilMinSak(oppgaverFraDomain);
    }

    private List<SakOgOppgaveDto> mappeOppgaveDtoTilMinSak(List<Oppgave> oppgaverFraDomain) {
        return oppgaverFraDomain.stream().map(oppgave -> {
            SakOgOppgaveDto dest = new SakOgOppgaveDto();
            dest.setOppgaveId(oppgave.getOppgaveId());
            dest.setDokumentID(oppgave.getDokumentId());
            dest.setAktivTil(oppgave.getAktivTil());
            List<no.nav.melosys.domain.Behandling> behandlinger = behandlingRepository.findBySaksnummer(oppgave.getGsakSaksnummer());
            dest.setSaksnummer(oppgave.getGsakSaksnummer());
            ekstraktSokenadDokument(behandlinger, SaksopplysningType.SØKNAD).ifPresent(saksopplysningDokument -> {
                SoeknadDokument søknadDokument = (SoeknadDokument) saksopplysningDokument;
                dest.setLand(mappeLand(søknadDokument));
                dest.setSoknadsperiode(mappeDato(søknadDokument));
            });
            ekstraktSokenadDokument(behandlinger, SaksopplysningType.PERSONOPPLYSNING).ifPresent(
                    saksopplysningDokument -> {
                        PersonDokument personDokument = (PersonDokument) saksopplysningDokument;
                        dest.setSammensattNavn(personDokument.sammensattNavn);
                    }
            );
            Fagsak fagsak = fagsakRepository.findByGsakSaksnummer(oppgave.getGsakSaksnummer());
            dest.setSakstype(new KodeverdiDto(fagsak.getType().getKode(), fagsak.getType().getBeskrivelse()));
            dest.setBehandling(mappeSaksTypeOgBehandling(fagsak));
            return dest;
        }).collect(Collectors.<SakOgOppgaveDto>toList());
    }

    private Optional<SaksopplysningDokument> ekstraktSokenadDokument(List<no.nav.melosys.domain.Behandling> behandlinger, SaksopplysningType saksopplysningType) {
        return behandlinger.stream().flatMap(behandling -> behandling.getSaksopplysninger().stream()).filter(
                saksopplysning -> saksopplysning.getType().equals(
                        saksopplysningType)).findFirst().map(Saksopplysning::getDokument);
    }
}
