package no.nav.melosys.service.oppgave;


import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.service.oppgave.dto.*;
import no.nav.melosys.repository.ProsessinstansRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.util.SaksopplysningerUtils.hentDokument;
import static no.nav.melosys.domain.util.SoeknadUtils.hentLand;
import static no.nav.melosys.domain.util.SoeknadUtils.hentPeriode;

@Service
public class OppgaveService {

    private final GsakFasade gsakFasade;
    private final FagsakRepository fagsakRepository;
    private final TpsFasade tpsFasade;
    private final ProsessinstansRepository prosessinstansRepository;
    private final OppgaveTilgang oppgaveTilgang;

    @Autowired
    public OppgaveService(GsakFasade gsakFasade,
                          FagsakRepository fagsakRepository,
                          TpsFasade tpsFasade,
                          ProsessinstansRepository prosessinstansRepository,
                          OppgaveTilgang oppgaveTilgang) {
        this.gsakFasade = gsakFasade;
        this.fagsakRepository = fagsakRepository;
        this.tpsFasade = tpsFasade;
        this.prosessinstansRepository = prosessinstansRepository;
        this.oppgaveTilgang = oppgaveTilgang;
    }

    @Transactional
    public List<OppgaveDto> hentOppgaverMedAnsvarlig(String ansvarligID) throws TekniskException {
        List<Oppgave> oppgaverFraDomain = gsakFasade.finnOppgaveListeMedAnsvarlig(ansvarligID);
        oppgaverFraDomain.removeIf(o -> oppgaveTilgang.harIkkeTilgangTil(o));
        return oppgaverTilDtoer(oppgaverFraDomain);
    }

    @Transactional
    public List<OppgaveDto> hentOppgaverMedBruker(String brukerIdent) throws IkkeFunnetException, TekniskException {
        String aktørId = tpsFasade.hentAktørIdForIdent(brukerIdent);
        if (aktørId == null) {
            throw new IkkeFunnetException("Finnes ikke aktørId for FNR " + brukerIdent);
        }
        List<Oppgave> oppgaverFraDomain = gsakFasade.finnOppgaveListeMedBruker(aktørId);
        oppgaverFraDomain.removeIf(o -> oppgaveTilgang.harIkkeTilgangTil(o));
        return oppgaverTilDtoer(oppgaverFraDomain);
    }

    private List<OppgaveDto> oppgaverTilDtoer(List<Oppgave> oppgaverFraDomain) {
        return oppgaverFraDomain.stream().map(this::tilOppgaveDto).collect(Collectors.toList());
    }

    private OppgaveDto tilOppgaveDto(Oppgave oppgave) {
        OppgaveDto dest;

        if (oppgave.erJournalFøring()) {
            JournalfoeringsoppgaveDto jfrOppgaveDto = new JournalfoeringsoppgaveDto();
            jfrOppgaveDto.setJournalpostID(oppgave.getJournalpostId());
            dest = jfrOppgaveDto;
        } else if (oppgave.erBehandling()) {
            BehandlingsoppgaveDto behOppgaveDto = new BehandlingsoppgaveDto();
            Fagsak fagsak = fagsakRepository.findByGsakSaksnummer(oppgave.getGsakSaksnummer());
            if (fagsak == null) {
                throw new RuntimeException("Fagsak med Gsak saksnummer " + oppgave.getGsakSaksnummer() + " ikke funnet!");
            }

            behOppgaveDto.setSaksnummer(fagsak.getSaksnummer());
            behOppgaveDto.setSakstypeKode(fagsak.getType().getKode());

            Behandling behandling = fagsak.getAktivBehandling();
            behOppgaveDto.setBehandling(mapBehandling(behandling));

            hentDokument(behandling, SaksopplysningType.SØKNAD).ifPresent(saksopplysningDokument -> {
                SoeknadDokument søknadDokument = (SoeknadDokument) saksopplysningDokument;
                behOppgaveDto.setLand(hentLand(søknadDokument));
                behOppgaveDto.setSoknadsperiode(mapPeriode(søknadDokument));
            });
            hentDokument(behandling, SaksopplysningType.PERSONOPPLYSNING).ifPresent(
                saksopplysningDokument -> {
                    PersonDokument personDokument = (PersonDokument) saksopplysningDokument;
                    behOppgaveDto.setSammensattNavn(personDokument.sammensattNavn);
                }
            );

            dest = behOppgaveDto;
        } else {
            throw new TekniskException("Oppgavetype " + oppgave.getOppgavetype() + " støttes ikke");
        }

        dest.setAktivTil(oppgave.getFristFerdigstillelse());
        dest.setAnsvarligID(oppgave.getTilordnetRessurs());
        dest.setOppgaveID(oppgave.getOppgaveId());
        dest.setOppgavetypeKode(oppgave.getOppgavetype().getKode());
        dest.setPrioritet(oppgave.getPrioritet());
        dest.setVersjon(oppgave.getVersjon());

        return dest;
    }

    private BehandlingDto mapBehandling(Behandling behandling) {
        BehandlingDto behandlingDto = new BehandlingDto();
        if (behandling == null) {
            throw new RuntimeException("Det finnes ingen aktive behandlinger");
        } else {
            behandlingDto.setBehandlingID(behandling.getId());
            behandlingDto.setBehandlingStatus(behandling.getStatus());
            behandlingDto.setBehandlingType(behandling.getType());
            behandlingDto.setEndretDato(behandling.getEndretDato());
            Optional<Prosessinstans> prosessinstans = prosessinstansRepository.findByStegIsNotNullAndTypeAndBehandling_Id(ProsessType.OPPFRISKNING, behandling.getId());
            if (prosessinstans.isPresent()) {
                behandlingDto.setErUnderOppdatering(true);
            } else {
                behandlingDto.setErUnderOppdatering(false);
            }
        }
        return behandlingDto;
    }

    private static PeriodeDto mapPeriode(SoeknadDokument soeknadDokument) {
        Periode periode = hentPeriode(soeknadDokument);
        return new PeriodeDto(periode.getFom(), periode.getTom());
    }
}
