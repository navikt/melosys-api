package no.nav.melosys.domain.person;

import java.util.Arrays;

public record Navn(String fornavn, String mellomnavn, String etternavn) {
    public String tilSammensattNavn() {
        return (etternavn + leggTilMellomnavn() + " " + fornavn).trim();
    }

    private String leggTilMellomnavn() {
        return mellomnavn == null ? "" : " " + mellomnavn();
    }


    // A B C -> C, A B
    public static String navnEtternavnFørst(String fulltNavnEtternavnSist) {
        String[] splittetNavn = fulltNavnEtternavnSist.split(" ");
        var etternavn = splittetNavn[splittetNavn.length - 1];
        var forOgMellomnavn = String.join(" ", Arrays.copyOf(splittetNavn, splittetNavn.length - 1));
        return String.format("%s, %s", etternavn, forOgMellomnavn);
    }

    // C, A B -> A B C
    public static String navnEtternavnSist(String fulltNavnEtternavnFørst) {
        String[] splittetNavn = fulltNavnEtternavnFørst.split(" ");
        var etternavn = splittetNavn[0].substring(0, splittetNavn[0].length() - 1);
        var forOgMellomnavn = String.join(" ", Arrays.copyOfRange(splittetNavn, 1, splittetNavn.length));
        return String.format("%s %s", forOgMellomnavn, etternavn);
    }
}
