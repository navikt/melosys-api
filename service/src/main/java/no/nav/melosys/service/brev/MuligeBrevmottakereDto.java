package no.nav.melosys.service.brev;

import java.util.List;

import no.nav.melosys.domain.brev.muligemottakere.Brevmottaker;

@Deprecated(since = "Slett klasse sammen med melosys.MEL-4835.refactor1 toggle")
public record MuligeBrevmottakereDto(
    Brevmottaker hovedMottaker,
    List<Brevmottaker> kopiMottakere,
    List<Brevmottaker> fasteMottakere) {

    public Brevmottaker getHovedMottaker() {
        return hovedMottaker;
    }

    public List<Brevmottaker> getKopiMottakere() {
        return kopiMottakere;
    }

    public List<Brevmottaker> getFasteMottakere() {
        return fasteMottakere;
    }
}
