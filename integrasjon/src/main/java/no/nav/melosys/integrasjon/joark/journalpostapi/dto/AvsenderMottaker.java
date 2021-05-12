package no.nav.melosys.integrasjon.joark.journalpostapi.dto;

import no.nav.melosys.domain.kodeverk.Avsendertyper;
import no.nav.melosys.exception.TekniskException;

public class AvsenderMottaker {

    private String id;
    private String navn;
    private String land;
    private IdType idType;

    public AvsenderMottaker(String id, String navn, String land, IdType idType) {
        this.id = id;
        this.navn = navn;
        this.land = land;
        this.idType = idType;
    }

    public AvsenderMottaker() {
    }

    public static AvsenderMottakerBuilder builder() {
        return new AvsenderMottakerBuilder();
    }

    public String getId() {
        return this.id;
    }

    public String getNavn() {
        return this.navn;
    }

    public String getLand() {
        return this.land;
    }

    public IdType getIdType() {
        return this.idType;
    }

    public enum IdType {
        FNR, ORGNR, HPRNR, UTL_ORG
    }

    public static class AvsenderMottakerBuilder {
        private String id;
        private String navn;
        private String land;
        private IdType idType;

        AvsenderMottakerBuilder() {
        }

        public AvsenderMottaker.AvsenderMottakerBuilder id(String id) {
            this.id = id;
            return this;
        }

        public AvsenderMottaker.AvsenderMottakerBuilder navn(String navn) {
            this.navn = navn;
            return this;
        }

        public AvsenderMottaker.AvsenderMottakerBuilder land(String land) {
            this.land = land;
            return this;
        }

        public AvsenderMottaker.AvsenderMottakerBuilder idType(IdType idType) {
            this.idType = idType;
            return this;
        }

        public AvsenderMottaker build() {
            return new AvsenderMottaker(id, navn, land, idType);
        }
    }

    public static AvsenderMottaker.IdType tilAvsenderMottakerIdType(Avsendertyper avsendertype) {
        switch (avsendertype) {
            case PERSON:
                return AvsenderMottaker.IdType.FNR;
            case ORGANISASJON:
                return AvsenderMottaker.IdType.ORGNR;
            case UTENLANDSK_TRYGDEMYNDIGHET:
                return AvsenderMottaker.IdType.UTL_ORG;
            default:
                throw new TekniskException("AvsenderType " + avsendertype + " støttes ikke.");
        }
    }
}

