package fi.katvasoft.ldaputil.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;

@Configuration
public class LdapConfig {


    @Autowired
    Environment env;

    @Bean
    public LdapContextSource contextSource() {

        LdapContextSource contextSource = new LdapContextSource();
        if(!env.getProperty("ldap.contextSource.url","NOT_FOUND").equals("NOT_FOUND")) {
            contextSource.setUrl(env.getProperty("ldap.contextSource.url", "NOT_FOUND"));

            contextSource.setUserDn(env.getProperty("ldap.contextSource.userDn", "NOT_FOUND"));
            contextSource.setPassword(env.getProperty("ldap.contextSource.password", "NOT_FOUND"));
            return contextSource;
        } else {
            return null;
        }


    }

    @Bean()
    public LdapTemplate ldapTemplate() {
        if(contextSource() != null) {
            return new LdapTemplate(contextSource());
        } else {
            return null;

        }


    }


}
