import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.stereotype.Service;
import org.springframework.ldap.core.AttributesMapper;

import javax.naming.directory.SearchControls;
import javax.naming.directory.Attributes;
import java.util.List;

@Service
public class LdapService {

    @Autowired
    private LdapTemplate ldapTemplate;

    public List<String> getGroupMembers(String groupName) {
        EqualsFilter filter = new EqualsFilter("cn", groupName);
        
        return ldapTemplate.search("", filter.encode(), getGroupMemberAttributesMapper());
    }

    private AttributesMapper<String> getGroupMemberAttributesMapper() {
        return attrs -> {
            if (attrs != null) {
                try {
                    return attrs.get("member").get().toString();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        };
    }

    public String getSamAccountName(String memberDn) {
        EqualsFilter filter = new EqualsFilter("objectClass", "user");

        List<String> result = ldapTemplate.search(memberDn, filter.encode(), SearchControls.OBJECT_SCOPE, 
            new String[]{"sAMAccountName"}, (Attributes attrs) -> {
                if (attrs != null) {
                    try {
                        return attrs.get("sAMAccountName").get().toString();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return null;
            });

        return result.isEmpty() ? null : result.get(0);
    }
}
