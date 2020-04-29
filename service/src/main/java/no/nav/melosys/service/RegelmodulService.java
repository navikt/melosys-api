package no.nav.melosys.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.StatsborgerskapPeriode;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.dokument.soeknad.Soeknadsland;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.regelmodul.RegelmodulFasade;
import no.nav.melosys.regler.api.lovvalg.rep.Alvorlighetsgrad;
import no.nav.melosys.regler.api.lovvalg.rep.VurderInngangsvilkaarReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.util.LandkoderUtils.tilIso3;

/**
 * Service som kaller regelmodulen.
 */
@Service
public class RegelmodulService {
    private static final Logger log = LoggerFactory.getLogger(RegelmodulService.class);

    private final SaksopplysningerService saksopplysningerService;
    private final RegelmodulFasade regelmodulFasade;

    @Autowired
    public RegelmodulService(SaksopplysningerService saksopplysningerService,
                             RegelmodulFasade regelmodulFasade) {
        this.saksopplysningerService = saksopplysningerService;
        this.regelmodulFasade = regelmodulFasade;
    }

    public boolean kvalifisererForEF_883_2004(Long behandlingID, Soeknadsland søknadsland, Periode periode)
        throws FunksjonellException, TekniskException {
        return Boolean.TRUE.equals(vurderInngangsvilkår(behandlingID, søknadsland.landkoder, periode).kvalifisererForEf883_2004);
     }

    public VurderInngangsvilkaarReply vurderInngangsvilkår(long behandlingID, List<String> søknadsland, Periode søknadsperiode)
        throws FunksjonellException, TekniskException {
        Land statsborgerskap = hentStatsborgerskapForPerioden(behandlingID, søknadsperiode);
        if (statsborgerskap == null) {
            throw new FunksjonellException("Finner ingen informasjon om statsborgerskap");
        }

        VurderInngangsvilkaarReply res = regelmodulFasade.vurderInngangsvilkår(statsborgerskap, tilIso3(søknadsland), søknadsperiode);

        List<String> feilmeldinger = res.feilmeldinger.stream()
            .filter(feilmelding -> feilmelding.kategori.alvorlighetsgrad == Alvorlighetsgrad.FEIL)
            .map(feilmelding -> feilmelding.melding)
            .collect(Collectors.toList());

        if (!feilmeldinger.isEmpty()) {
            throw new FunksjonellException("Vurdering av inngangsvilkår feilet: " + String.join(System.lineSeparator(), feilmeldinger));
        }
        return res;
    }

    Land hentStatsborgerskapForPerioden(long behandlingID, Periode periode) throws IkkeFunnetException {
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
}
