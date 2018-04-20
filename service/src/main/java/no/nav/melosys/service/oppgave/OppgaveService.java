package no.nav.melosys.service.oppgave;


import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Oppgave;
import no.nav.melosys.domain.Oppgavetype;
import no.nav.melosys.domain.SaksopplysningType;
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

import static no.nav.melosys.domain.util.SaksopplysningerUtil.hentDokument;
import static no.nav.melosys.domain.util.SoeknadUtil.hentLand;
import static no.nav.melosys.domain.util.SoeknadUtil.hentPeriode;

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
            // FIXME MELOSYS-1119 logisk ID for Fagsak
            dest.setSaksnummer(""+fagsak.getId());
            dest.setSakstype(fagsak.getType());

            Behandling behandling = fagsak.getAktivBehandling();
            dest.setBehandling(mapBehandling(behandling));

            hentDokument(behandling, SaksopplysningType.SØKNAD).ifPresent(saksopplysningDokument -> {
                SoeknadDokument søknadDokument = (SoeknadDokument) saksopplysningDokument;
                dest.setLand(hentLand(søknadDokument));
                dest.setSoknadsperiode(mapPeriode(søknadDokument));
            });
            hentDokument(behandling, SaksopplysningType.PERSONOPPLYSNING).ifPresent(
                    saksopplysningDokument -> {
                        PersonDokument personDokument = (PersonDokument) saksopplysningDokument;
                        dest.setSammensattNavn(personDokument.sammensattNavn);
                    }
            );

        } else {
            throw new RuntimeException("Oppgavetype " + oppgave.getOppgavetype() + " støttes ikke");
        }

        return dest;
    }

    private static BehandlingDto mapBehandling(Behandling behandling) {
        BehandlingDto behandlingDto = new BehandlingDto();
        if (behandling == null) {
            throw new RuntimeException("Det finnes ingen aktive behandlinger");
        } else {
            behandlingDto.setStatus(behandling.getStatus());
            behandlingDto.setType(behandling.getType());
        }
        return behandlingDto;
    }

    private static PeriodeDto mapPeriode(SoeknadDokument soeknadDokument) {
        Periode periode = hentPeriode(soeknadDokument);
        return new PeriodeDto(periode.getFom(), periode.getTom());
    }
}
