package fi.katvasoft.ldaputil.services;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.ldap.support.LdapNameBuilder;
import org.springframework.stereotype.Component;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;



@Component
public class LdapService {

    private final LdapTemplate ldapTemplate;

    private final Environment env;

    private final String memberOfProperty;

    private final String groupBase;

    @Autowired
    public LdapService(LdapTemplate localLdapTemplate, Environment env) {
        this.ldapTemplate = localLdapTemplate;
        this.env = env;
        memberOfProperty = env.getProperty("ldap.user.memberOf.property");

        groupBase = env.getProperty("ldap.contextSource.groupSeachBase");
    }

    public List<String> listUsers() {

        LdapQuery query = LdapQueryBuilder.query()
                .base(getContextSourceBaseProperty())
                .filter(getOrgPersonFilter());



        List<String> users = ldapTemplate.search(query, (Attributes attrs) -> {
            return tryGetUsers(attrs);
        }).stream().flatMap(Collection::stream).collect(Collectors.toList());

        return users;
    }

    public void removeObject(String objectName) {

        String userdn = createDn(objectName);

        Name dn = LdapNameBuilder.newInstance()
                    .add(userdn).build();

        ldapTemplate.unbind(dn);

    }

    public List<String> listGroups() {

        LdapQuery query = LdapQueryBuilder.query()
                .base(getContextSourceBaseProperty())
                .filter(getGroupOfNamesFilter());



        List<String> roles = ldapTemplate.search(query, (Attributes attrs) -> {
               return tryGetAdGroups(attrs);
        }).stream().flatMap(Collection::stream).collect(Collectors.toList());

        return roles;
    }

    public void addUserToGroup(String userName, String groupName) {

        String userDn = createDn(userName);

        attachUserToGroup(userDn,groupName);

        attachGroupToUser(userName,groupName);

    }

    public void addGroup(String groupName, String username) {

        String userDn = createDn(username);
        createGroup(groupName,userDn);


    }

    public void addUser(String username, String password ,List<String> groups) {

        createUser(username, password, groups);

        groups.forEach(role -> attachUserToGroup(createDn(username),role));

    }

    private void attachUserToGroup(String usernameDn, String groupName) {

        Name dn = LdapNameBuilder
                .newInstance()
                .add(groupBase)
                .add("cn", groupName)
                .build();


        DirContextOperations context = ldapTemplate.lookupContext(dn);

        String propertyName = env.getProperty("ldap.member.property");

        context.addAttributeValue(propertyName, usernameDn);

        ldapTemplate.modifyAttributes(context);

    }

    private void attachGroupToUser(String userName, String groupName) {

        Name dn = LdapNameBuilder
                .newInstance()
                .add(groupBase)
                .add("cn", userName)
                .build();


        DirContextOperations context = ldapTemplate.lookupContext(dn);



        context.addAttributeValue(memberOfProperty, createDn(groupName));

        ldapTemplate.modifyAttributes(context);

    }

    private void createUser(String username, String password, List<String> groups) {

        List<String> roleDns = groups.stream().map(x -> createDn(x)).collect(Collectors.toList());

        Name dn = LdapNameBuilder
                .newInstance()
                .add(groupBase)
                .add("cn", username)

                .build();
        DirContextAdapter context = new DirContextAdapter(dn);

        context.setAttributeValues(
                "objectclass",
                new String[]
                        { "top",
                                "person",
                                "inetOrgPerson" });

        context.setAttributeValue("cn", username);
        context.setAttributeValue("sn", username);
        roleDns.forEach(val -> context.addAttributeValue(memberOfProperty,val));
        context.setAttributeValue
                ("userPassword", digestSHA(password));

        ldapTemplate.bind(context);


    }



    private void createGroup(String groupName, String userDn) {

        Name dn = LdapNameBuilder
                .newInstance()
                .add(groupBase)
                .add("cn", groupName)
                .build();

        DirContextAdapter context = new DirContextAdapter(dn);
        context.setAttributeValues(
                "objectclass",
                new String[]
                        { "top",
                                "groupOfNames"});

        context.setAttributeValue("cn", groupName);
        context.setAttributeValue(env.getProperty("ldap.member.property"), userDn);
        ldapTemplate.bind(context);

    }

    private String digestSHA(final String password) {
        String base64;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA");
            digest.update(password.getBytes());
            base64 = Base64
                    .getEncoder()
                    .encodeToString(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return "{SHA}" + base64;
    }

    private AndFilter getMemberOfFilter(String query) {
        return new AndFilter().and(new EqualsFilter("memberOf", query));
    }

    private AndFilter getGroupOfNamesFilter() {
        return new AndFilter().and(new EqualsFilter("objectClass", "groupOfNames"));
    }

    private AndFilter getOrgPersonFilter() {
        return new AndFilter().and(new EqualsFilter("objectClass", "inetOrgPerson"));
    }

    private String getContextSourceBaseProperty() {
        return env.getProperty("ldap.contextSource.base");
    }

    private String createDn(String roleName) {
      return String.format("cn=%s,%s", roleName,groupBase);
    }

    private String getGroupName(String groupCnName) {

        StringTokenizer comma = new StringTokenizer(groupCnName,",");
        String dnName = comma.nextToken();
        StringTokenizer equals = new StringTokenizer(dnName,"=");
        equals.nextToken();
        String groupName = equals.nextToken();
        return groupName;

    }



    private List<String> tryGetAdGroups(Attributes attributes) {
        try {
            Attribute attribute = attributes.get("cn");
            List<String> adGroupNames = new ArrayList<>();
            try {
                NamingEnumeration names = attribute.getAll();
                while (names.hasMore()) {

                    String dnGroupName = (String)names.next();
                    if(dnGroupName.contains("role_")) {
                        String className = attribute.getClass().getName();

                        adGroupNames.add(dnGroupName);
                    }


                }
            } catch (NamingException ne) {
                return null;
            }
            return adGroupNames;
        } catch (Exception exp) {
            return null;
        }

    }

    private List<String> tryGetUsers(Attributes attributes) {
        try {
            Attribute attribute = attributes.get("cn");
            List<String> userNames = new ArrayList<>();
            try {
                NamingEnumeration names = attribute.getAll();
                while (names.hasMore()) {

                    String name = (String)names.next();
                    if(!name.contains("Use")) {
                        userNames.add(name);
                    }


                }
            } catch (NamingException ne) {
                return null;
            }
            return userNames;
        } catch (Exception exp) {
            return null;
        }

    }
}
