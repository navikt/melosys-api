package no.nav.melosys.integrasjon.joark.journalpostapi.dto;

public class Tilleggsopplysning {

    private String nokkel;
    private String verdi;

    public Tilleggsopplysning(String nokkel, String verdi) {
        this.nokkel = nokkel;
        this.verdi = verdi;
    }

    public Tilleggsopplysning() {
    }

    public static TilleggsopplysningBuilder builder() {
        return new TilleggsopplysningBuilder();
    }

    public String getNokkel() {
        return this.nokkel;
    }

    public String getVerdi() {
        return this.verdi;
    }

    public static class TilleggsopplysningBuilder {
        private String nokkel;
        private String verdi;

        TilleggsopplysningBuilder() {
        }

        public Tilleggsopplysning.TilleggsopplysningBuilder nokkel(String nokkel) {
            this.nokkel = nokkel;
            return this;
        }

        public Tilleggsopplysning.TilleggsopplysningBuilder verdi(String verdi) {
            this.verdi = verdi;
            return this;
        }

        public Tilleggsopplysning build() {
            return new Tilleggsopplysning(nokkel, verdi);
        }
    }
}
