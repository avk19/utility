@Service
public class LdapService {

    @Autowired
    private LdapDynamicConfig ldapDynamicConfig;

    /**
     * Fetch all members of a group using SAMAccountName, including cross-domain members.
     *
     * @param groupName the SAMAccountName of the group
     * @param domain the domain of the group
     * @return list of group members
     */
    public List<String> getGroupMembersWithSAMAccountName(String groupName, String domain) {
        LdapTemplate ldapTemplate = ldapDynamicConfig.getLdapTemplateForDomain(domain);

        // Fetch group DN based on SAMAccountName
        String groupDn = getGroupDnBySAMAccountName(groupName, ldapTemplate);

        if (groupDn == null) {
            throw new IllegalArgumentException("Group not found with SAMAccountName: " + groupName);
        }

        // Fetch members using the group DN
        List<String> memberSams = ldapTemplate.search(
            "",
            String.format("(memberOf=%s)", groupDn),
            (AttributesMapper<String>) attrs -> (String) attrs.get("sAMAccountName").get()
        );

        // Resolve cross-domain members
        List<String> resolvedMembers = new ArrayList<>();
        for (String memberSam : memberSams) {
            if (isCrossDomainMember(memberSam, domain)) {
                String crossDomain = extractDomainFromSam(memberSam);
                LdapTemplate crossDomainLdapTemplate = ldapDynamicConfig.getLdapTemplateForDomain(crossDomain);

                String resolvedMember = fetchUserDetailsBySAMAccountName(memberSam, crossDomainLdapTemplate);
                resolvedMembers.add(resolvedMember);
            } else {
                resolvedMembers.add(fetchUserDetailsBySAMAccountName(memberSam, ldapTemplate));
            }
        }

        return resolvedMembers;
    }

    /**
     * Fetch all groups for a given member using SAMAccountName.
     *
     * @param memberSam the SAMAccountName of the member
     * @param domain the domain of the member
     * @return list of groups the member belongs to
     */
    public List<String> getMemberGroupsBySAMAccountName(String memberSam, String domain) {
        LdapTemplate ldapTemplate = ldapDynamicConfig.getLdapTemplateForDomain(domain);

        // Fetch groups using member's SAMAccountName
        List<String> groups = ldapTemplate.search(
            "",
            String.format("(sAMAccountName=%s)", memberSam),
            (AttributesMapper<List<String>>) attrs -> {
                Attribute memberOfAttr = attrs.get("memberOf");
                if (memberOfAttr != null) {
                    List<String> groupDns = new ArrayList<>();
                    NamingEnumeration<?> groupEnum = memberOfAttr.getAll();
                    while (groupEnum.hasMore()) {
                        groupDns.add((String) groupEnum.next());
                    }
                    return groupDns;
                }
                return Collections.emptyList();
            }
        ).stream().flatMap(List::stream).collect(Collectors.toList());

        return groups.stream()
                .map(this::extractGroupNameFromDn)
                .collect(Collectors.toList());
    }

    /**
     * Fetch details of a user by SAMAccountName.
     */
    private String fetchUserDetailsBySAMAccountName(String samAccountName, LdapTemplate ldapTemplate) {
        List<String> results = ldapTemplate.search(
            "",
            String.format("(sAMAccountName=%s)", samAccountName),
            (AttributesMapper<String>) attrs -> (String) attrs.get("cn").get()
        );

        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Get the DN of a group using SAMAccountName.
     */
    private String getGroupDnBySAMAccountName(String groupName, LdapTemplate ldapTemplate) {
        List<String> results = ldapTemplate.search(
            "",
            String.format("(sAMAccountName=%s)", groupName),
            (AttributesMapper<String>) attrs -> (String) attrs.get("distinguishedName").get()
        );

        return results.isEmpty() ? null : results.get(0);
    }

    // Helper methods remain unchanged
    private boolean isCrossDomainMember(String memberSam, String currentDomain) {
        // Update logic to determine cross-domain using SAMAccountName if needed
        return !memberSam.toLowerCase().contains(currentDomain.toLowerCase());
    }

    private String extractDomainFromSam(String memberSam) {
        // Implement logic to extract domain from SAMAccountName if needed
        throw new UnsupportedOperationException("Domain extraction from SAMAccountName is not yet implemented");
    }

    private String extractGroupNameFromDn(String groupDn) {
        Pattern cnPattern = Pattern.compile("CN=([^,]+)");
        Matcher matcher = cnPattern.matcher(groupDn);

        if (matcher.find()) {
            return matcher.group(1);
        }

        throw new IllegalArgumentException("Unable to extract group name from DN: " + groupDn);
    }
}



