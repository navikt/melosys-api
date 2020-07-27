package no.nav.melosys.saksflyt.steg.ufm;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.BehandlingsresultatBegrunnelse;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Utfallregistreringunntak;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class UnntaksperiodeIkkeGodkjent extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(UnntaksperiodeIkkeGodkjent.class);

    private final FagsakService fagsakService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final MedlPeriodeService medlPeriodeService;

    public UnntaksperiodeIkkeGodkjent(FagsakService fagsakService, BehandlingsresultatService behandlingsresultatService, MedlPeriodeService medlPeriodeService) {
        this.fagsakService = fagsakService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.medlPeriodeService = medlPeriodeService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_PERIODE_IKKE_GODKJENT;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {

        final long behandlingID = prosessinstans.getBehandling().getId();

        List<String> begrunnelser = prosessinstans.getData(ProsessDataKey.BEHANDLINGSRESULTAT_BEGRUNNELSER, List.class);
        String begrunnelseFritekst = prosessinstans.getData(ProsessDataKey.BEHANDLINGSRESULTAT_BEGRUNNELSE_FRITEKST);
        if (begrunnelser == null || begrunnelser.isEmpty()) {
            throw new TekniskException("Registrering av ikke-godkjent unntaksperiode krever minst en begrunnelse! Behandlingid: " + behandlingID);
        }

        fagsakService.avsluttFagsakOgBehandling(prosessinstans.getBehandling().getFagsak(), Saksstatuser.AVSLUTTET);

        behandlingsresultatService.oppdaterUtfallRegistreringUnntak(behandlingID, Utfallregistreringunntak.IKKE_GODKJENT);
        behandlingsresultatService.oppdaterBegrunnelser(behandlingID, lagBegrunnelser(begrunnelser), begrunnelseFritekst);

        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        Set<Lovvalgsperiode> lovvalgsperioder = behandlingsresultat.getLovvalgsperioder();
        if (!lovvalgsperioder.isEmpty()) {
            medlPeriodeService.avvisPeriode(lovvalgsperioder.iterator().next().getMedlPeriodeID());
            log.info("Unntaksperiode avvist i medl for fagsak {}", prosessinstans.getBehandling().getFagsak().getSaksnummer());
        }

        prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_SAK_OG_BEHANDLING_AVSLUTTET);
    }

    private Set<BehandlingsresultatBegrunnelse> lagBegrunnelser(Collection<String> begrunnelser) {
        return begrunnelser.stream().map(begrunnelse -> {
            BehandlingsresultatBegrunnelse behandlingsresultatBegrunnelse = new BehandlingsresultatBegrunnelse();
            behandlingsresultatBegrunnelse.setKode(begrunnelse);
            return behandlingsresultatBegrunnelse;
        }).collect(Collectors.toSet());
    }
}
