package no.nav.melosys.integrasjon.joark.journalpostapi.dto;

public class Bruker {

    public enum BrukerIdType {
        FNR,
        ORGNR
    }

    private BrukerIdType idType;
    private String id;

    public Bruker(BrukerIdType idType, String id) {
        this.idType = idType;
        this.id = id;
    }

    public Bruker() {
    }

    public static BrukerBuilder builder() {
        return new BrukerBuilder();
    }

    public BrukerIdType getIdType() {
        return this.idType;
    }

    public String getId() {
        return this.id;
    }

    public static class BrukerBuilder {
        private BrukerIdType idType;
        private String id;

        BrukerBuilder() {
        }

        public Bruker.BrukerBuilder idType(BrukerIdType idType) {
            this.idType = idType;
            return this;
        }

        public Bruker.BrukerBuilder id(String id) {
            this.id = id;
            return this;
        }

        public Bruker build() {
            return new Bruker(idType, id);
        }
    }
}

