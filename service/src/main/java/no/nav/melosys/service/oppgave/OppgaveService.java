package no.nav.melosys.service.oppgave;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingStatus;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Oppgave;
import no.nav.melosys.domain.Oppgavetype;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.service.oppgave.dto.BehandlingDto;
import no.nav.melosys.service.oppgave.dto.OppgaveDto;
import no.nav.melosys.service.oppgave.dto.PeriodeDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OppgaveService {

    private GsakFasade gsakFasade;
    private FagsakRepository fagsakRepository;

    @Autowired
    public OppgaveService(GsakFasade gsakFasade,
                          FagsakRepository fagsakRepository) {
        this.gsakFasade = gsakFasade;
        this.fagsakRepository = fagsakRepository;
    }

    @Transactional
    public List<OppgaveDto> hentOppgaver(String ansvarligID) {
        List<Oppgave> oppgaverFraDomain = gsakFasade.finnOppgaveListe(ansvarligID);
        return oppgaverTilMineSaker(oppgaverFraDomain);
    }

    private List<OppgaveDto> oppgaverTilMineSaker(List<Oppgave> oppgaverFraDomain) {
        return oppgaverFraDomain.stream().map(oppgave -> tilOppgaveDto(oppgave)).collect(Collectors.toList());
    }

    private OppgaveDto tilOppgaveDto(Oppgave oppgave) {
        OppgaveDto dest = new OppgaveDto();
        dest.setOppgaveID(oppgave.getOppgaveId());
        dest.setAktivTil(oppgave.getAktivTil());

        if (oppgave.erJournalFøring()) {
            dest.setOppgavetype(Oppgavetype.JFR);
            dest.setJournalpostID(oppgave.getDokumentId());
        } else if (oppgave.erBehandling()) {
            dest.setOppgavetype(Oppgavetype.BEH_SAK);

            Fagsak fagsak = fagsakRepository.findByGsakSaksnummer(oppgave.getGsakSaksnummer());
            if (fagsak == null) {
                throw new RuntimeException("Fagsak med Gsak saksnummer " + oppgave.getGsakSaksnummer() + " ikke funnet!");
            }
            List<Behandling> behandlinger = fagsak.getBehandlinger();
            // FIXME MELOSYS-1119 logisk ID for Fagsak
            dest.setSaksnummer(""+fagsak.getId());

            ekstraktSokenadDokument(behandlinger, SaksopplysningType.SØKNAD).ifPresent(saksopplysningDokument -> {
                SoeknadDokument søknadDokument = (SoeknadDokument) saksopplysningDokument;
                dest.setLand(mapLand(søknadDokument));
                dest.setSoknadsperiode(mapDato(søknadDokument));
            });
            ekstraktSokenadDokument(behandlinger, SaksopplysningType.PERSONOPPLYSNING).ifPresent(
                    saksopplysningDokument -> {
                        PersonDokument personDokument = (PersonDokument) saksopplysningDokument;
                        dest.setSammensattNavn(personDokument.sammensattNavn);
                    }
            );

            dest.setBehandling(mapSaksTypeOgBehandling(fagsak));
            dest.setSakstype(fagsak.getType());
        } else {
            throw new RuntimeException("Oppgavetype " + oppgave.getOppgavetype() + " støttes ikke");
        }

        return dest;
    }

    private Optional<SaksopplysningDokument> ekstraktSokenadDokument(List<Behandling> behandlinger, SaksopplysningType saksopplysningType) {
        return behandlinger.stream().flatMap(behandling -> behandling.getSaksopplysninger().stream()).filter(
                saksopplysning -> saksopplysning.getType().equals(saksopplysningType)).findFirst().map(Saksopplysning::getDokument);
    }

    private static BehandlingDto mapSaksTypeOgBehandling(Fagsak fagsak) {
        BehandlingDto behandlingDto = new BehandlingDto();
        List<Behandling> aktivBehandlinger = fagsak.getBehandlinger().stream().
                filter(varBehandling -> !varBehandling.getStatus().equals(BehandlingStatus.AVSLUTTET)).collect(Collectors.toList());
        if (aktivBehandlinger.size() > 1) {
            throw new RuntimeException("Det finnes mer enn en aktive behandlinger");
        } else if (aktivBehandlinger.size() == 1) {
            Behandling aktivBehandling = aktivBehandlinger.get(0);
            behandlingDto.setStatus(aktivBehandling.getStatus());
            behandlingDto.setType(aktivBehandling.getType());
        } else{
            throw new RuntimeException("Det finnes ingen aktive behandlinger");
        }
        return behandlingDto;
    }

    private static PeriodeDto mapDato(SoeknadDokument soeknadDokument) {
        Optional<Periode> arbeidsperiode = Optional.ofNullable(soeknadDokument.arbeidUtland.arbeidsperiode);
        Optional<Periode> oppholdsPeriode = Optional.ofNullable(soeknadDokument.oppholdUtland.oppholdsPeriode);
        if (arbeidsperiode.isPresent()) {
            return new PeriodeDto(arbeidsperiode.get().getFom(), arbeidsperiode.get().getTom());
        } else if (oppholdsPeriode.isPresent()) {
            return new PeriodeDto(oppholdsPeriode.get().getFom(), oppholdsPeriode.get().getTom());
        }
        throw new RuntimeException("Det finnes ikke noen arbeidsperiode eller oppholdsPeriode");
    }

    private static List<String> mapLand(SoeknadDokument soeknadDokument) {
        List<String> landkoder = new ArrayList<>();
        Optional<List<Land>> landListe = Optional.ofNullable(soeknadDokument.arbeidUtland.arbeidsland);
        landListe.ifPresent(lands -> landkoder.addAll(soeknadDokument.arbeidUtland.arbeidsland.stream().filter(Objects::nonNull).map(Land::getKode).collect(Collectors.toList())));
        landListe = Optional.ofNullable(soeknadDokument.oppholdUtland.oppholdsland);
        landListe.ifPresent(lands -> landkoder.addAll(soeknadDokument.oppholdUtland.oppholdsland.stream().filter(Objects::nonNull).map(Land::getKode).collect(Collectors.toList())));
        return landkoder;
    }
}
