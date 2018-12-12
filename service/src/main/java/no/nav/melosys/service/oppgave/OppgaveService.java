package no.nav.melosys.service.oppgave;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.oppgave.dto.*;
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
    private final SaksopplysningerService saksopplysningerService;

    @Autowired
    public OppgaveService(GsakFasade gsakFasade,
                          FagsakRepository fagsakRepository,
                          TpsFasade tpsFasade,
                          SaksopplysningerService saksopplysningerService) {
        this.gsakFasade = gsakFasade;
        this.fagsakRepository = fagsakRepository;
        this.tpsFasade = tpsFasade;
        this.saksopplysningerService = saksopplysningerService;
    }

    @Transactional
    public List<OppgaveDto> hentOppgaverMedAnsvarlig(String ansvarligID) throws TekniskException, FunksjonellException {
        List<Oppgave> oppgaverFraDomain = gsakFasade.finnOppgaveListeMedAnsvarlig(ansvarligID);
        return oppgaverTilDtoer(oppgaverFraDomain);
    }

    @Transactional
    public void ferdigstillOppgaverforAnsvarlig(String ansvarligID) throws TekniskException, FunksjonellException {
        List<Oppgave> oppgaverFraDomain = gsakFasade.finnOppgaveListeMedAnsvarlig(ansvarligID);
        for (Oppgave oppgave : oppgaverFraDomain) {
            gsakFasade.ferdigstillOppgave(oppgave.getOppgaveId());
        }
    }

    @Transactional
    public List<OppgaveDto> hentOppgaverMedBruker(String brukerIdent) throws TekniskException, FunksjonellException {
        String aktørId = tpsFasade.hentAktørIdForIdent(brukerIdent);
        if (aktørId == null) {
            throw new IkkeFunnetException("Finnes ikke aktørId for FNR " + brukerIdent);
        }
        List<Oppgave> oppgaverFraDomain = gsakFasade.finnOppgaveListeMedBruker(aktørId);
        return oppgaverTilDtoer(oppgaverFraDomain);
    }

    @Transactional
    public List<BehandlingsoppgaveDto> hentBehandlingsoppgaverMedBruker(String brukerIdent) throws FunksjonellException, TekniskException {
        String aktørId = tpsFasade.hentAktørIdForIdent(brukerIdent);
        if (aktørId == null) {
            throw new IkkeFunnetException("Finnes ikke aktørId for FNR " + brukerIdent);
        }
        List<Oppgave> oppgaverFraDomain = gsakFasade.finnBehandlingsoppgaverMedBruker(aktørId);
        return oppgaverTilDtoer(oppgaverFraDomain).stream()
                .map(oppgave -> (BehandlingsoppgaveDto) oppgave)
                .collect(Collectors.toList());
    }

    public void avsluttOppgave(String oppgaveID) throws FunksjonellException, TekniskException {
        gsakFasade.ferdigstillOppgave(oppgaveID);
    }

    public Oppgave finnOppgaveMedFagSaksnummer(String fagSaksnummer) throws FunksjonellException, TekniskException {
        return  gsakFasade.finnOppgaveMedSaksnummer(fagSaksnummer);
    }

    private List<OppgaveDto> oppgaverTilDtoer(List<Oppgave> oppgaverFraDomain) throws TekniskException {
        List<OppgaveDto> res = new ArrayList<>();
        for (Oppgave o : oppgaverFraDomain) {
            res.add(tilOppgaveDto(o));
        }
        return res;
    }

    private OppgaveDto tilOppgaveDto(Oppgave oppgave) throws TekniskException {
        OppgaveDto dest;

        if (oppgave.erJournalFøring()) {
            JournalfoeringsoppgaveDto jfrOppgaveDto = new JournalfoeringsoppgaveDto();
            jfrOppgaveDto.setJournalpostID(oppgave.getJournalpostId());
            dest = jfrOppgaveDto;
        } else if (oppgave.erBehandling()) {
            BehandlingsoppgaveDto behOppgaveDto = new BehandlingsoppgaveDto();
            Fagsak fagsak = fagsakRepository.findBySaksnummer(oppgave.getSaksnummer());
            if (fagsak == null) {
                throw new TekniskException("Fagsak med saksnummer " + oppgave.getSaksnummer() + " ikke funnet!");
            }

            behOppgaveDto.setSaksnummer(fagsak.getSaksnummer());
            behOppgaveDto.setSakstype(fagsak.getType());

            Behandling behandling = fagsak.getAktivBehandling();
            if (behandling == null) {
                throw new TekniskException("Det finnes ingen aktiv behandling for " + fagsak.getSaksnummer() + ".");
            }
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
        dest.setPrioritet(oppgave.getPrioritet());
        dest.setVersjon(oppgave.getVersjon());

        return dest;
    }

    private BehandlingDto mapBehandling(Behandling behandling) {
        BehandlingDto behandlingDto = new BehandlingDto();
        behandlingDto.setBehandlingID(behandling.getId());
        behandlingDto.setBehandlingsstatus(behandling.getStatus());
        behandlingDto.setBehandlingstype(behandling.getType());
        behandlingDto.setSisteOpplysningerHentetDato(behandling.getSistOpplysningerHentetDato());
        behandlingDto.setErUnderOppdatering(saksopplysningerService.harAktivOppfrisking(behandling.getId()));
        return behandlingDto;
    }

    private static PeriodeDto mapPeriode(SoeknadDokument soeknadDokument) {
        Periode periode = hentPeriode(soeknadDokument);
        return new PeriodeDto(periode.getFom(), periode.getTom());
    }
}
