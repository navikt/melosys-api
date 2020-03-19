package no.nav.melosys.integrasjon.ldap;

import java.util.Collection;
import java.util.Objects;

public class LdapBruker {
    private final String displayName;
    private final Collection<String> groups;

    public LdapBruker(String displayName, Collection<String> groups) {
        this.displayName = displayName;
        this.groups = groups;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Collection<String> getGroups() {
        return groups;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LdapBruker that = (LdapBruker) o;
        return Objects.equals(displayName, that.displayName) &&
            Objects.equals(groups, that.groups);
    }

    @Override
    public int hashCode() {
        return Objects.hash(displayName, groups);
    }
}
