package no.nav.melosys.service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.mottatteopplysninger.AnmodningEllerAttest;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.util.MottatteOpplysningerUtils.*;

@Service
public class LandvelgerService {

    private final AvklartefaktaService avklartefaktaService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final MottatteOpplysningerService mottatteOpplysningerService;

    public LandvelgerService(AvklartefaktaService avklartefaktaService,
                             BehandlingsresultatService behandlingsresultatService,
                             MottatteOpplysningerService mottatteOpplysningerService) {
        this.avklartefaktaService = avklartefaktaService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.mottatteOpplysningerService = mottatteOpplysningerService;
    }

    public Land_iso2 hentArbeidsland(long behandlingID) {
        Collection<Land_iso2> alleArbeidsland = hentAlleArbeidslandUtenMarginaltArbeid(behandlingID);
        if (alleArbeidsland.size() != 1) {
            throw new FunksjonellException("Fant ingen eller flere enn ett arbeidsland");
        }
        return alleArbeidsland.iterator().next();
    }

    public String hentArbeidslandkodeFraAvklarteFaktaFTRL(long behandlingID) {
        Collection<String> alleArbeidsland = avklartefaktaService.hentAlleAvklarteArbeidslandFTRL(behandlingID);

        if (alleArbeidsland.size() != 1) {
            throw new FunksjonellException("Fant ingen eller flere enn ett arbeidsland");
        }

        return alleArbeidsland.iterator().next();
    }

    public Collection<Land_iso2> hentAlleArbeidsland(long behandlingID) {
        Collection<Land_iso2> alleArbeidsland = avklartefaktaService.hentAlleAvklarteArbeidsland(behandlingID);
        if (alleArbeidsland.isEmpty() || erArtikkel13(behandlingID)) {
            MottatteOpplysninger mottatteOpplysninger = mottatteOpplysningerService.hentMottatteOpplysninger(behandlingID);
            MottatteOpplysningerData grunnlagData = mottatteOpplysninger.getMottatteOpplysningerData();
            Behandling behandling = mottatteOpplysninger.getBehandling();
            var søknadsland = grunnlagData.soeknadsland;

            if (behandling.erAnmodningOmUnntak() && søknadsland.getLandkoder().isEmpty()) {
                alleArbeidsland.add(Land_iso2.valueOf(behandling.hentSedDokument().getUnntakFraLovvalgslandKode().getKode()));
            } else {
                alleArbeidsland.addAll(hentSøknadslandkoder(grunnlagData));
            }
        }

        return alleArbeidsland;
    }

    public boolean isFlereLandUkjentHvilke(long behandlingID) {
        MottatteOpplysningerData grunnlagData = mottatteOpplysningerService.hentMottatteOpplysninger(behandlingID).getMottatteOpplysningerData();
        return hentSøknadsland(grunnlagData).isFlereLandUkjentHvilke();
    }

    public Collection<Land_iso2> hentAlleArbeidslandUtenMarginaltArbeid(long behandlingID) {
        Collection<Land_iso2> alleArbeidsland = hentAlleArbeidsland(behandlingID);
        Collection<Land_iso2> landMedMarginaltArbeid = avklartefaktaService.hentLandkoderMedMarginaltArbeid(behandlingID);
        alleArbeidsland.removeAll(landMedMarginaltArbeid);

        return alleArbeidsland;
    }

    private boolean erArtikkel13(long behandlingId) {
        return erArtikkel13(behandlingsresultatService.hentBehandlingsresultat(behandlingId));
    }

    private boolean erArtikkel13(Behandlingsresultat behandlingsresultat) {
        if (behandlingsresultat.harPeriodeOmLovvalg()) {
            PeriodeOmLovvalg periodeOmLovvalg = behandlingsresultat.hentValidertPeriodeOmLovvalg();
            return periodeOmLovvalg.erArtikkel13();
        } else {
            return erVideresendt(behandlingsresultat);
        }
    }

    private boolean erArtikkel11_3aMed11_5Tilleggsbestemmelse(Behandlingsresultat behandlingsresultat) {
        return behandlingsresultat.finnLovvalgsperiode()
            .filter(Lovvalgsperiode::erArtikkel11_3aMed11_5Tilleggsbestemmelse)
            .isPresent();
    }

    private boolean erVideresendt(Behandlingsresultat behandlingsresultat) {
        Fagsak fagsak = behandlingsresultat.getBehandling().getFagsak();
        return fagsak.getStatus() == Saksstatuser.VIDERESENDT;
    }

