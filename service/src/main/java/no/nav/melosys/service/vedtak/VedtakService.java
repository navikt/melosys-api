package no.nav.melosys.service.vedtak;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.util.LovvalgBestemmelseUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.integrasjon.gsak.oppgave.dto.OppgaveDto;
import no.nav.melosys.integrasjon.gsak.oppgave.dto.OpprettOppgaveDto;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.oppgave.OppgaveFactory;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import static no.nav.melosys.integrasjon.Konstanter.MELOSYS_ENHET_ID;

@Service
public class VedtakService {
    private static final Logger log = LoggerFactory.getLogger(VedtakService.class);

    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final OppgaveService oppgaveService;
    private final ProsessinstansService prosessinstansService;
    private final EessiService eessiService;
    private final LandvelgerService landvelgerService;
    private final FagsakService fagsakService;
    private final GsakFasade gsakFasade;

    @Autowired
    public VedtakService(BehandlingService behandlingService, BehandlingsresultatService behandlingsresultatService,
                         OppgaveService oppgaveService, ProsessinstansService prosessinstansService,
                         EessiService eessiService, LandvelgerService landvelgerService,
                        FagsakService fagsakService, GsakFasade gsakFasade) {
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.oppgaveService = oppgaveService;
        this.prosessinstansService = prosessinstansService;
        this.eessiService = eessiService;
        this.landvelgerService = landvelgerService;
        this.gsakFasade = gsakFasade;
        this.fagsakService = fagsakService;
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void fattVedtak(long behandlingID, Behandlingsresultattyper behandlingsresultattype) throws MelosysException {
        fattVedtak(behandlingID, behandlingsresultattype, null, null, null, null);
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void fattVedtak(long behandlingID, Behandlingsresultattyper behandlingsresultatType, String fritekst, String mottakerInstitusjon, Vedtakstyper vedtakstype, String revurderBegrunnelse) throws MelosysException {
        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);
        behandlingsresultatService.oppdaterBehandlingsresultattype(behandlingID, behandlingsresultatType);
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        log.info("Fatter vedtak for sak: {} behandling: {}", behandling.getFagsak().getSaksnummer(), behandlingID);

        if (skalSendesSed(behandlingsresultat)) {
            validerMottakerInstitusjon(behandling, behandlingsresultat, mottakerInstitusjon);
        }
        behandling.setStatus(Behandlingsstatus.IVERKSETTER_VEDTAK);
        behandlingService.lagre(behandling);
        prosessinstansService.opprettProsessinstansIverksettVedtak(behandling, behandlingsresultatType, fritekst, mottakerInstitusjon, vedtakstype, revurderBegrunnelse );
        oppgaveService.ferdigstillOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
    }

    private boolean skalSendesSed(Behandlingsresultat behandlingsresultat) {
        if (behandlingsresultat.erAvslag()) {
            return false;
        }
        return !behandlingsresultat.erArt16EtterUtlandMedRegistrertSvar();
    }

    private void validerMottakerInstitusjon(Behandling behandling, Behandlingsresultat behandlingsresultat, String mottakerInstitusjon) throws MelosysException {

        Collection<Landkoder> landkoder = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandling.getId());
        String landkode = landkoder.iterator().next().getKode();
        String bucType = avklarBucType(behandlingsresultat);
        boolean landErEessiReady = eessiService.landErEessiReady(bucType, landkode);
        if (landErEessiReady) {
            if (StringUtils.isEmpty(mottakerInstitusjon)) {
                throw new FunksjonellException(String.format("Kan ikke fatte vedtak: %s er EESSI-ready, men mottaker er ikke satt", landkode));
            } else if (!eessiService.erGyldigInstitusjonForLand(bucType, landkode, mottakerInstitusjon)) {
                throw new FunksjonellException(String.format("MottakerID %s er ugyldig for land %s", mottakerInstitusjon, landkode));
            }
        }
    }

    private String avklarBucType(Behandlingsresultat behandlingsresultat) {
        return LovvalgBestemmelseUtils.hentBucTypeFraBestemmelse(
            behandlingsresultat.hentValidertMedlemskapsperiode().getBestemmelse()
        ).name();
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void endreVedtak(Long behandlingID, Endretperiode endretperiode, Behandlingstyper behandlingstype, String fritekst) throws FunksjonellException, TekniskException {
        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);
        log.info("Endrer vedtak for sak: {} behandling: {}", behandling.getFagsak().getSaksnummer(), behandlingID);

        behandling.setType(behandlingstype);
        behandlingService.lagre(behandling);

        prosessinstansService.opprettProsessinstansForkortPeriode(behandling, endretperiode, fritekst);
        oppgaveService.ferdigstillOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
}

    @Transactional(rollbackFor = MelosysException.class)
    public long revurderVedtak(long behandlingID) throws FunksjonellException, TekniskException {
        Behandling eksisterendeBehandling = behandlingService.hentBehandling(behandlingID);

        validerBehandlingForRevurdering(eksisterendeBehandling);

        log.info("Revurderer vedtak for sak: {} behandling: {}", eksisterendeBehandling.getFagsak().getSaksnummer(), behandlingID);

        Behandling nyBehandling = opprettRevurderingsbehandlingFraEksisterende(eksisterendeBehandling);

        eksisterendeBehandling.getFagsak().setStatus(Saksstatuser.OPPRETTET);
        fagsakService.lagre(eksisterendeBehandling.getFagsak());

        opprettRevurderingsoppgave(eksisterendeBehandling, SubjectHandler.getInstance().getUserID(), nyBehandling);

        return nyBehandling.getId();
    }

