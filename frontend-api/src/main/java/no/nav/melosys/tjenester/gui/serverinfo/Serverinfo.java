package no.nav.melosys.tjenester.gui.serverinfo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import no.nav.melosys.tjenester.gui.dto.ServerinfoDto;
import org.apache.commons.lang3.StringUtils;

public final class Serverinfo {
    public static final String FEIL = "Feil";
    private static final String VERA_URL_TEMPLATE = "https://vera.adeo.no/#/matrix?apps=melosys$&envs=%s:%s";
    private static final String NAMESPACE_ENV = "NAIS_NAMESPACE";
    private static final String CLUSTER_ENV = "NAIS_CLUSTER_NAME";
    private static final String IMAGE_ENV = "NAIS_APP_IMAGE";

    private static ServerinfoDto serverinfoDto;

    private Serverinfo() {
        throw new UnsupportedOperationException();
    }

    static ServerinfoDto tilDto() {
        if (serverinfoDto == null) {
            serverinfoDto = hentServerinfo();
        }
        return serverinfoDto;
    }

    private static ServerinfoDto hentServerinfo() {
        final String naisAppImage = System.getenv(IMAGE_ENV);
        final String namespace = System.getenv(NAMESPACE_ENV);
        final String cluster = System.getenv(CLUSTER_ENV);
        return new ServerinfoDto(
            namespace,
            cluster,
            hentBranch(naisAppImage),
            hentHash(naisAppImage),
            hentVeraUrl(namespace, cluster)
        );
    }

    static List<String> hentBranchOgHash(final String image) {
        if (StringUtils.isNotEmpty(image) && image.split(":").length == 3) {
            String imageTag = image.split(":")[2];
            return Arrays.asList(imageTag.split("-"));
        }
        return Collections.emptyList();
    }

    static String hentBranch(final String image) {
        List<String> branchOgHash = hentBranchOgHash(image);
        if (branchOgHash.size() > 1) {
            return branchOgHash.get(0) + "-" + branchOgHash.get(1);
        } else {
            return FEIL;
        }
    }

    static String hentHash(final String image) {
        List<String> branchOgHash = hentBranchOgHash(image);
        if (branchOgHash.size() > 2) {
            return branchOgHash.get(2);
        } else {
            return FEIL;
        }
    }

    static String hentVeraUrl(String namespace, String cluster)  {
        return String.format(VERA_URL_TEMPLATE, namespace, cluster);
    }
}
