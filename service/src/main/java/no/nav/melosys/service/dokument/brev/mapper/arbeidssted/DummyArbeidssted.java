package no.nav.melosys.service.dokument.brev.mapper.arbeidssted;

public final class DummyArbeidssted extends AbstractArbeidssted implements IkkeFysiskArbeidssted {
    public DummyArbeidssted() {
        super("", "", "");
    }

    @Override
    public String getOmråde() {
        return "";
    }

    @Override
    public String getEnhetNavn() {
        return "";
    }

    @Override
    public String lagAdresselinje() {
        return " ";
    }
}
