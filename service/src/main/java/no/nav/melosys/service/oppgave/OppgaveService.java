package no.nav.melosys.service.oppgave;


import java.util.*;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.SoeknadService;
import no.nav.melosys.service.oppgave.dto.*;
import no.nav.melosys.service.sak.FagsakService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.util.SoeknadUtils.hentPeriode;
import static no.nav.melosys.domain.util.SoeknadUtils.hentSøknadsland;

@Service
@Primary
public class OppgaveService {
    private static final Logger log = LoggerFactory.getLogger(OppgaveService.class);

    private final BehandlingService behandlingService;
    private final FagsakService fagsakService;
    private final GsakFasade gsakFasade;
    private final SaksopplysningerService saksopplysningerService;
    private final SoeknadService søknadService;
    private final TpsFasade tpsFasade;
    private static final String UKJENT = "UKJENT";

    @Autowired
    public OppgaveService(BehandlingService behandlingService,
                          FagsakService fagsakService,
                          GsakFasade gsakFasade,
                          SaksopplysningerService saksopplysningerService,
                          SoeknadService søknadService,
                          TpsFasade tpsFasade) {
        this.behandlingService = behandlingService;
        this.fagsakService = fagsakService;
        this.gsakFasade = gsakFasade;
        this.saksopplysningerService = saksopplysningerService;
        this.tpsFasade = tpsFasade;
        this.søknadService = søknadService;
    }

    public List<Oppgave> finnOppgaverMedBrukerID(String brukerIdent) throws FunksjonellException, TekniskException {
        String aktørId = tpsFasade.hentAktørIdForIdent(brukerIdent);
        if (aktørId == null) {
            throw new IkkeFunnetException("Finner ikke aktørId for ident " + brukerIdent);
        }
        return gsakFasade.finnOppgaverMedBrukerID(aktørId);
    }

    public List<OppgaveDto> hentOppgaverMedAnsvarlig(String ansvarligID) throws TekniskException, FunksjonellException {
        Collection<Oppgave> oppgaverFraDomain = gsakFasade.finnOppgaveListeMedAnsvarlig(ansvarligID);
        return oppgaverTilDtoer(oppgaverFraDomain);
    }

    public void ferdigstillOppgave(String oppgaveID) throws FunksjonellException, TekniskException {
        log.info("Ferdigstiller oppgave {}", oppgaveID);
        gsakFasade.ferdigstillOppgave(oppgaveID);
    }

    public void ferdigstillOppgaveMedSaksnummer(String fagSaksnummer) throws FunksjonellException, TekniskException {
        Oppgave oppgave;
        try {
            oppgave = hentOppgaveMedFagsaksnummer(fagSaksnummer);
        } catch (IkkeFunnetException e) {
            log.debug("Sak {} har ingen oppgaver å ferdigstille.", fagSaksnummer);
            return;
        }
        ferdigstillOppgave(oppgave.getOppgaveId());
    }

    public void leggTilbakeOppgaveMedSaksnummer(String fagSaksnummer) throws FunksjonellException, TekniskException {
        Oppgave oppgave = hentOppgaveMedFagsaksnummer(fagSaksnummer);
        gsakFasade.leggTilbakeOppgave(oppgave.getOppgaveId());
    }

    public Optional<Oppgave> finnOppgaveMedFagsaksnummer(String saksnummer) throws FunksjonellException, TekniskException {
        try {
            return Optional.of(gsakFasade.hentOppgaveMedSaksnummer(saksnummer));
        } catch (IkkeFunnetException e) {
            log.warn(e.getMessage());
            return Optional.empty();
        }
    }

    public Oppgave hentOppgaveMedFagsaksnummer(String saksnummer) throws FunksjonellException, TekniskException {
        return gsakFasade.hentOppgaveMedSaksnummer(saksnummer);
    }

    public Oppgave hentOppgaveMedOppgaveID(String oppgaveID) throws FunksjonellException, TekniskException {
        return gsakFasade.hentOppgave(oppgaveID);
    }

    public Behandling hentAktivBehandling(String saksnummer) throws IkkeFunnetException, TekniskException {
        Fagsak fagsak = fagsakService.hentFagsak(saksnummer);
        return Optional.ofNullable(fagsak.getAktivBehandling())
            .orElseThrow(() -> new TekniskException("Fagsak med saksnummer " + saksnummer + " har ingen aktive behandlinger"));
    }

    private List<OppgaveDto> oppgaverTilDtoer(Collection<Oppgave> oppgaverFraDomain) throws TekniskException, FunksjonellException {
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
        } else if (oppgave.erBehandling() || oppgave.erVurderDokument() || oppgave.erSedBehandling()) {
            BehandlingsoppgaveDto behOppgaveDto = new BehandlingsoppgaveDto();
            Fagsak fagsak = fagsakService.hentFagsak(oppgave.getSaksnummer());
            behOppgaveDto.setSaksnummer(fagsak.getSaksnummer());
            behOppgaveDto.setSakstype(fagsak.getType());

            Behandling behandling = fagsak.getAktivBehandling();
            if (behandling == null) {
                throw new TekniskException("Det finnes ingen aktiv behandling for " + fagsak.getSaksnummer() + ".");
            }
            behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandling.getId());
            behOppgaveDto.setBehandling(mapBehandling(behandling));

            if (behandling.getType() == Behandlingstyper.SOEKNAD
                || behandling.getType() == Behandlingstyper.SOEKNAD_IKKE_YRKESAKTIV
                || behandling.getType() == Behandlingstyper.ENDRET_PERIODE) {
                SoeknadDokument søknadDokument = søknadService.hentSøknad(behandling.getId());
                behOppgaveDto.setLand(hentSøknadsland(søknadDokument));
                behOppgaveDto.setPeriode(mapPeriode(søknadDokument));
            } else {
                saksopplysningerService.finnSedOpplysninger(behandling.getId()).ifPresent(
                    sedDokument -> {
                        behOppgaveDto.setLand(Collections.singletonList(sedDokument.getLovvalgslandKode() != null
                            ? sedDokument.getLovvalgslandKode().getKode() : null));
                        behOppgaveDto.setPeriode(new PeriodeDto(
                            sedDokument.getLovvalgsperiode().getFom(), sedDokument.getLovvalgsperiode().getTom())
                        );
                    });
            }
            saksopplysningerService.finnPersonOpplysninger(behandling.getId()).ifPresent(
                personDokument -> {
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
        behandlingDto.setEndretDato(behandling.getEndretDato());
        behandlingDto.setSvarFrist(behandling.getDokumentasjonSvarfristDato());
        behandlingDto.setErUnderOppdatering(saksopplysningerService.harAktivOppfrisking(behandling.getId()));
        return behandlingDto;
    }

    private static PeriodeDto mapPeriode(SoeknadDokument soeknadDokument) {
        Periode periode = hentPeriode(soeknadDokument);
        return new PeriodeDto(periode.getFom(), periode.getTom());
    }
}
