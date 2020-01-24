package no.nav.melosys.service.kontroll.vedtak;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.LovvalgsperiodeService;
import org.springframework.stereotype.Component;

@Component
public class VedtakKontrollService {

    private final BehandlingService behandlingService;
    private final LovvalgsperiodeService lovvalgsperiodeService;

    public VedtakKontrollService(BehandlingService behandlingService, LovvalgsperiodeService lovvalgsperiodeService) {
        this.behandlingService = behandlingService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
    }

    public Collection<Kontroll_begrunnelser> utførKontroller(long behandlingID, Vedtakstyper vedtakstype) throws FunksjonellException, TekniskException {
        return utførKontroller(
            behandlingService.hentBehandling(behandlingID),
            lovvalgsperiodeService.hentValidertLovvalgsperiode(behandlingID),
            VedtakKontrollFactory.hentKontrollerForVedtakstype(vedtakstype)
        );
    }

    private Collection<Kontroll_begrunnelser> utførKontroller(
        Behandling behandling,
        Lovvalgsperiode lovvalgsperiode,
        Set<Function<VedtakKontrollData, Kontroll_begrunnelser>> kontroller
    ) throws TekniskException {
        MedlemskapDokument medlemskapDokument = SaksopplysningerUtils.hentMedlemskapDokument(behandling);
        VedtakKontrollData vedtakKontrollData = new VedtakKontrollData(medlemskapDokument, lovvalgsperiode);
        return kontroller.stream()
            .map(f -> f.apply(vedtakKontrollData))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
