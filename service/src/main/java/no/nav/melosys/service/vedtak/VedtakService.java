package no.nav.melosys.service.vedtak;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.kodeverk.Kodeverk;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.sed.EessiService;
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
    private final VedtakKontrollService vedtakKontrollService;
    private final RegisteropplysningerService registeropplysningerService;

    @Autowired
    public VedtakService(BehandlingService behandlingService, BehandlingsresultatService behandlingsresultatService,
                         OppgaveService oppgaveService, ProsessinstansService prosessinstansService,
                         EessiService eessiService, LandvelgerService landvelgerService,
                         TpsFasade tpsFasade,
                         VedtakKontrollService vedtakKontrollService,
                         RegisteropplysningerService registeropplysningerService) {
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.oppgaveService = oppgaveService;
        this.prosessinstansService = prosessinstansService;
        this.eessiService = eessiService;
        this.landvelgerService = landvelgerService;
        this.tpsFasade = tpsFasade;
        this.vedtakKontrollService = vedtakKontrollService;
        this.registeropplysningerService = registeropplysningerService;
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void fattVedtak(long behandlingID, Behandlingsresultattyper behandlingsresultattype) throws MelosysException {
        fattVedtak(behandlingID, behandlingsresultattype, null, null, null, Vedtakstyper.FØRSTEGANGSVEDTAK, null);
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void fattVedtak(long behandlingID, Behandlingsresultattyper behandlingsresultatType,
                           String fritekst, String fritekstSed, List<String> mottakerinstitusjoner,
                           Vedtakstyper vedtakstype, String revurderBegrunnelse) throws MelosysException {
        behandlingsresultatService.oppdaterBehandlingsresultattype(behandlingID, behandlingsresultatType);
        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);
        validerBehandlingstypeFattVedtak(behandling);
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        log.info("Fatter vedtak for sak: {} behandling: {}", behandling.getFagsak().getSaksnummer(), behandlingID);

        if (behandlingsresultat.erInnvilgelse()) {
            Lovvalgsperiode lovvalgsperiode = behandlingsresultat.hentValidertLovvalgsperiode();
            String fnr = tpsFasade.hentIdentForAktørId(behandling.getFagsak().hentBruker().getAktørId());

            registeropplysningerService.hentOgLagreOpplysninger(
                RegisteropplysningerRequest.builder()
                    .behandlingID(behandlingID)
                    .fnr(fnr)
                    .fom(lovvalgsperiode.getFom())
                    .tom(lovvalgsperiode.getTom())
                    .saksopplysningTyper(RegisteropplysningerRequest.SaksopplysningTyper.builder()
                        .medlemskapsopplysninger().build())
                    .build());

            kontrollerFattVedtak(behandlingID, vedtakstype);
        }

        Collection<Landkoder> landkoder = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        if (skalSendesSed(behandlingsresultat, landkoder)) {
            mottakerinstitusjoner = eessiService.validerOgAvklarMottakerInstitusjonerForBuc(
                mottakerinstitusjoner,
                landkoder,
                avklarBucType(behandlingsresultat)
            );
        } else {
            mottakerinstitusjoner = Collections.emptyList();
        }

        behandling.setStatus(Behandlingsstatus.IVERKSETTER_VEDTAK);
        behandlingService.lagre(behandling);
        prosessinstansService.opprettProsessinstansIverksettVedtak(behandling, behandlingsresultatType, fritekst, fritekstSed, mottakerinstitusjoner, vedtakstype, revurderBegrunnelse);
        oppgaveService.ferdigstillOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
    }

    private void validerBehandlingstypeFattVedtak(Behandling behandling) throws FunksjonellException {
        if (!behandling.kanResultereIVedtak()) {
            throw new FunksjonellException("Kan ikke fatte vedtak ved behandlingstema " + behandling.getTema().getBeskrivelse());
        }
    }

    private void kontrollerFattVedtak(long behandlingID, Vedtakstyper vedtakstype) throws MelosysException {
        Collection<Kontroll_begrunnelser> feilValideringer = vedtakKontrollService.utførKontroller(behandlingID, vedtakstype);
        if (!feilValideringer.isEmpty()) {
            throw new ValideringException("Feil i validering. Kan ikke fatte vedtak.",
                feilValideringer.stream().map(Kodeverk::getKode).collect(Collectors.toList()));
        }
    }

    private static boolean skalSendesSed(Behandlingsresultat behandlingsresultat, Collection<Landkoder> landkoder) {
        if (behandlingsresultat.erAvslag()) {
            return false;
        }
        if (landkoder.isEmpty()) {
            return false;
        }
        return !behandlingsresultat.erArt16EtterUtlandMedRegistrertSvar();
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

        prosessinstansService.opprettProsessinstansForkortPeriode(behandling, endretperiode, fritekst, fritekstSed);
        oppgaveService.ferdigstillOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
    }
}