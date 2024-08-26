import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapNameBuilder;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.stereotype.Service;

import javax.naming.Name;
import javax.naming.directory.Attributes;
import java.util.ArrayList;
import java.util.List;

@Service
public class LdapService {

    private final LdapTemplate ldapTemplate;

    @Autowired
    public LdapService(LdapTemplate ldapTemplate) {
        this.ldapTemplate = ldapTemplate;
    }

    public List<String> getGroupMembers(String groupName) {
        EqualsFilter filter = new EqualsFilter("cn", groupName);

        List<String> memberDns = ldapTemplate.search("", filter.encode(), (Attributes attrs) -> {
            if (attrs != null) {
                return attrs.get("member").get().toString();
            }
            return null;
        });

        List<String> samAccountNames = new ArrayList<>();
        for (String memberDn : memberDns) {
            String samAccountName = getSamAccountName(memberDn);
            if (samAccountName != null) {
                samAccountNames.add(samAccountName);
            }
        }
        return samAccountNames;
    }

    private String getSamAccountName(String memberDn) {
        Name dn = LdapNameBuilder.newInstance(memberDn).build();

        return ldapTemplate.lookup(dn, new String[]{"sAMAccountName"}, (Attributes attrs) -> {
            if (attrs != null) {
                return attrs.get("sAMAccountName").get().toString();
            }
            return null;
        });
    }
}
