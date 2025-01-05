@Service
public class LdapService {

    private final LdapDynamicConfig ldapDynamicConfig;

    public LdapService(LdapDynamicConfig ldapDynamicConfig) {
        this.ldapDynamicConfig = ldapDynamicConfig;
    }

    /**
     * Retrieve members of a group by its name.
     */
    public List<String> getGroupMembers(String groupName, String domain) {
        LdapTemplate ldapTemplate = ldapDynamicConfig.getLdapTemplateForDomain(domain);

        return ldapTemplate.search(
            query().base("ou=Groups")
                    .where("objectClass").is("group")
                    .and("cn").is(groupName),
            (AttributesMapper<String>) attrs -> (String) attrs.get("member").get()
        );
    }

    /**
     * Retrieve all groups a member belongs to.
     */
    public List<String> getMemberGroups(String memberDn, String domain) {
        LdapTemplate ldapTemplate = ldapDynamicConfig.getLdapTemplateForDomain(domain);

        return ldapTemplate.search(
            query().base("ou=Groups")
                    .where("objectClass").is("group")
                    .and("member").is(memberDn),
            (AttributesMapper<String>) attrs -> (String) attrs.get("cn").get()
        );
    }

    /**
     * Resolve domain dynamically based on the userPrincipalName.
     */
    public String resolveDomain(String userPrincipalName) {
        if (userPrincipalName == null || !userPrincipalName.contains("@")) {
            throw new IllegalArgumentException("Invalid userPrincipalName format");
        }
        return userPrincipalName.split("@")[1];
    }

    /**
     * Helper to get user DN dynamically from any domain.
     */
    public String getUserDn(String userPrincipalName) {
        String domain = resolveDomain(userPrincipalName);
        LdapTemplate ldapTemplate = ldapDynamicConfig.getLdapTemplateForDomain(domain);

        List<String> results = ldapTemplate.search(
            query().base("ou=Users")
                    .where("userPrincipalName").is(userPrincipalName),
            (AttributesMapper<String>) attrs -> (String) attrs.get("distinguishedName").get()
        );

        if (results.isEmpty()) {
            throw new IllegalArgumentException("User not found in domain: " + domain);
        }

        return results.get(0);
    }
}
