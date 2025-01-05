@Service
public class LdapService {

    @Autowired
    private LdapDynamicConfig ldapDynamicConfig;

    /**
     * Fetches all members of a given group, including cross-domain members.
     *
     * @param groupName the name of the group
     * @param domain the domain of the group
     * @return list of members
     */
    public List<String> getGroupMembersWithCrossDomain(String groupName, String domain) {
        LdapTemplate ldapTemplate = ldapDynamicConfig.getLdapTemplateForDomain(domain);

        // Search filter to fetch members of the group
        Filter groupFilter = new EqualsFilter("cn", groupName);
        String baseDn = ""; // Adjust based on your AD structure

        // Fetch members from the intranet domain
        List<String> memberDns = ldapTemplate.search(
            baseDn,
            groupFilter.encode(),
            (AttributesMapper<String>) attrs -> (String) attrs.get("member").get()
        );

        List<String> resolvedMembers = new ArrayList<>();

        for (String memberDn : memberDns) {
            if (isCrossDomainMember(memberDn, domain)) {
                // Fetch details of cross-domain members
                String crossDomain = extractDomainFromDn(memberDn);
                LdapTemplate crossDomainLdapTemplate = ldapDynamicConfig.getLdapTemplateForDomain(crossDomain);

                String resolvedMember = fetchUserDetails(memberDn, crossDomainLdapTemplate);
                resolvedMembers.add(resolvedMember);
            } else {
                resolvedMembers.add(memberDn);
            }
        }

        return resolvedMembers;
    }

    /**
     * Checks if the member belongs to a different domain.
     */
    private boolean isCrossDomainMember(String memberDn, String currentDomain) {
        return !memberDn.toLowerCase().contains(currentDomain.toLowerCase());
    }

    /**
     * Extracts the domain from the DN.
     */
    private String extractDomainFromDn(String memberDn) {
        Pattern domainPattern = Pattern.compile("DC=([a-zA-Z0-9]+),DC=([a-zA-Z0-9]+)");
        Matcher matcher = domainPattern.matcher(memberDn);

        if (matcher.find()) {
            return matcher.group(1) + "." + matcher.group(2);
        }

        throw new IllegalArgumentException("Unable to extract domain from DN: " + memberDn);
    }

    /**
     * Fetches user details for a given DN from the specified domain.
     */
    private String fetchUserDetails(String userDn, LdapTemplate ldapTemplate) {
        return ldapTemplate.lookup(userDn, (AttributesMapper<String>) attrs -> {
            // Fetch specific attributes of the user
            return (String) attrs.get("cn").get();
        });
    }
}

