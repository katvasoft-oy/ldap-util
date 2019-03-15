# LDAP Util



LDAP-util is a simple Java and Spring Boot based command line util for LDAP. I am not by no means a LDAP - professional but for me this has done the work. :) 

Currently it allows:

- Listing groups and users
- Removing users and groups
- Adding users
- Adding groups
- Adding users to groups

# How-TO

Create file called : **application.properties** in same directory as the jar-file is situated

Example ->
```properties   
ldap.contextSource.url=ldap:///
ldap.contextSource.base=cn=Users,dc=devad,dc=foo,dc=com
ldap.contextSource.userDn=cn=admin,dc=foo,dc=com
ldap.contextSource.password=secret
ldap.contextSource.groupSeachBase=cn=Users,dc=devad,DC=foo,DC=com

ldap.member.property=member

ldap.user.memberOf.property=memberOf
```
Properties explained:

| Property | Description |
| ------ | ------ |
| ldap.contextSource.url | Url of the LDAP|
| ldap.contextSource.userDn | LDAP Admin user DN |
| ldap.contextSource.password | Password of the LDAP Admin |
| ldap.contextSource.base | Base path of the users |
| ldap.contextSource.groupSeachBase | Base path of the groups |

### Tech

This simple util program uses following depencies:

* [TextIO] - Really great library for creating command line applications! ( https://text-io.beryx.org/releases/latest/ )
* [Spring Boot] - Make Spring Great again!
* [spring-security-ldap] - For LDAP thingies


### Installation

Just build the thing and you have a jar-file you can copy to any server etc. 

Then create or modify application.propeties (NOTE ! It must be in the same directory as the jar-file)

Then run :

```sh
$ java -jar ldap-helper.jar
```

### Todos

 - Write tests!
 - Simplify the LdapService structure and remove magic string etc.




