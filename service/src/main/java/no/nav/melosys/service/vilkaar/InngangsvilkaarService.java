package no.nav.melosys.service.vilkaar;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Periode;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.StatsborgerskapPeriode;
import no.nav.melosys.domain.inngangsvilkar.Feilmelding;
import no.nav.melosys.domain.inngangsvilkar.InngangsvilkarResponse;
import no.nav.melosys.domain.kodeverk.begrunnelser.Inngangsvilkaar;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.inngangsvilkar.InngangsvilkaarConsumer;
import no.nav.melosys.service.SaksopplysningerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.kodeverk.Vilkaar.FO_883_2004_INNGANGSVILKAAR;
import static no.nav.melosys.domain.util.LandkoderUtils.tilIso3;
import static no.nav.melosys.service.registeropplysninger.RegisteropplysningerPeriodeFactory.REGISTEROPPLYSNINGER_DEFAULT_SLUTTDATO_ANTALL_ÅR;

@Service
public class InngangsvilkaarService {
    private static final Logger log = LoggerFactory.getLogger(InngangsvilkaarService.class);

    private final SaksopplysningerService saksopplysningerService;
    private final InngangsvilkaarConsumer inngangsvilkaarConsumer;
    private final VilkaarsresultatService vilkaarsresultatService;

    @Autowired
    public InngangsvilkaarService(SaksopplysningerService saksopplysningerService,
                                  InngangsvilkaarConsumer inngangsvilkaarConsumer,
                                  VilkaarsresultatService vilkaarsresultatService) {
        this.saksopplysningerService = saksopplysningerService;
        this.inngangsvilkaarConsumer = inngangsvilkaarConsumer;
        this.vilkaarsresultatService = vilkaarsresultatService;
    }

    public boolean oppfyllervurderingEF_883_2004(long behandlingID) {
        return vilkaarsresultatService.oppfyllerVilkaar(behandlingID, FO_883_2004_INNGANGSVILKAAR);
    }

    public boolean vurderOgLagreInngangsvilkår(long behandlingID,
                                               Collection<String> søknadsland,
                                               ErPeriode søknadsperiode) throws FunksjonellException {
        final InngangsvilkaarVurdering vurderingEF_883_2004 = vurderInngangsvilkår(behandlingID, søknadsland, søknadsperiode);
        final boolean erEF_883_2004 = vurderingEF_883_2004.isOppfylt();

        vilkaarsresultatService.oppdaterVilkaarsresultat(behandlingID, FO_883_2004_INNGANGSVILKAAR,
            erEF_883_2004, vurderingEF_883_2004.getBegrunnelseKode());
        return erEF_883_2004;
    }

    private InngangsvilkaarVurdering vurderInngangsvilkår(long behandlingID, Collection<String> søknadsland, ErPeriode søknadsperiode)
        throws FunksjonellException {
        Land statsborgerskap = hentStatsborgerskapForPerioden(behandlingID, søknadsperiode);
        if (statsborgerskap == null) {
            return new InngangsvilkaarVurdering(false, Inngangsvilkaar.MANGLER_STATSBORGERSKAP);
        }
        if (søknadsperiode.getTom() == null) {
            søknadsperiode = new Periode(søknadsperiode.getFom(), søknadsperiode.getFom().plusYears(REGISTEROPPLYSNINGER_DEFAULT_SLUTTDATO_ANTALL_ÅR));
        }

        var landkoderISO3 = Set.copyOf(tilIso3(søknadsland));
        InngangsvilkarResponse res = inngangsvilkaarConsumer.vurderInngangsvilkår(statsborgerskap, landkoderISO3, søknadsperiode);

        List<String> feilmeldinger = res.getFeilmeldinger().stream().map(Feilmelding::getMelding).collect(Collectors.toList());

        if (!feilmeldinger.isEmpty()) {
            if (log.isErrorEnabled()) {
                log.error("Vurdering av inngangsvilkår feilet: {}", String.join(System.lineSeparator(), feilmeldinger));
            }
            return new InngangsvilkaarVurdering(false, Inngangsvilkaar.TEKNISK_FEIL);
        } else {
            // Vurdering fra regelmodul gir ikke begrunnelser så langt.
            return new InngangsvilkaarVurdering(res.getKvalifisererForEf883_2004(), null);
        }
    }

    private Land hentStatsborgerskapForPerioden(long behandlingID, ErPeriode periode) throws IkkeFunnetException {
        // Hent statsborgerskap fra saksopplysningene...
        // Ved søknad tilbake i tid brukes historisk statsborgerskap
        if (periode.getFom().isBefore(LocalDate.now())) {
            return avgjørStatsborgerskapPåStartDato(
                saksopplysningerService.hentPersonhistorikk(behandlingID).statsborgerskapListe, periode.getFom());
        } else {
            return saksopplysningerService.hentPersonOpplysninger(behandlingID).statsborgerskap;
        }
    }

    Land avgjørStatsborgerskapPåStartDato(List<StatsborgerskapPeriode> statsborgerskapListe, LocalDate startDato) {
        if (statsborgerskapListe.isEmpty()) {
            return null;
        }
        List<StatsborgerskapPeriode> gyldigeStasborgerskap = statsborgerskapListe.stream()
            .filter(p -> p.getPeriode().inkluderer(startDato))
            .collect(Collectors.toList());
        if (gyldigeStasborgerskap.isEmpty()) {
            return null;
        } else if (gyldigeStasborgerskap.size() == 1) {
            return gyldigeStasborgerskap.get(0).statsborgerskap;
        } else {
            // Hvis det finnes flere kilder for samme dato så ønsker vi å se bort fra det som kommer fra Skattedirektoratet
            // pga. dårlig datakvalitet. Vi filterer også ukjent statsborgerskap siden det ikke hjelper å vurdere inngangsvilkår.
            return gyldigeStasborgerskap.stream().filter(p -> !p.erFraSkattedirektoratet())
                .filter(p -> !p.statsborgerskap.erUkjent())
                .max(Comparator.comparing(p -> p.endringstidspunkt))
                .map(p -> p.statsborgerskap).orElse(null);
        }
    }

    public void overstyrInngangsvilkår(long behandlingID) throws IkkeFunnetException {
        vilkaarsresultatService.oppdaterVilkaarsresultat(behandlingID, FO_883_2004_INNGANGSVILKAAR, true, Inngangsvilkaar.OVERSTYRT_AV_SAKSBEHANDLER);
    }
}
