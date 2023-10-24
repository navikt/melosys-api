package no.nav.melosys.service.dokument.brev.datagrunnlag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.service.avklartefakta.OppsummerteAvklarteFaktaService;

public class AvklarteVirksomheterGrunnlag {
    private final Behandling behandling;
    private final OppsummerteAvklarteFaktaService oppsummerteAvklarteFaktaService;

    // Microcachede verdier som kun eksisterer under brevbygging.
    // For å slippe å gjøre register- og kodeverksoppslag gjentatte ganger
    private List<AvklartVirksomhet> norskeVirksomheter;
    private List<AvklartVirksomhet> norskeArbeidsgivere;
    private List<AvklartVirksomhet> norskeSelvstendige;
    private List<AvklartVirksomhet> utenlandskeVirksomheter;

    public AvklarteVirksomheterGrunnlag(Behandling behandling,
                                        OppsummerteAvklarteFaktaService oppsummerteAvklarteFaktaService) {
        this.behandling = behandling;
        this.oppsummerteAvklarteFaktaService = oppsummerteAvklarteFaktaService;
    }

    public List<AvklartVirksomhet> hentAlleNorskeVirksomheterMedAdresse() {
        if (norskeVirksomheter == null) {
            norskeVirksomheter = oppsummerteAvklarteFaktaService.hentAlleNorskeVirksomheter(behandling);
        }
        return norskeVirksomheter;
    }

    public List<AvklartVirksomhet> hentNorskeArbeidsgivere() {
        if (norskeArbeidsgivere == null) {
            norskeArbeidsgivere = oppsummerteAvklarteFaktaService.hentNorskeArbeidsgivere(behandling);
        }
        return norskeArbeidsgivere;
    }

    public List<AvklartVirksomhet> hentNorskeSelvstendige() {
        if (norskeSelvstendige == null) {
            norskeSelvstendige = oppsummerteAvklarteFaktaService.hentNorskeSelvstendigeForetak(behandling);
        }
        return norskeSelvstendige;
    }

    public List<AvklartVirksomhet> hentUtenlandskeVirksomheter() {
        if (utenlandskeVirksomheter == null) {
            utenlandskeVirksomheter = oppsummerteAvklarteFaktaService.hentUtenlandskeVirksomheter(behandling);
        }
        return utenlandskeVirksomheter;
    }

    public List<AvklartVirksomhet> hentUtenlandskeArbeidsgivere() {
        return hentUtenlandskeVirksomheter().stream()
            .filter(AvklartVirksomhet::erArbeidsgiver)
            .collect(Collectors.toList());
    }

    public List<AvklartVirksomhet> hentUtenlandskeSelvstendige() {
        return hentUtenlandskeVirksomheter().stream()
            .filter(AvklartVirksomhet::erSelvstendigForetak)
            .collect(Collectors.toList());
    }

    public Set<String> hentNorskeArbeidsgivendeOrgnumre() {
        return oppsummerteAvklarteFaktaService.hentNorskeArbeidsgivendeOrgnumre(behandling);
    }

    public AvklartVirksomhet hentHovedvirksomhet() {
        if (!hentAlleNorskeVirksomheterMedAdresse().isEmpty()) {
            return hentAlleNorskeVirksomheterMedAdresse().iterator().next();
        } else {
            return hentUtenlandskeVirksomheter().iterator().next();
        }
    }

    public int antallVirksomheter() {
        return hentAlleNorskeVirksomheterMedAdresse().size() +
               hentUtenlandskeVirksomheter().size();
    }

    public Collection<AvklartVirksomhet> hentBivirksomheter() {
        Collection<AvklartVirksomhet> bivirksomheter = new ArrayList<>();
        bivirksomheter.addAll(hentAlleNorskeVirksomheterMedAdresse());
        bivirksomheter.addAll(hentUtenlandskeVirksomheter());
        bivirksomheter.remove(hentHovedvirksomhet());
        return bivirksomheter;
    }
}
