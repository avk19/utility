import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.stereotype.Service;

import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LdapService {

    @Autowired
    private LdapTemplate ldapTemplate;

    public List<String> getGroupMembers(String groupName) {
        String groupDn = "cn=" + groupName + ",ou=groups,dc=example,dc=com";
        LdapQuery query = LdapQueryBuilder.query()
                .base(groupDn)
                .where("objectClass").is("group")
                .and("cn").is(groupName);

        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.OBJECT_SCOPE);
        searchControls.setReturningAttributes(new String[]{"member"});

        List<String> memberDNs = ldapTemplate.search(query, searchControls, (Attributes attributes) -> {
            if (attributes.get("member") != null) {
                return (String) attributes.get("member").get();
            }
            return null;
        });

        return memberDNs;
    }

    public List<String> getSamAccountNames(List<String> memberDNs) {
        return memberDNs.stream()
                .map(memberDn -> {
                    LdapQuery query = LdapQueryBuilder.query()
                            .base(memberDn)
                            .where("objectClass").is("person");

                    SearchControls searchControls = new SearchControls();
                    searchControls.setSearchScope(SearchControls.OBJECT_SCOPE);
                    searchControls.setReturningAttributes(new String[]{"sAMAccountName"});

                    List<String> samAccountNames = ldapTemplate.search(query, searchControls, (Attributes attributes) -> {
                        if (attributes.get("sAMAccountName") != null) {
                            return (String) attributes.get("sAMAccountName").get();
                        }
                        return null;
                    });

                    return samAccountNames.isEmpty() ? null : samAccountNames.get(0);
                })
                .filter(samAccountName -> samAccountName != null)
                .collect(Collectors.toList());
    }
}
