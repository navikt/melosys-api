package no.nav.melosys.service.kontroll.vedtak;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.validering.Kontrollfeil;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class VedtakKontrollService {

    private final BehandlingService behandlingService;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final PersondataFasade persondataFasade;
    private final Unleash unleash;

    public VedtakKontrollService(BehandlingService behandlingService, LovvalgsperiodeService lovvalgsperiodeService,
                                 PersondataFasade persondataFasade, Unleash unleash) {
        this.behandlingService = behandlingService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.persondataFasade = persondataFasade;
        this.unleash = unleash;
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
        Persondata persondata = hentPersondata(behandling);
        VedtakKontrollData vedtakKontrollData = new VedtakKontrollData(medlemskapDokument, persondata, behandlingsgrunnlagData, lovvalgsperiode);
        return kontroller.stream()
            .map(f -> f.apply(vedtakKontrollData))
            .filter(Objects::nonNull)
            .toList();
    }

    private Persondata hentPersondata(Behandling behandling) {
        if (unleash.isEnabled("melosys.pdl.aktiv")) {
            return persondataFasade.hentPerson(behandling.getFagsak().hentAktørID());
        }
        return behandling.hentPersonDokument();
    }
}
