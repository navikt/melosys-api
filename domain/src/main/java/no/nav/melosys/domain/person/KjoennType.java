package no.nav.melosys.domain.person;

public enum KjoennType {
    KVINNE("K"),
    MANN("M"),
    UKJENT("U");

    KjoennType(String kode) {
        this.kode = kode;
    }

    private final String kode;

    public String getKode() {
        return kode;
    }

    public static KjoennType avKode(String kode) {
        return switch (kode) {
            case "K" -> KVINNE;
            case "M" -> MANN;
            default -> UKJENT;
        };
    }

    @Override
    public String toString() {
        return switch (this) {
            case KVINNE -> "Kvinne";
            case MANN -> "Mann";
            default -> "Ukjent";
        };
    }
}
