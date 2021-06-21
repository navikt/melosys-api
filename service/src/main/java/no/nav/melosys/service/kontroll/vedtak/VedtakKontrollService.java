package no.nav.melosys.service.kontroll.vedtak;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.validering.Kontrollfeil;
import org.springframework.stereotype.Component;

@Component
public class VedtakKontrollService {

    private final BehandlingService behandlingService;
    private final LovvalgsperiodeService lovvalgsperiodeService;

    public VedtakKontrollService(BehandlingService behandlingService, LovvalgsperiodeService lovvalgsperiodeService) {
        this.behandlingService = behandlingService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
    }

    public Collection<Kontrollfeil> utførKontroller(long behandlingID, Vedtakstyper vedtakstype) {
        return utførKontroller(
            behandlingService.hentBehandling(behandlingID),
            lovvalgsperiodeService.hentValidertLovvalgsperiode(behandlingID),
            VedtakKontrollFactory.hentKontrollerForVedtakstype(vedtakstype)
        );
    }

    private Collection<Kontrollfeil> utførKontroller(
        Behandling behandling,
        Lovvalgsperiode lovvalgsperiode,
        Set<Function<VedtakKontrollData, Kontrollfeil>> kontroller
    ) {
        BehandlingsgrunnlagData behandlingsgrunnlagData = behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata();
        MedlemskapDokument medlemskapDokument = behandling.hentMedlemskapDokument();
        Persondata persondata = behandling.hentPersonDokument();
        VedtakKontrollData vedtakKontrollData = new VedtakKontrollData(medlemskapDokument, persondata, behandlingsgrunnlagData, lovvalgsperiode);
        return kontroller.stream()
            .map(f -> f.apply(vedtakKontrollData))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