    private void validerBehandlingForRevurdering(Behandling eksisterendeBehandling) throws FunksjonellException {
        if (eksisterendeBehandling.isAktiv()) {
            throw new FunksjonellException(String.format("Kan ikke revurdere vedtak på behandling %s for fagsak %s fordi den fortsatt er aktiv", eksisterendeBehandling.getId(), eksisterendeBehandling.getFagsak().getSaksnummer()));
        }

        if (Behandlingstyper.ENDRET_PERIODE == eksisterendeBehandling.getType()) {
            throw new FunksjonellException(String.format("Kan ikke revurdere vedtak på behandling %s for fagsak %s fordi vedtaket er gjort i forbindelse med forkortet periode", eksisterendeBehandling.getId(), eksisterendeBehandling.getFagsak().getSaksnummer()));
        }
    }

    private Behandling opprettRevurderingsbehandlingFraEksisterende(Behandling eksisterendeBehandling) throws IkkeFunnetException, TekniskException {
        try {
            return behandlingService.replikerBehandlingOgBehandlingsresultat(eksisterendeBehandling, Behandlingsstatus.OPPRETTET, Behandlingstyper.NY_VURDERING);
        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            throw new TekniskException(String.format("Klarte ikke replikere behandling %s for fagsak %s", eksisterendeBehandling.getId(), eksisterendeBehandling.getFagsak().getSaksnummer()), e);
        }
    }

    private void opprettRevurderingsoppgave(Behandling eksisterendeBehandling, String saksbehandler, Behandling nyBehandling) throws TekniskException, FunksjonellException {
        Optional<OppgaveDto> dtoOptional = gsakFasade.hentSisteOppgaveDtoForSak(eksisterendeBehandling.getFagsak().getSaksnummer());

        if (dtoOptional.isPresent()) {
            OppgaveDto oppgaveDto = dtoOptional.get();
            oppgaveDto.setTilordnetRessurs(saksbehandler);
            gsakFasade.opprettOppgave(kopierOpprettOppgaveDto(oppgaveDto, saksbehandler));                
        } else {
            Oppgave oppgave = OppgaveFactory.lagBehandlingsOppgaveForType(eksisterendeBehandling.getType())
                    .setJournalpostId(nyBehandling.getInitierendeJournalpostId())
                    .setAktørId(finnAktoerBruker(eksisterendeBehandling.getFagsak().getAktører()).getAktørId())
                    .setSaksnummer(eksisterendeBehandling.getFagsak().getSaksnummer())
                    .setBehandlingstype(nyBehandling.getType())
                    .setTilordnetRessurs(saksbehandler)
                    .build();

            gsakFasade.opprettOppgave(oppgave);
        }
    }

    private OpprettOppgaveDto kopierOpprettOppgaveDto(no.nav.melosys.integrasjon.gsak.oppgave.dto.OppgaveDto eksisterendeOppgave, String saksbehandler) {
        LocalDate idag = LocalDate.now();
        OpprettOppgaveDto oppgaveDto = new OpprettOppgaveDto();

        oppgaveDto.setAktivDato(idag);
        oppgaveDto.setAktørId(eksisterendeOppgave.getAktørId());
        oppgaveDto.setBehandlingstype(Behandlingstyper.NY_VURDERING.getKode());
        oppgaveDto.setBehandlingstema(eksisterendeOppgave.getBehandlingstema());
        oppgaveDto.setBeskrivelse(eksisterendeOppgave.getBeskrivelse());
        oppgaveDto.setFristFerdigstillelse(idag.isAfter(eksisterendeOppgave.getFristFerdigstillelse()) ? idag : eksisterendeOppgave.getFristFerdigstillelse());
        oppgaveDto.setJournalpostId(eksisterendeOppgave.getJournalpostId());
        oppgaveDto.setOppgavetype(eksisterendeOppgave.getOppgavetype());
        oppgaveDto.setPrioritet(eksisterendeOppgave.getPrioritet());
        oppgaveDto.setSaksreferanse(eksisterendeOppgave.getSaksreferanse());
        oppgaveDto.setTema(eksisterendeOppgave.getTema());
        oppgaveDto.setTildeltEnhetsnr(Integer.toString(MELOSYS_ENHET_ID));
        oppgaveDto.setBehandlesAvApplikasjon(eksisterendeOppgave.getBehandlesAvApplikasjon());

        oppgaveDto.setTilordnetRessurs(saksbehandler);

        return oppgaveDto;
    }

    private Aktoer finnAktoerBruker(Set<Aktoer> aktoerer) throws FunksjonellException {
        return aktoerer.stream()
                .filter(a -> a.getRolle() == Aktoersroller.BRUKER)
                .findFirst()
                .orElseThrow(() -> new FunksjonellException("Kan ikke finne aktør med rolle bruker"));
    }
}