    public Collection<Land_iso2> hentUtenlandskTrygdemyndighetsland(long behandlingID) {
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);

        if (erArtikkel13(behandlingsresultat) && !erVideresendt(behandlingsresultat)) {
            return hentUtenlandskTrygdemyndighetslandArtikkel13(behandlingsresultat);
        } else if (erArtikkel11_3aMed11_5Tilleggsbestemmelse(behandlingsresultat)) {
            return avklartefaktaService.hentInformertMyndighet(behandlingID).stream()
                .filter(landkode -> landkode != Land_iso2.NO).collect(Collectors.toSet());
        }

        Collection<Land_iso2> trygdemyndighetsland = hentTrygdemyndighetsland(behandlingsresultat);
        trygdemyndighetsland.remove(Land_iso2.NO);
        return trygdemyndighetsland;
    }

    private Collection<Land_iso2> hentUtenlandskTrygdemyndighetslandArtikkel13(Behandlingsresultat behandlingsresultat) {
        final long behandlingID = behandlingsresultat.getId();
        Set<Land_iso2> landkoderMedMarginaltArbeid = avklartefaktaService.hentLandkoderMedMarginaltArbeid(behandlingID);
        MottatteOpplysninger mottatteOpplysninger = mottatteOpplysningerService.hentMottatteOpplysninger(behandlingID);

        Stream<Land_iso2> marginaleArbeidslandMedUtenlandskArbeid = Stream.concat(
            mottatteOpplysninger.getMottatteOpplysningerData().hentUtenlandskeArbeidsstederLandkode().stream(),
            mottatteOpplysninger.getMottatteOpplysningerData().hentUtenlandskeArbeidsgivereLandkode().stream()
        ).map(Land_iso2::valueOf).filter(landkoderMedMarginaltArbeid::contains);

        Stream<Land_iso2> utpektLovvalgsland = behandlingsresultat.getUtpekingsperioder().stream()
            .map(Utpekingsperiode::getLovvalgsland)
            .map(landkoder -> Land_iso2.valueOf(landkoder.getKode()));

        return Streams.concat(
            marginaleArbeidslandMedUtenlandskArbeid,
            utpektLovvalgsland,
            hentTrygdemyndighetsland(behandlingsresultat).stream()
        ).filter(landkoder -> landkoder != Land_iso2.NO).collect(Collectors.toSet());
    }

    private Collection<Land_iso2> hentTrygdemyndighetsland(Behandlingsresultat behandlingsresultat) {
        final long behandlingID = behandlingsresultat.getId();
        MottatteOpplysningerData grunnlagdata = mottatteOpplysningerService.hentMottatteOpplysninger(behandlingID).getMottatteOpplysningerData();

        if (behandlingsresultat.erInnvilgetArbeidPåSkipOmfattetAvArbeidsland() || erVideresendt(behandlingsresultat)) {
            return Lists.newArrayList(Land_iso2.valueOf(hentBostedsland(behandlingID, grunnlagdata).landkode()));
        }
        if (grunnlagdata instanceof AnmodningEllerAttest anmodningEllerAttest) {
            return Collections.singleton(anmodningEllerAttest.getLovvalgsland());
        }
        return new ArrayList<>(hentAlleArbeidslandUtenMarginaltArbeid(behandlingID));
    }

    public Bostedsland hentBostedsland(Behandling behandling) {
        return hentBostedsland(behandling.getId(), behandling.getMottatteOpplysninger().getMottatteOpplysningerData());
    }

    public Bostedsland hentBostedsland(long behandlingID, MottatteOpplysningerData grunnlagData) {
        Optional<Bostedsland> bostedslandOppgittAvSaksbehandler = hentBostedslandOppgittAvSaksbehandler(behandlingID, grunnlagData);
        return bostedslandOppgittAvSaksbehandler.orElse(new Bostedsland(Landkoder.NO));
    }

    private Optional<Bostedsland> hentBostedslandOppgittAvSaksbehandler(long behandlingID, MottatteOpplysningerData grunnlagData) {
        Optional<Bostedsland> bostedsland = avklartefaktaService.hentBostedland(behandlingID);
        if (bostedsland.isPresent()) {
            return bostedsland;
        } else {
            return hentOppgittBostedsland(grunnlagData);
        }
    }
}
