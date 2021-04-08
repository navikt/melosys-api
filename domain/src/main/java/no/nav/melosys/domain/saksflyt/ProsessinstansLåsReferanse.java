package no.nav.melosys.domain.saksflyt;

public interface ProsessinstansLåsReferanse {

    static ProsessinstansLåsReferanse tilReferanseObjekt(ProsessinstansLåsType låsType, String låsReferanse) {
        if (låsType == ProsessinstansLåsType.SED) {
            return new SedLåsReferanse(låsReferanse);
        }

        throw new IllegalArgumentException("Ukjent låstype: " + låsType);
    }

    String getReferanse();
}
