package no.nav.melosys.service.vedtak;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.exception.*;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.validering.Kontrollfeil;
import no.nav.melosys.service.kontroll.vedtak.VedtakKontrollService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerRequest;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VedtakService {
    private static final Logger log = LoggerFactory.getLogger(VedtakService.class);

    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final OppgaveService oppgaveService;
    private final ProsessinstansService prosessinstansService;
    private final EessiService eessiService;
    private final LandvelgerService landvelgerService;
    private final TpsFasade tpsFasade;
    private final RegisteropplysningerService registeropplysningerService;
    private final VedtakKontrollService vedtakKontrollService;

    @Autowired
    public VedtakService(BehandlingService behandlingService, BehandlingsresultatService behandlingsresultatService,
                         OppgaveService oppgaveService, ProsessinstansService prosessinstansService,
                         EessiService eessiService, LandvelgerService landvelgerService,
                         TpsFasade tpsFasade, RegisteropplysningerService registeropplysningerService,
                         VedtakKontrollService vedtakKontrollService) {
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.oppgaveService = oppgaveService;
        this.prosessinstansService = prosessinstansService;
        this.eessiService = eessiService;
        this.landvelgerService = landvelgerService;
        this.tpsFasade = tpsFasade;
        this.registeropplysningerService = registeropplysningerService;
        this.vedtakKontrollService = vedtakKontrollService;
    }

    @Transactional(rollbackFor = MelosysException.class, noRollbackFor = {ValideringException.class})
    public void fattVedtak(long behandlingID, Behandlingsresultattyper behandlingsresultattype) throws MelosysException {
        fattVedtak(behandlingID, behandlingsresultattype, null, null, null, Vedtakstyper.FØRSTEGANGSVEDTAK, null);
    }

    @Transactional(rollbackFor = MelosysException.class, noRollbackFor = {ValideringException.class})
    public void fattVedtak(long behandlingID, Behandlingsresultattyper behandlingsresultatType,
                           String fritekst, String fritekstSed, Set<String> mottakerinstitusjoner,
                           Vedtakstyper vedtakstype, String revurderBegrunnelse) throws MelosysException {
        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);
        validerBehandlingstypeFattVedtak(behandling);
        
        behandlingsresultatService.oppdaterBehandlingsresultattype(behandlingID, behandlingsresultatType);
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        log.info("Fatter vedtak for sak: {} behandling: {}", behandling.getFagsak().getSaksnummer(), behandlingID);

        if (behandlingsresultat.erInnvilgelse()) {
            validerInnvilgelse(vedtakstype, behandling, behandlingsresultat);
        }

        mottakerinstitusjoner = validerOgAvklarMottakerInstitusjoner(behandling, mottakerinstitusjoner, behandlingsresultat);

        if (prosessinstansService.harAktivVedtakInstans(behandlingID)) {
            throw new FunksjonellException("Det finnes allerede en aktiv prosess for behandling " + behandling);
        }
        behandling.setStatus(Behandlingsstatus.IVERKSETTER_VEDTAK);
        behandlingService.lagre(behandling);
        prosessinstansService.opprettProsessinstansIverksettVedtak(behandling, behandlingsresultatType,
            fritekst, fritekstSed, mottakerinstitusjoner, vedtakstype, revurderBegrunnelse);
        oppgaveService.ferdigstillOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
    }

    private void validerInnvilgelse(Vedtakstyper vedtakstype,
                                    Behandling behandling,
                                    Behandlingsresultat behandlingsresultat) throws MelosysException {
        Lovvalgsperiode lovvalgsperiode = behandlingsresultat.hentValidertLovvalgsperiode();
        String fnr = tpsFasade.hentIdentForAktørId(behandling.getFagsak().hentBruker().getAktørId());

        registeropplysningerService.hentOgLagreOpplysninger(
            RegisteropplysningerRequest.builder()
                .behandlingID(behandling.getId())
                .fnr(fnr)
                .fom(lovvalgsperiode.getFom())
                .tom(lovvalgsperiode.getTom())
                .saksopplysningTyper(RegisteropplysningerRequest.SaksopplysningTyper.builder()
                    .medlemskapsopplysninger().build())
                .build());

        kontrollerFattVedtak(behandling.getId(), vedtakstype);
    }

    private Set<String> validerOgAvklarMottakerInstitusjoner(Behandling behandling,
                                                             Set<String> mottakerinstitusjoner,
                                                             Behandlingsresultat behandlingsresultat) throws MelosysException {
        Collection<Landkoder> landkoder = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandling.getId());
        if (mottakereTrenges(behandling) && skalSedSendes(behandlingsresultat, landkoder)) {
            mottakerinstitusjoner = eessiService.validerOgAvklarMottakerInstitusjonerForBuc(
                mottakerinstitusjoner,
                landkoder,
                avklarBucType(behandlingsresultat)
            );
        } else {
            mottakerinstitusjoner = Collections.emptySet();
        }
        return mottakerinstitusjoner;
    }

    private static boolean mottakereTrenges(Behandling behandling) {
        return !behandling.erNorgeUtpekt();
    }

    private static boolean skalSedSendes(Behandlingsresultat behandlingsresultat, Collection<Landkoder> landkoder) {
        if (behandlingsresultat.erAvslag()) {
            return false;
        }
        if (landkoder.isEmpty()) {
            return false;
        }
        return !behandlingsresultat.erArt16EtterUtlandMedRegistrertSvar();
    }

    private void validerBehandlingstypeFattVedtak(Behandling behandling) throws FunksjonellException {
        if (!behandling.kanResultereIVedtak()) {
            throw new FunksjonellException("Kan ikke fatte vedtak ved behandlingstema " + behandling.getTema().getBeskrivelse());
        }
    }

    private void kontrollerFattVedtak(long behandlingID, Vedtakstyper vedtakstype) throws MelosysException {
        Collection<Kontrollfeil> feilValideringer = vedtakKontrollService.utførKontroller(behandlingID, vedtakstype);
        if (!feilValideringer.isEmpty()) {
            throw new ValideringException("Feil i validering. Kan ikke fatte vedtak.",
                feilValideringer.stream().map(Kontrollfeil::tilDto).collect(Collectors.toList()));
        }
    }

    private static BucType avklarBucType(Behandlingsresultat behandlingsresultat) {
        return BucType.fraBestemmelse(
            behandlingsresultat.hentValidertMedlemskapsperiode().getBestemmelse()
        );
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void endreVedtak(Long behandlingID, Endretperiode endretperiode, String fritekst, String fritekstSed) throws FunksjonellException, TekniskException {
        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);
        log.info("Endrer vedtak for sak: {} behandling: {}", behandling.getFagsak().getSaksnummer(), behandlingID);

        if (prosessinstansService.harAktivProsessinstans(behandlingID)) {
            throw new FunksjonellException("Det finnes allerede en aktiv prosess for behandling " + behandling);
        }
        prosessinstansService.opprettProsessinstansForkortPeriode(behandling, endretperiode, fritekst, fritekstSed);
        oppgaveService.ferdigstillOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
    }
}