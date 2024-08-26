import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.stereotype.Service;
import org.springframework.ldap.core.AttributesMapper;

import javax.naming.directory.Attributes;
import java.util.List;

@Service
public class LdapService {

    private final LdapTemplate ldapTemplate;

    @Autowired
    public LdapService(LdapTemplate ldapTemplate) {
        this.ldapTemplate = ldapTemplate;
    }

    public List<String> getGroupMembersSamAccountNames(String groupName) {
        // Filter to search the group by its 'cn' (common name)
        EqualsFilter filter = new EqualsFilter("cn", groupName);

        // Search for group members and fetch their 'sAMAccountName'
        return ldapTemplate.search("", filter.encode(), (Attributes attrs) -> {
            if (attrs != null && attrs.get("member") != null) {
                String memberDn = attrs.get("member").get().toString();
                return getSamAccountName(memberDn);
            }
            return null;
        });
    }

    private String getSamAccountName(String memberDn) {
        // Lookup the 'sAMAccountName' attribute directly in the same operation
        List<String> result = ldapTemplate.search(memberDn, "(objectClass=*)", SearchControls.OBJECT_SCOPE,
                new String[]{"sAMAccountName"}, (Attributes attrs) -> {
                    if (attrs != null && attrs.get("sAMAccountName") != null) {
                        return attrs.get("sAMAccountName").get().toString();
                    }
                    return null;
                });
        return result.isEmpty() ? null : result.get(0);
    }
}
