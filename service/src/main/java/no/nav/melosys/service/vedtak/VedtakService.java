package no.nav.melosys.service.vedtak;

import java.util.Collection;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsystem;
import no.nav.melosys.domain.kodeverk.Kodeverk;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;
import no.nav.melosys.domain.kodeverk.begrunnelser.Unntak_periode_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.util.LovvalgBestemmelseUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.kontroll.vedtak.VedtakKontrollService;
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
    private final VedtakKontrollService vedtakKontrollService;
    private final SaksopplysningerService saksopplysningerService;

    @Autowired
    public VedtakService(BehandlingService behandlingService, BehandlingsresultatService behandlingsresultatService,
                         OppgaveService oppgaveService, ProsessinstansService prosessinstansService,
                         EessiService eessiService, LandvelgerService landvelgerService,
                         FagsakService fagsakService, GsakFasade gsakFasade, VedtakKontrollService vedtakKontrollService, SaksopplysningerService saksopplysningerService) {
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.oppgaveService = oppgaveService;
        this.prosessinstansService = prosessinstansService;
        this.eessiService = eessiService;
        this.landvelgerService = landvelgerService;
        this.gsakFasade = gsakFasade;
        this.fagsakService = fagsakService;
        this.vedtakKontrollService = vedtakKontrollService;
        this.saksopplysningerService = saksopplysningerService;
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void fattVedtak(long behandlingID, Behandlingsresultattyper behandlingsresultattype) throws MelosysException {
        fattVedtak(behandlingID, behandlingsresultattype, null, null, Vedtakstyper.FØRSTEGANGSVEDTAK, null);
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void fattVedtak(long behandlingID, Behandlingsresultattyper behandlingsresultatType,
                           String fritekst, String mottakerInstitusjon,
                           Vedtakstyper vedtakstype, String revurderBegrunnelse) throws MelosysException {
        behandlingsresultatService.oppdaterBehandlingsresultattype(behandlingID, behandlingsresultatType);
        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        log.info("Fatter vedtak for sak: {} behandling: {}", behandling.getFagsak().getSaksnummer(), behandlingID);

        if (behandlingsresultat.erInnvilgelse()) {
            saksopplysningerService.hentSaksopplysningMedl(behandlingID, behandlingsresultat.hentValidertLovvalgsperiode());
            validerFattVedtak(behandlingID, vedtakstype);
        }

        Collection<Landkoder> landkoder = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        if (skalSendesSed(behandlingsresultat, landkoder)) {
            String landkode = landkoder.iterator().next().getKode();
            validerMottakerInstitusjon(landkode, behandlingsresultat, mottakerInstitusjon);
        }

        behandling.setStatus(Behandlingsstatus.IVERKSETTER_VEDTAK);
        behandlingService.lagre(behandling);
        prosessinstansService.opprettProsessinstansIverksettVedtak(behandling, behandlingsresultatType, fritekst, mottakerInstitusjon, vedtakstype, revurderBegrunnelse );
        oppgaveService.ferdigstillOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
    }

    private void validerFattVedtak(long behandlingID, Vedtakstyper vedtakstype) throws MelosysException {
        Collection<Unntak_periode_begrunnelser> feilValideringer = vedtakKontrollService.utførKontroller(behandlingID, vedtakstype);
        if (!feilValideringer.isEmpty()) {
            throw new ValideringException("Feil i validering. Kan ikke fatte vedtak.",
                feilValideringer.stream().map(Kodeverk::getKode).collect(Collectors.toList()));
        }
    }

    private boolean skalSendesSed(Behandlingsresultat behandlingsresultat, Collection<Landkoder> landkoder) {
        if (behandlingsresultat.erAvslag()) {
            return false;
        }
        if (landkoder.isEmpty()) {
            return false;
        }
        return !behandlingsresultat.erArt16EtterUtlandMedRegistrertSvar();
    }

    private void validerMottakerInstitusjon(String landkode, Behandlingsresultat behandlingsresultat, String mottakerInstitusjon) throws MelosysException {
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

        Behandling nyBehandling = behandlingService.replikerBehandlingOgBehandlingsresultat(eksisterendeBehandling, Behandlingsstatus.OPPRETTET, Behandlingstyper.NY_VURDERING);

        eksisterendeBehandling.getFagsak().setStatus(Saksstatuser.OPPRETTET);
        fagsakService.lagre(eksisterendeBehandling.getFagsak());

        opprettRevurderingsoppgave(eksisterendeBehandling, nyBehandling, SubjectHandler.getInstance().getUserID());

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

    private void opprettRevurderingsoppgave(Behandling eksisterendeBehandling, Behandling nyBehandling, String saksbehandler)
        throws TekniskException, FunksjonellException {
        Oppgave oppgave = OppgaveFactory.lagBehandlingsOppgaveForType(Behandlingstyper.NY_VURDERING)
            .setAktørId(eksisterendeBehandling.getFagsak().hentBruker().getAktørId())
            .setSaksnummer(eksisterendeBehandling.getFagsak().getSaksnummer())
            .setJournalpostId(nyBehandling.getInitierendeJournalpostId())
            .setTilordnetRessurs(saksbehandler)
            .setBehandlesAvApplikasjon(Fagsystem.MELOSYS)
            .build();
        gsakFasade.opprettOppgave(oppgave);
    }
}