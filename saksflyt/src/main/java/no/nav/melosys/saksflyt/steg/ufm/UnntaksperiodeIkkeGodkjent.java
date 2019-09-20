package no.nav.melosys.saksflyt.steg.ufm;

import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Utfallregistreringunntak;
import no.nav.melosys.domain.kodeverk.begrunnelser.Ikke_godkjent_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.integrasjon.medl.StatusaarsakMedl;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class UnntaksperiodeIkkeGodkjent extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(UnntaksperiodeIkkeGodkjent.class);

    private final BehandlingRepository behandlingRepository;
    private final BehandlingsresultatRepository behandlingsresultatRepository;
    private final MedlFasade medlFasade;

    public UnntaksperiodeIkkeGodkjent(BehandlingRepository behandlingRepository, BehandlingsresultatRepository behandlingsresultatRepository, MedlFasade medlFasade) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.medlFasade = medlFasade;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_PERIODE_IKKE_GODKJENT;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {

        Behandling behandling = prosessinstans.getBehandling();
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        behandlingRepository.save(behandling);

        Behandlingsresultat behandlingsresultat = behandlingsresultatRepository.findById(behandling.getId())
            .orElseThrow(() -> new TekniskException("Ingen behandlingsresultat for behandling " + behandling.getId()));

        behandlingsresultat.setType(Behandlingsresultattyper.REGISTRERT_UNNTAK);
        behandlingsresultat.setUtfallRegistreringUnntak(Utfallregistreringunntak.IKKE_GODKJENT);

        List<String> begrunnelser = prosessinstans.getData(ProsessDataKey.BEHANDLINGSRESULTAT_BEGRUNNELSER, List.class);
        if (begrunnelser == null || begrunnelser.isEmpty()) {
            throw new TekniskException("Registrering av ikke-godkjent unntaksperiode krever minst en begrunnelse! Behandlingid: " + behandling.getId());
        }
        begrunnelser.forEach(begrunnelse -> {
            BehandlingsresultatBegrunnelse behandlingsresultatBegrunnelse = new BehandlingsresultatBegrunnelse();
            behandlingsresultatBegrunnelse.setKode(begrunnelse);
            behandlingsresultatBegrunnelse.setBehandlingsresultat(behandlingsresultat);
            behandlingsresultat.getBehandlingsresultatBegrunnelser().add(behandlingsresultatBegrunnelse);
        });
        if (begrunnelser.contains(Ikke_godkjent_begrunnelser.ANNET.getKode())) {
            behandlingsresultat.setBegrunnelseFritekst(prosessinstans.getData(ProsessDataKey.BEHANDLINGSRESULTAT_BEGRUNNELSE_FRITEKST));
        }

        behandlingsresultatRepository.save(behandlingsresultat);

        Set<Lovvalgsperiode> lovvalgsperioder = behandlingsresultat.getLovvalgsperioder();
        if (!lovvalgsperioder.isEmpty()) {
            medlFasade.avvisPeriode(lovvalgsperioder.iterator().next().getMedlPeriodeID(), StatusaarsakMedl.AVVIST);
        }

        log.info("Unntaksperiode avvist i medl og behandling avsluttet for fagsak {}", prosessinstans.getBehandling().getFagsak().getSaksnummer());
        prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_SAK_OG_BEHANDLING_AVSLUTTET);
    }
}
