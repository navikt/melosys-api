package no.nav.melosys.service;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
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
import no.nav.melosys.service.oppgave.dto.KodeverdiDto;
import no.nav.melosys.service.oppgave.dto.PeriodeDto;
import no.nav.melosys.service.oppgave.dto.SakOgOppgaveDto;
import no.nav.melosys.service.oppgave.dto.Sakstype;
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

    private static Sakstype mappeSaksTypeOgBehandling(Fagsak fagsak) {
        Sakstype sakstype = new Sakstype();
        sakstype.setStatus(new KodeverdiDto(fagsak.getStatus().getKode(), fagsak.getStatus().getBeskrivelse()));
        List<Behandling> aktivBehandlinger = fagsak.getBehandlinger().stream().filter(behandling -> !behandling.getStatus().equals(BehandlingStatus.AVSLUTTET)).collect(Collectors.toList());
        if (aktivBehandlinger.size() > 1) {
            throw new RuntimeException("Finnes mer en to aktiv behandlinger");
        } else {
            sakstype.setBehandlingDto(new KodeverdiDto(aktivBehandlinger.get(0).getStatus().getKode(), aktivBehandlinger.get(0).getStatus().getBeskrivelse()));
        }
        sakstype.setFagSakType(new KodeverdiDto(fagsak.getType().getKode(), fagsak.getType().getBeskrivelse()));
        return sakstype;
    }

    private static PeriodeDto mappeDato(SoeknadDokument soeknadDokument) {
        Optional<Periode> arbeidsperiode = Optional.ofNullable(soeknadDokument.arbeidUtland.arbeidsperiode);
        Optional<Periode> oppholdsPeriode = Optional.ofNullable(soeknadDokument.oppholdUtland.oppholdsPeriode);
        if (arbeidsperiode.isPresent()) {
            return new PeriodeDto(arbeidsperiode.get().getFom(), arbeidsperiode.get().getTom());
        } else if (oppholdsPeriode.isPresent()) {
            return new PeriodeDto(oppholdsPeriode.get().getFom(), oppholdsPeriode.get().getTom());
        }
        throw new RuntimeException("Finnes ikke noen Arbeidsperiode ellers oppholdsPeriode");
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
            List<Behandling> behandlinger = behandlingRepository.findBySaksnummer(oppgave.getGsakSaksnummer());
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
            dest.setSakstype(mappeSaksTypeOgBehandling(fagsakRepository.findByGsakSaksnummer(oppgave.getGsakSaksnummer())));
            return dest;
        }).
                collect(Collectors.<SakOgOppgaveDto>toList());
    }

    private Optional<SaksopplysningDokument> ekstraktSokenadDokument(List<Behandling> behandlinger, SaksopplysningType saksopplysningType) {
        return behandlinger.stream().flatMap(behandling -> behandling.getSaksopplysninger().stream()).filter(
                saksopplysning -> saksopplysning.getType().equals(
                        saksopplysningType)).findFirst().map(Saksopplysning::getDokument);
    }
}
