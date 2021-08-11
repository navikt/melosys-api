package no.nav.melosys.service.unntaksperiode;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingsresultatBegrunnelse;
import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Utfallregistreringunntak;
import no.nav.melosys.domain.kodeverk.begrunnelser.Ikke_godkjent_begrunnelser;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.kontroll.PeriodeKontroller;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    public UnntaksperiodeService(BehandlingService behandlingService,
                                 BehandlingsresultatService behandlingsresultatService,
                                 LovvalgsperiodeService lovvalgsperiodeService,
                                 OppgaveService oppgaveService,
                                 ProsessinstansService prosessinstansService) {
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.oppgaveService = oppgaveService;
        this.prosessinstansService = prosessinstansService;
    }

    @Transactional
    public void godkjennPeriode(long behandlingID, boolean varsleUtland, String fritekst) {
        Behandling behandling = hentOgValiderBehandling(behandlingID);

        validerPeriodeFraBehandling(behandling);
        opprettLovvalgsperiodeFraSedDokument(behandlingID, behandling.hentSedDokument());

        behandlingsresultatService.oppdaterUtfallRegistreringUnntak(behandlingID, Utfallregistreringunntak.GODKJENT);
        prosessinstansService.opprettProsessinstansGodkjennUnntaksperiode(
            behandling,
            varsleUtland,
            fritekst
        );
        oppgaveService.ferdigstillOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
    }

    @Transactional
    public void godkjennOgEndrePeriode(long behandlingID, EndretUnntaksperiodeGodkjenning endretUnntaksperiodeGodkjenning) {
        Behandling behandling = hentOgValiderBehandling(behandlingID);

        var endretPeriode = endretUnntaksperiodeGodkjenning.endretPeriode();
        validerEndretPeriode(endretPeriode);
        opprettEndretLovvalgsperiode(behandlingID, endretPeriode);

        behandlingsresultatService.oppdaterUtfallRegistreringUnntak(behandlingID, Utfallregistreringunntak.GODKJENT);
        prosessinstansService.opprettProsessinstansGodkjennUnntaksperiode(
            behandling,
            endretUnntaksperiodeGodkjenning.varsleUtland(),
            endretUnntaksperiodeGodkjenning.fritekst()
        );
        oppgaveService.ferdigstillOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
    }

    private void opprettEndretLovvalgsperiode(long behandlingID, Unntaksperiode unntaksperiodeDto) {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setFom(unntaksperiodeDto.fom());
        lovvalgsperiode.setTom(unntaksperiodeDto.tom());

        lovvalgsperiodeService.lagreLovvalgsperioder(
            behandlingID,
            Collections.singleton(lovvalgsperiode)
        );
    }

    private void opprettLovvalgsperiodeFraSedDokument(long behandlingID, SedDokument sedDokument) {
        lovvalgsperiodeService.lagreLovvalgsperioder(
            behandlingID,
            Collections.singleton(sedDokument.opprettInnvilgetLovvalgsperiode())
        );
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
        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);
        validerBehandling(behandling);
        return behandling;
    }

    private void validerBehandling(Behandling behandling) {
        if (!behandling.erRegisteringAvUnntak()) {
            throw new FunksjonellException(
                String.format("Behandling %s er ikke av tema registrering-unntak, men %s", behandling.getId(), behandling.getTema())
            );
        } else if (behandling.erInaktiv()) {
            throw new FunksjonellException(String.format("Behandling %s er inaktiv", behandling.getId()));
        }
    }

    private void validerEndretPeriode(Unntaksperiode unntaksperiodeDto) {
        if (PeriodeKontroller.feilIPeriode(unntaksperiodeDto.fom(), unntaksperiodeDto.tom())) {
            throw new FunksjonellException(
                String.format("Feil i perioden %s - %s som det forsøkes å endre til", unntaksperiodeDto.fom(), unntaksperiodeDto.tom()));
        }
    }

    private void validerPeriodeFraBehandling(Behandling behandling) {
        ErPeriode periode = behandling.hentPeriode();
        if (PeriodeKontroller.feilIPeriode(periode.getFom(), periode.getTom())) {
            throw new FunksjonellException(String.format("Behandling %s har feil i perioden", behandling.getId()));
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
