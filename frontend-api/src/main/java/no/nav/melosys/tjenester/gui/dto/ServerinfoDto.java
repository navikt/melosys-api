package no.nav.melosys.tjenester.gui.dto;

import java.util.Objects;

public class ServerinfoDto {
    private final String namespace;
    private final String cluster;
    private final String branchName;
    private final String longVersionHash;
    private final String veraUrl;

    public ServerinfoDto(String namespace, String cluster, String branchName, String longVersionHash, String veraUrl) {
        this.namespace = namespace;
        this.cluster = cluster;
        this.branchName = branchName;
        this.longVersionHash = longVersionHash;
        this.veraUrl = veraUrl;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getCluster() {
        return cluster;
    }

    public String getBranchName() {
        return branchName;
    }

    public String getLongVersionHash() {
        return longVersionHash;
    }

    public String getVeraUrl() {
        return veraUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServerinfoDto)) return false;
        ServerinfoDto that = (ServerinfoDto) o;
        return Objects.equals(getNamespace(), that.getNamespace()) &&
            Objects.equals(getCluster(), that.getCluster()) &&
            Objects.equals(getBranchName(), that.getBranchName()) &&
            Objects.equals(getLongVersionHash(), that.getLongVersionHash()) &&
            getVeraUrl().equals(that.getVeraUrl());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getNamespace(), getCluster(), getBranchName(), getLongVersionHash(), getVeraUrl());
    }

    @Override
    public String toString() {
        return "ServerinfoDto{" +
            "namespace='" + namespace + '\'' +
            ", cluster='" + cluster + '\'' +
            ", branchName='" + branchName + '\'' +
            ", longVersionHash='" + longVersionHash + '\'' +
            ", veraUrl='" + veraUrl + '\'' +
            '}';
    }
}
