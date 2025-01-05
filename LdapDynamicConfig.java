@Configuration
public class LdapDynamicConfig {

    // Cache for domain-specific LdapTemplate instances
    private final Map<String, LdapTemplate> ldapTemplateCache = new HashMap<>();

    public LdapTemplate getLdapTemplateForDomain(String domain) {
        return ldapTemplateCache.computeIfAbsent(domain, this::createLdapTemplateForDomain);
    }

    private LdapTemplate createLdapTemplateForDomain(String domain) {
        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl("ldap://" + domain);
        contextSource.setBase("dc=" + domain.replace(".", ",dc="));
        contextSource.setUserDn("cn=ldap_user,ou=users,dc=" + domain.replace(".", ",dc="));
        contextSource.setPassword("password");
        contextSource.afterPropertiesSet();

        return new LdapTemplate(contextSource);
    }
}
