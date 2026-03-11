package no.nav.melosys.service.unntak;

import java.time.ZoneId;

public final class AnmodningUnntakKonstanter {
    private AnmodningUnntakKonstanter() {}

    public static final ZoneId TIME_ZONE_ID = ZoneId.systemDefault();
    public static final int SVARFRIST_MÅNEDER = 2;
}

