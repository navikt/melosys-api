package no.nav.melosys.service.dokument.brev.mapper.arbeidssted;

public interface IkkeFysiskArbeidssted extends Arbeidssted {
    /**
     * Område tilsvarer landkode for fysiske arbeidssteder,
     * men er en formatert landkode for maritimtarbeidssted.
     * Eks: for sokkel "offshore, <landkode>"
     **/
    String getOmråde();
}
