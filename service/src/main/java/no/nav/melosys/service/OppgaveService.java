package no.nav.melosys.service;


import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Oppgave;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.service.oppgave.converter.OppgaverDomainTilOppgaveogSakDtoConverterUtil;
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
            List<Behandling> behandlinger = behandlingRepository.findBySaksnummer(oppgave.getSaksnummer());
            dest.setSaksnummer(oppgave.getSaksnummer());
            SoeknadDokument søknadDokument = (SoeknadDokument) ekstraktSokenadDokument(behandlinger, SaksopplysningType.SØKNAD).get();
            PersonDokument personDokument = (PersonDokument) ekstraktSokenadDokument(behandlinger, SaksopplysningType.PERSONOPPLYSNING).get();
            dest.setSammensattNavn(personDokument.sammensattNavn);
            dest.setLand(OppgaverDomainTilOppgaveogSakDtoConverterUtil.mappeLander(søknadDokument));
            dest.setSoknadsperiode(OppgaverDomainTilOppgaveogSakDtoConverterUtil.mappeDato(søknadDokument));
            dest.setSaksType(OppgaverDomainTilOppgaveogSakDtoConverterUtil.mappeSaksTypeOgBehandling(fagsakRepository.findByGsakSaksnummer(oppgave.getSaksnummer())));
            return dest;
        }).
                collect(Collectors.<SakOgOppgaveDto>toList());
    }

    private Optional<SaksopplysningDokument> ekstraktSokenadDokument(List<Behandling> behandlinger, SaksopplysningType saksopplysningType) {
        return behandlinger.stream().flatMap(behandling -> behandling.getSaksopplysninger().stream()).filter(
                saksopplysning -> saksopplysning.getType().equals(
                        saksopplysningType)).findFirst().map(saksopplysning -> saksopplysning.getDokument());
    }
}
