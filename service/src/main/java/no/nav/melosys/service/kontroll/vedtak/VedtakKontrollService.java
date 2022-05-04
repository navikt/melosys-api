package no.nav.melosys.service.kontroll.vedtak;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerRequest;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import no.nav.melosys.service.validering.Kontrollfeil;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Primary
public class VedtakKontrollService {

    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final PersondataFasade persondataFasade;
    private final RegisteropplysningerService registeropplysningerService;

    public VedtakKontrollService(BehandlingService behandlingService,
                                 BehandlingsresultatService behandlingsresultatService,
                                 LovvalgsperiodeService lovvalgsperiodeService, PersondataFasade persondataFasade,
                                 RegisteropplysningerService registeropplysningerService) {
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.persondataFasade = persondataFasade;
        this.registeropplysningerService = registeropplysningerService;
    }

    @Transactional
    public void kontrollerVedtak(long behandlingId, boolean skalRegisteropplysningerOppdateres,
                                 Behandlingsresultattyper behandlingsresultattype) throws ValideringException {
        var behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingId);
        var sakstype = behandling.getFagsak().getType();
        var erAvslag = behandlingsresultattype.equals(Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL);

        if (skalRegisteropplysningerOppdateres) {
            var behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingId);
            kontrollerVedtakMedNyeRegisteropplysninger(behandling, behandlingsresultat, sakstype, erAvslag);
        } else {
            kontrollerVedtak(behandlingId, sakstype, erAvslag);
        }
    }

    public void kontrollerVedtakMedNyeRegisteropplysninger(Behandling behandling,
                                                           Behandlingsresultat behandlingsresultat, Sakstyper sakstype,
                                                           boolean erAvslag) throws ValideringException {
        hentNyeRegisteropplysninger(behandlingsresultat, behandling);
        kontrollerVedtak(behandling.getId(), sakstype, erAvslag);
    }

    private void hentNyeRegisteropplysninger(Behandlingsresultat behandlingsresultat, Behandling behandling) {
        Lovvalgsperiode lovvalgsperiode = behandlingsresultat.hentValidertLovvalgsperiode();
        String fnr = persondataFasade.hentFolkeregisterident(behandling.getFagsak().hentBrukersAktørID());

        registeropplysningerService.hentOgLagreOpplysninger(
            RegisteropplysningerRequest.builder()
                .behandlingID(behandling.getId())
                .fnr(fnr)
                .fom(lovvalgsperiode.getFom())
                .tom(lovvalgsperiode.getTom())
                .saksopplysningTyper(RegisteropplysningerRequest.SaksopplysningTyper.builder()
                    .medlemskapsopplysninger().build())
                .build());
    }

    private void kontrollerVedtak(long behandlingID, Sakstyper sakstype, boolean erAvslag) throws ValideringException {
        Collection<Kontrollfeil> kontrollfeil = utførKontroller(behandlingID, sakstype, erAvslag);
        if (!kontrollfeil.isEmpty()) {
            throw new ValideringException("Feil i validering. Kan ikke fatte vedtak.",
                kontrollfeil.stream().map(Kontrollfeil::tilDto).toList());
        }
    }

    public Collection<Kontrollfeil> utførKontroller(long behandlingID, Sakstyper sakstype, boolean erAvslag) {
        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingID);
        return erAvslag ? utførKontrollerForAvslag(behandling) : utførKontroller(behandling, sakstype);
    }

    private Collection<Kontrollfeil> utførKontrollerForAvslag(Behandling behandling) {
        Set<Function<VedtakKontrollData, Kontrollfeil>> vedtakKontroller =
            VedtakKontrollFactory.hentKontrollerForAvslag();
        var kontrollData = hentKontrollDataForAvslag(behandling);
        return vedtakKontroller.stream()
            .map(f -> f.apply(kontrollData))
            .filter(Objects::nonNull)
            .toList();
    }

    private Collection<Kontrollfeil> utførKontroller(Behandling behandling, Sakstyper sakstype) {
        Set<Function<VedtakKontrollData, Kontrollfeil>> vedtakKontroller =
            VedtakKontrollFactory.hentKontrollerForVedtak(sakstype);
        var vedtakKontrollData = hentVedtakKontrollData(behandling);
        return vedtakKontroller.stream()
            .map(f -> f.apply(vedtakKontrollData))
            .filter(Objects::nonNull)
            .toList();
    }

    private VedtakKontrollData hentKontrollDataForAvslag(Behandling behandling) {
        BehandlingsgrunnlagData behandlingsgrunnlagData =
            behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata();
        Persondata persondata = hentPersondata(behandling);
        return VedtakKontrollData.lagKontrollDataForAvslag(persondata, behandlingsgrunnlagData);
    }

    private VedtakKontrollData hentVedtakKontrollData(Behandling behandling) {
        Lovvalgsperiode lovvalgsperiode = lovvalgsperiodeService.hentValidertLovvalgsperiode(behandling.getId());
        Lovvalgsperiode opprinneligLovvalgsperiode =
            lovvalgsperiodeService.finnOpprinneligLovvalgsperiode(behandling.getId()).orElse(null);
        var behandlingsgrunnlagData = behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata();
        var medlemskapDokument = behandling.hentMedlemskapDokument();
        var persondata = hentPersondata(behandling);

        return new VedtakKontrollData(medlemskapDokument, persondata, behandlingsgrunnlagData,
            lovvalgsperiode, opprinneligLovvalgsperiode);
    }

    private Persondata hentPersondata(Behandling behandling) {
        return persondataFasade.hentPerson(behandling.getFagsak().hentBrukersAktørID());
    }
}
