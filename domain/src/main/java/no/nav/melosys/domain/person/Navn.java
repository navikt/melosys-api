package no.nav.melosys.domain.person;

public record Navn(String fornavn, String mellomnavn, String etternavn) {
    public String tilSammensattNavn() {
        return etternavn + leggTilMellomnavn() + " " + fornavn;
    }

    private String leggTilMellomnavn() {
        return mellomnavn == null ? "" : " " + mellomnavn();
    }
}
