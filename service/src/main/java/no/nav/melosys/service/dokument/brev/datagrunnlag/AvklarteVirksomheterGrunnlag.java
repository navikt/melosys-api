package no.nav.melosys.service.dokument.brev.datagrunnlag;

import java.util.*;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;

public class AvklarteVirksomheterGrunnlag {
    private final Behandling behandling;
    private final AvklarteVirksomheterService avklarteVirksomheterService;

    // Microcachede verdier som kun eksisterer under brevbygging.
    // For å slippe å gjøre register- og kodeverksoppslag gjentatte ganger
    private List<AvklartVirksomhet> norskeVirksomheter;
    private List<AvklartVirksomhet> norskeArbeidsgivere;
    private List<AvklartVirksomhet> norskeSelvstendige;
    private List<AvklartVirksomhet> utenlandskeVirksomheter;

    public AvklarteVirksomheterGrunnlag(Behandling behandling,
                                        AvklarteVirksomheterService avklarteVirksomheterService) {
        this.behandling = behandling;
        this.avklarteVirksomheterService = avklarteVirksomheterService;
    }

    public List<AvklartVirksomhet> hentAlleNorskeVirksomheterMedAdresse() {
        if (norskeVirksomheter == null) {
            norskeVirksomheter = avklarteVirksomheterService.hentAlleNorskeVirksomheter(behandling);
        }
        return norskeVirksomheter;
    }

    public List<AvklartVirksomhet> hentNorskeArbeidsgivere() {
        if (norskeArbeidsgivere == null) {
            norskeArbeidsgivere = avklarteVirksomheterService.hentNorskeArbeidsgivere(behandling);
        }
        return norskeArbeidsgivere;
    }

    public List<AvklartVirksomhet> hentNorskeSelvstendige() {
        if (norskeSelvstendige == null) {
            norskeSelvstendige = avklarteVirksomheterService.hentNorskeSelvstendigeForetak(behandling);
        }
        return norskeSelvstendige;
    }

    public List<AvklartVirksomhet> hentUtenlandskeVirksomheter() {
        if (utenlandskeVirksomheter == null) {
            utenlandskeVirksomheter = avklarteVirksomheterService.hentUtenlandskeVirksomheter(behandling);
        }
        return utenlandskeVirksomheter;
    }

    public List<AvklartVirksomhet> hentUtenlandskeArbeidsgivere() {
        return hentUtenlandskeVirksomheter().stream()
            .filter(AvklartVirksomhet::erArbeidsgiver)
            .toList();
    }

    public List<AvklartVirksomhet> hentUtenlandskeSelvstendige() {
        return hentUtenlandskeVirksomheter().stream()
            .filter(AvklartVirksomhet::erSelvstendigForetak)
            .toList();
    }

    public Set<String> hentNorskeArbeidsgivendeOrgnumre() {
        return avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(behandling);
    }

    public AvklartVirksomhet hentHovedvirksomhet() {
        List<AvklartVirksomhet> alleNorskeVirksomheterList = hentAlleNorskeVirksomheterMedAdresse();
        List<AvklartVirksomhet> utenlandskeVirksomheterList = hentUtenlandskeVirksomheter();

        if (!alleNorskeVirksomheterList.isEmpty()) {
            return alleNorskeVirksomheterList.stream()
                .min(Comparator.comparing(AvklartVirksomhet::getNavn))
                .orElse(null);
        } else {
            return utenlandskeVirksomheterList.stream()
                .min(Comparator.comparing(AvklartVirksomhet::getNavn))
                .orElse(null);
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
