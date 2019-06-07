package no.nav.melosys.service.oppgave;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.oppgave.dto.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.util.SaksopplysningerUtils.hentDokument;
import static no.nav.melosys.domain.util.SoeknadUtils.hentPeriode;
import static no.nav.melosys.domain.util.SoeknadUtils.hentSøknadsland;

@Service
public class OppgaveService {

    private static final Logger log = LoggerFactory.getLogger(OppgaveService.class);

    private final GsakFasade gsakFasade;
    private final FagsakRepository fagsakRepository;
    private final BehandlingRepository behandlingRepository;
    private final TpsFasade tpsFasade;
    private final SaksopplysningerService saksopplysningerService;
    private static final String UKJENT = "UKJENT";

    @Autowired
    public OppgaveService(GsakFasade gsakFasade,
                          FagsakRepository fagsakRepository,
                          BehandlingRepository behandlingRepository,
                          TpsFasade tpsFasade,
                          SaksopplysningerService saksopplysningerService) {
        this.gsakFasade = gsakFasade;
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.tpsFasade = tpsFasade;
        this.saksopplysningerService = saksopplysningerService;
    }

    public List<OppgaveDto> hentOppgaverMedAnsvarlig(String ansvarligID) throws TekniskException, FunksjonellException {
        List<Oppgave> oppgaverFraDomain = gsakFasade.finnOppgaveListeMedAnsvarlig(ansvarligID);
        return oppgaverTilDtoer(oppgaverFraDomain);
    }

    public void ferdigstillOppgaverforAnsvarlig(String ansvarligID) throws TekniskException, FunksjonellException {
        List<Oppgave> oppgaverFraDomain = gsakFasade.finnOppgaveListeMedAnsvarlig(ansvarligID);
        for (Oppgave oppgave : oppgaverFraDomain) {
            gsakFasade.ferdigstillOppgave(oppgave.getOppgaveId());
        }
    }

    public void ferdigstillOppgave(String oppgaveID) throws FunksjonellException, TekniskException {
        log.info("Ferdigstiller oppgave {}", oppgaveID);
        gsakFasade.ferdigstillOppgave(oppgaveID);
    }

    public void ferdigstillOppgaveMedSaksnummer(String fagSaksnummer) throws FunksjonellException, TekniskException {
        Oppgave oppgave = hentOppgaveMedFagsaksnummer(fagSaksnummer).
            orElseThrow(() -> new TekniskException("Ingen oppgave funnet for fagsak " + fagSaksnummer));
        ferdigstillOppgave(oppgave.getOppgaveId());
    }

    public void leggTilbakeOppgaveMedSaksnummer(String fagSaksnummer) throws FunksjonellException, TekniskException {
        Oppgave oppgave = hentOppgaveMedFagsaksnummer(fagSaksnummer).
            orElseThrow(() -> new TekniskException("Ingen oppgave funnet for fagsak " + fagSaksnummer));
        gsakFasade.leggTilbakeOppgave(oppgave.getOppgaveId());
    }

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

    public Optional<Oppgave> hentOppgaveMedFagsaksnummer(String saksnummer) throws FunksjonellException, TekniskException {
        return gsakFasade.finnOppgaveMedSaksnummer(saksnummer);
    }

    public Long hentAktivBehandlingId(String saksnummer) throws TekniskException {
        Fagsak fagsak = Optional.ofNullable(fagsakRepository.findBySaksnummer(saksnummer))
            .orElseThrow(() -> new TekniskException("Fagsak med saksnummer " + saksnummer + " ikke funnet"));
        return Optional.ofNullable(fagsak.getAktivBehandling())
            .orElseThrow(() -> new TekniskException("Fagsak med saksnummer " + saksnummer + " har ingen aktive behandlinger"))
            .getId();
    }

    private List<OppgaveDto> oppgaverTilDtoer(List<Oppgave> oppgaverFraDomain) throws TekniskException, FunksjonellException {
        List<OppgaveDto> res = new ArrayList<>();
        for (Oppgave o : oppgaverFraDomain) {
            res.add(tilOppgaveDto(o));
        }
        return res;
    }

    private OppgaveDto tilOppgaveDto(Oppgave oppgave) throws TekniskException, FunksjonellException {
        OppgaveDto dest;

        if (oppgave.erJournalFøring()) {
            JournalfoeringsoppgaveDto jfrOppgaveDto = new JournalfoeringsoppgaveDto();
            jfrOppgaveDto.setJournalpostID(oppgave.getJournalpostId());
            dest = jfrOppgaveDto;
            String aktørId = oppgave.getAktørId();
            String fnr = aktørId != null ? tpsFasade.hentIdentForAktørId(aktørId) : null;
            if (StringUtils.isNotEmpty(fnr)){
                dest.setFnr(fnr);
                dest.setSammensattNavn(tpsFasade.hentSammensattNavn(fnr));
            }
            else {
                dest.setFnr(UKJENT);
                dest.setSammensattNavn(UKJENT);
            }
        } else if (oppgave.erBehandling() || oppgave.erVurderDokument()) {
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
            // Henter saksopplysninger
            behandling = behandlingRepository.findWithSaksopplysningerById(behandling.getId());

            behOppgaveDto.setBehandling(mapBehandling(behandling));

            hentDokument(behandling, SaksopplysningType.SØKNAD).ifPresent(saksopplysningDokument -> {
                SoeknadDokument søknadDokument = (SoeknadDokument) saksopplysningDokument;
                behOppgaveDto.setLand(hentSøknadsland(søknadDokument));
                behOppgaveDto.setSoknadsperiode(mapPeriode(søknadDokument));
            });
            hentDokument(behandling, SaksopplysningType.PERSOPL).ifPresent(
                saksopplysningDokument -> {
                    PersonDokument personDokument = (PersonDokument) saksopplysningDokument;
                    behOppgaveDto.setSammensattNavn(personDokument.sammensattNavn);
                    behOppgaveDto.setFnr(personDokument.fnr);
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
        behandlingDto.setRegistrertDato(behandling.getRegistrertDato());
        behandlingDto.setSisteOpplysningerHentetDato(behandling.getSistOpplysningerHentetDato());
        behandlingDto.setErUnderOppdatering(saksopplysningerService.harAktivOppfrisking(behandling.getId()));
        return behandlingDto;
    }

    private static PeriodeDto mapPeriode(SoeknadDokument soeknadDokument) {
        Periode periode = hentPeriode(soeknadDokument);
        return new PeriodeDto(periode.getFom(), periode.getTom());
    }
}
