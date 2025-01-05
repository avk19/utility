@RestController
@RequestMapping("/ldap")
public class LdapController {

    private final LdapService ldapService;

    public LdapController(LdapService ldapService) {
        this.ldapService = ldapService;
    }

    @GetMapping("/group-members")
    public List<String> getGroupMembers(
            @RequestParam String groupName,
            @RequestParam String domain
    ) {
        return ldapService.getGroupMembers(groupName, domain);
    }

    @GetMapping("/member-groups")
    public List<String> getMemberGroups(
            @RequestParam String userPrincipalName
    ) {
        String domain = ldapService.resolveDomain(userPrincipalName);
        String userDn = ldapService.getUserDn(userPrincipalName);
        return ldapService.getMemberGroups(userDn, domain);
    }
}
