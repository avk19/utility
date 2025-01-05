@Service
public class LdapService {

    @Autowired
    private LdapDynamicConfig ldapDynamicConfig;

    /**
     * Fetch all members of a group, including cross-domain members.
     *
     * @param groupName the group name
     * @param domain the domain of the group
     * @return list of group members
     */
    public List<String> getGroupMembersWithMemberOf(String groupName, String domain) {
        LdapTemplate ldapTemplate = ldapDynamicConfig.getLdapTemplateForDomain(domain);

        // Fetch the DN of the group
        String groupDn = getGroupDn(groupName, ldapTemplate);

        if (groupDn == null) {
            throw new IllegalArgumentException("Group not found: " + groupName);
        }

        // Fetch members of the group
        List<String> memberDns = ldapTemplate.search(
            "", 
            String.format("(memberOf=%s)", groupDn), 
            (AttributesMapper<String>) attrs -> (String) attrs.get("distinguishedName").get()
        );

        // Resolve cross-domain members
        List<String> resolvedMembers = new ArrayList<>();
        for (String memberDn : memberDns) {
            if (isCrossDomainMember(memberDn, domain)) {
                String crossDomain = extractDomainFromDn(memberDn);
                LdapTemplate crossDomainLdapTemplate = ldapDynamicConfig.getLdapTemplateForDomain(crossDomain);

                String resolvedMember = fetchUserDetails(memberDn, crossDomainLdapTemplate);
                resolvedMembers.add(resolvedMember);
            } else {
                resolvedMembers.add(fetchUserDetails(memberDn, ldapTemplate));
            }
        }

        return resolvedMembers;
    }

    /**
     * Fetch all groups for a given member using the memberOf attribute.
     *
     * @param memberPrincipalName the user principal name of the member
     * @param domain the domain of the member
     * @return list of groups the member belongs to
     */
    public List<String> getMemberGroupsWithMemberOf(String memberPrincipalName, String domain) {
        LdapTemplate ldapTemplate = ldapDynamicConfig.getLdapTemplateForDomain(domain);

        // Fetch DN of the member
        String memberDn = getUserDn(memberPrincipalName, ldapTemplate);

        if (memberDn == null) {
            throw new IllegalArgumentException("User not found: " + memberPrincipalName);
        }

        // Use memberOf to fetch groups
        List<String> groups = ldapTemplate.search(
            "", 
            String.format("(distinguishedName=%s)", memberDn), 
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

    // Helper methods remain the same as in the previous version
    private boolean isCrossDomainMember(String memberDn, String currentDomain) {
        return !memberDn.toLowerCase().contains(currentDomain.toLowerCase());
    }

    private String extractDomainFromDn(String memberDn) {
        Pattern domainPattern = Pattern.compile("DC=([a-zA-Z0-9]+),DC=([a-zA-Z0-9]+)");
        Matcher matcher = domainPattern.matcher(memberDn);

        if (matcher.find()) {
            return matcher.group(1) + "." + matcher.group(2);
        }

        throw new IllegalArgumentException("Unable to extract domain from DN: " + memberDn);
    }

    private String fetchUserDetails(String userDn, LdapTemplate ldapTemplate) {
        return ldapTemplate.lookup(userDn, (AttributesMapper<String>) attrs -> {
            return (String) attrs.get("cn").get();
        });
    }

    private String getUserDn(String userPrincipalName, LdapTemplate ldapTemplate) {
        List<String> results = ldapTemplate.search(
            "",
            String.format("(userPrincipalName=%s)", userPrincipalName),
            (AttributesMapper<String>) attrs -> (String) attrs.get("distinguishedName").get()
        );

        return results.isEmpty() ? null : results.get(0);
    }

    private String getGroupDn(String groupName, LdapTemplate ldapTemplate) {
        List<String> results = ldapTemplate.search(
            "",
            String.format("(cn=%s)", groupName),
            (AttributesMapper<String>) attrs -> (String) attrs.get("distinguishedName").get()
        );

        return results.isEmpty() ? null : results.get(0);
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


