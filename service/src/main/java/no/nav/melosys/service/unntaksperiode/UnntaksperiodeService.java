package no.nav.melosys.service.unntaksperiode;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingsresultatBegrunnelse;
import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.Utfallregistreringunntak;
import no.nav.melosys.domain.kodeverk.begrunnelser.Ikke_godkjent_begrunnelser;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.kontroll.feature.unntaksperiode.UnntaksperiodeKontrollService;
import no.nav.melosys.service.kontroll.regler.PeriodeRegler;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Primary
public class UnntaksperiodeService {
    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final OppgaveService oppgaveService;
    private final ProsessinstansService prosessinstansService;
    private final UnntaksperiodeKontrollService unntaksperiodeKontrollService;

    public UnntaksperiodeService(BehandlingService behandlingService,
                                 BehandlingsresultatService behandlingsresultatService,
                                 LovvalgsperiodeService lovvalgsperiodeService,
                                 OppgaveService oppgaveService,
                                 ProsessinstansService prosessinstansService,
                                 UnntaksperiodeKontrollService unntaksperiodeKontrollService) {
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.oppgaveService = oppgaveService;
        this.prosessinstansService = prosessinstansService;
        this.unntaksperiodeKontrollService = unntaksperiodeKontrollService;
    }

    @Transactional
    public void godkjennPeriode(long behandlingID, UnntaksperiodeGodkjenning unntaksperiodeGodkjenning) {
        Behandling behandling = hentOgValiderBehandling(behandlingID);
        validerPeriode(behandling, unntaksperiodeGodkjenning);
        opprettLovvalgsperiode(behandlingID, behandling.hentSedDokument(), unntaksperiodeGodkjenning);
        behandlingsresultatService.oppdaterUtfallRegistreringUnntak(behandlingID, Utfallregistreringunntak.GODKJENT);
        prosessinstansService.opprettProsessinstansGodkjennUnntaksperiode(
            behandling,
            unntaksperiodeGodkjenning.varsleUtland(),
            unntaksperiodeGodkjenning.fritekst()
        );
        oppgaveService.ferdigstillOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
    }

    private void validerPeriode(Behandling behandling, UnntaksperiodeGodkjenning unntaksperiodeGodkjenning) {
        ErPeriode periode = hentPeriode(behandling, unntaksperiodeGodkjenning);
        if (periode.getFom() == null || periode.getTom() == null) {
            throw new FunksjonellException("Oppgi både startdato og sluttdato på perioden");
        }
        if (PeriodeRegler.feilIPeriode(periode.getFom(), periode.getTom())) {
            throw new FunksjonellException("Behandling %s har feil i perioden med periode %s til %s"
                .formatted(behandling.getId(), periode.getFom(), periode.getTom()));
        }
        behandling.finnSedDokument()
            .ifPresent(sedDokument -> unntaksperiodeKontrollService.kontrollPeriode(sedDokument, periode));
    }

    private ErPeriode hentPeriode(Behandling behandling, UnntaksperiodeGodkjenning unntaksperiodeGodkjenning) {
        Unntaksperiode endretPeriode = unntaksperiodeGodkjenning.endretPeriode();
        if (endretPeriode != null) {
            return new Periode(endretPeriode.fom(), endretPeriode.tom());
        }
        return behandling.hentPeriode();
    }


    private void opprettLovvalgsperiode(long behandlingID, SedDokument sedDokument, UnntaksperiodeGodkjenning unntaksperiodeGodkjenning) {
        Lovvalgsperiode lovvalgsperiode = sedDokument.opprettInnvilgetLovvalgsperiode();
        LovvalgBestemmelse lovvalgBestemmelse = unntaksperiodeGodkjenning.lovvalgsbestemmelse();
        Unntaksperiode endretPeriode = unntaksperiodeGodkjenning.endretPeriode();

        if (lovvalgBestemmelse != null) {
            lovvalgsperiode.setBestemmelse(lovvalgBestemmelse);
        }
        if (endretPeriode != null) {
            lovvalgsperiode.setFom(endretPeriode.fom());
            lovvalgsperiode.setTom(endretPeriode.tom());
        }

        lovvalgsperiodeService.lagreLovvalgsperioder(behandlingID, Collections.singleton(lovvalgsperiode));
    }

    @Transactional
    public void ikkeGodkjennPeriode(long behandlingID, Set<String> begrunnelser, String fritekst) {
        Behandling behandling = hentOgValiderBehandling(behandlingID);
        Set<Ikke_godkjent_begrunnelser> ikkeGodkjentBegrunnelser = tilIkkeGodkjentBegrunnelser(begrunnelser);
        validerBegrunnelser(ikkeGodkjentBegrunnelser, fritekst);
        behandlingsresultatService.oppdaterUtfallRegistreringUnntak(behandlingID, Utfallregistreringunntak.IKKE_GODKJENT);
        behandlingsresultatService.oppdaterBegrunnelser(
            behandlingID, begrunnelser.stream().map(BehandlingsresultatBegrunnelse::lag).collect(Collectors.toSet()), fritekst
        );

        prosessinstansService.opprettProsessinstansUnntaksperiodeAvvist(behandling, ikkeGodkjentBegrunnelser, fritekst);
        oppgaveService.ferdigstillOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
    }

    private Set<Ikke_godkjent_begrunnelser> tilIkkeGodkjentBegrunnelser(Set<String> begrunnelser) {
        Set<Ikke_godkjent_begrunnelser> ikkeGodkjentBegrunnelser = new HashSet<>();
        for (String b : begrunnelser) {
            ikkeGodkjentBegrunnelser.add(Ikke_godkjent_begrunnelser.valueOf(b));
        }
        return ikkeGodkjentBegrunnelser;
    }

    private Behandling hentOgValiderBehandling(long behandlingID) {
        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        validerBehandling(behandling);
        return behandling;
    }

    private void validerBehandling(Behandling behandling) {
        if (!behandling.erRegisteringAvUnntak()) {
            throw new FunksjonellException("Behandling %s er ikke av tema registrering-unntak, men %s"
                .formatted(behandling.getId(), behandling.getTema()));
        } else if (behandling.erInaktiv()) {
            throw new FunksjonellException("Behandling %s er inaktiv".formatted(behandling.getId()));
        }
    }

    private void validerBegrunnelser(Set<Ikke_godkjent_begrunnelser> begrunnelser, String fritekst) {
        if (begrunnelser.isEmpty()) {
            throw new FunksjonellException("Ingen begrunnelser for avlag av periode");
        } else if (begrunnelser.contains(Ikke_godkjent_begrunnelser.ANNET) && StringUtils.isEmpty(fritekst)) {
            throw new FunksjonellException("Begrunnelse " + Ikke_godkjent_begrunnelser.ANNET + " krever fritekst!");
        }
    }
}
