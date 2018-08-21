package no.nav.melosys.service.oppgave;


import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.Oppgavetype;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.service.oppgave.dto.BehandlingDto;
import no.nav.melosys.service.oppgave.dto.OppgaveDto;
import no.nav.melosys.service.oppgave.dto.PeriodeDto;
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

    @Autowired
    public OppgaveService(GsakFasade gsakFasade,
                          FagsakRepository fagsakRepository,
                          TpsFasade tpsFasade) {
        this.gsakFasade = gsakFasade;
        this.fagsakRepository = fagsakRepository;
        this.tpsFasade = tpsFasade;
    }

    @Transactional
    public List<OppgaveDto> hentOppgaverMedAnsvarlig(String ansvarligID) throws TekniskException {
        List<Oppgave> oppgaverFraDomain = gsakFasade.finnOppgaveListeMedAnsvarlig(ansvarligID);
        return oppgaverTilDtoer(oppgaverFraDomain);
    }

    @Transactional
    public List<OppgaveDto> hentOppgaverMedBruker(String brukerIdent) throws IkkeFunnetException, TekniskException {
        String aktørId = tpsFasade.hentAktørIdForIdent(brukerIdent);
        if (aktørId == null) {
            throw new IkkeFunnetException("Finnes ikke aktørId for FNR " + brukerIdent);
        }
        List<Oppgave> oppgaverFraDomain = gsakFasade.finnOppgaveListeMedBruker(aktørId);
        return oppgaverTilDtoer(oppgaverFraDomain);
    }

    private List<OppgaveDto> oppgaverTilDtoer(List<Oppgave> oppgaverFraDomain) {
        return oppgaverFraDomain.stream().map(this::tilOppgaveDto).collect(Collectors.toList());
    }

    private OppgaveDto tilOppgaveDto(Oppgave oppgave) {
        OppgaveDto dest = new OppgaveDto();
        dest.setOppgaveID(oppgave.getOppgaveId());
        dest.setAktivTil(oppgave.getFristFerdigstillelse());
        dest.setOppgavetype(oppgave.getOppgavetype());
        dest.setPrioritet(oppgave.getPrioritet());
        dest.setVersjon(oppgave.getVersjon());
        dest.setAnsvarligId(oppgave.getTilordnetRessurs());

        if (oppgave.erJournalFøring()) {
            dest.setOppgavetype(Oppgavetype.JFR);
            dest.setJournalpostID(oppgave.getJournalpostId());
        } else if (oppgave.erBehandling()) {
            dest.setOppgavetype(Oppgavetype.BEH_SAK);

            Fagsak fagsak = fagsakRepository.findByGsakSaksnummer(oppgave.getGsakSaksnummer());
            if (fagsak == null) {
                throw new RuntimeException("Fagsak med Gsak saksnummer " + oppgave.getGsakSaksnummer() + " ikke funnet!");
            }

            dest.setSaksnummer(fagsak.getSaksnummer());
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
            throw new TekniskException("Oppgavetype " + oppgave.getOppgavetype() + " støttes ikke");
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

    // FIXME For å teste journalføring. Må fjernes.
    public String opprettOppgave(String ansvarligID, String oppgavetype, String brukerID, String journalpostID, String saksnummer) {
        return gsakFasade.opprettOppgave(ansvarligID, oppgavetype, brukerID, journalpostID, saksnummer);
    }
}
