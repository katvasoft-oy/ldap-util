
package fi.katvasoft.ldaputil;

import fi.katvasoft.ldaputil.services.LdapService;
import org.beryx.textio.TextIO;
import org.beryx.textio.TextIoFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.ArrayList;
import java.util.List;

// TODO: Remove magic strings and write some tests

@SpringBootApplication
public class LdapHelperApplication implements CommandLineRunner {

    @Autowired
    LdapService ldapService;

	public static void main(String[] args) {
		SpringApplication.run(LdapHelperApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {

		TextIO textIO = TextIoFactory.getTextIO();

        List<String> roles = ldapService.listGroups();

        showCommands(roles,textIO);

	}

	private void showCommands(List<String> roles, TextIO textIO) {

        print("Ldap Util");

        String selection = textIO.newStringInputReader()
                .withNumberedPossibleValues(Commands.ADD_CMD, Commands.ADD_GROUP_CMD, Commands.ADD_USER_TO_GROUP_CMD, Commands.LIST_GROUPS_CMD, Commands.LIST_USERS_CMD, Commands.REMOVE_USER_CMD, Commands.REMOVE_GROUP_CMD,  Commands.QUIT_CMD )
                .read("Select what to do");

        switch (selection) {
            case Commands.ADD_CMD:
                addUserInput(textIO,roles);
                break;

            case Commands.ADD_GROUP_CMD:
                addGroup(textIO);
                break;

            case Commands.ADD_USER_TO_GROUP_CMD:
                addUserToGroup(textIO);
                break;

            case Commands.LIST_GROUPS_CMD:
                roles.forEach(x -> print(x));
                print("");
                break;

            case Commands.LIST_USERS_CMD:

                List<String> users = ldapService.listUsers();
                print("");
                users.forEach(usr -> print(usr));

                break;

            case Commands.REMOVE_USER_CMD:

                removeUser(textIO);

                break;


            case Commands.REMOVE_GROUP_CMD:

                removeGroup(textIO);

                break;

            case Commands.QUIT_CMD:

                print("Goodbye");
                System.exit(0);

            default:
                print("Nothing selected exiting");
                System.exit(0);
        }

        showCommands(roles,textIO);

    }

	private void removeUser(TextIO textIO) {

	    List<String> users = ldapService.listUsers();

	    String user = textIO.newStringInputReader().withNumberedPossibleValues(users).read("Select user to remove");

	    ldapService.removeObject(user);

	    print("User removed");
	    print("");

    }

    private void removeGroup(TextIO textIO) {

        List<String> groups = ldapService.listGroups();

        String group = textIO.newStringInputReader().withNumberedPossibleValues(groups).read("Select group to remove");

        ldapService.removeObject(group);

        print("Group removed");
        print("");

    }

    private void addUserToGroup(TextIO textIO)  {

        List<String> users = ldapService.listUsers();

        List<String> groups = ldapService.listGroups();

        String userName = textIO.newStringInputReader().withNumberedPossibleValues(users).read("Select user");

        String groupName = textIO.newStringInputReader().withNumberedPossibleValues(groups).read("Selected group");

        ldapService.addUserToGroup(userName,groupName);

        print(String.format("Added user %s to group %s",userName,groupName));

        print("");

    }

    private void addGroup(TextIO textIO) {

        List<String> users = ldapService.listUsers();

	    String groupName = textIO.newStringInputReader().read("Group name");

	    print("");

	    String user = textIO.newStringInputReader().withNumberedPossibleValues(users).read("Select user to add");

	    ldapService.addGroup(groupName, user);

	    print("Group added");
	    print("");

    }

	private void addUserInput(TextIO textIO, List<String> roles) {

	    List<String> selectedRoles = new ArrayList<>();

        String userName = textIO.newStringInputReader()
                        .read("Username:");

        print(String.format("Username %s", userName));


        String selectedGroup = textIO.newStringInputReader()
                                .withNumberedPossibleValues(roles)
                                .read("Select group for user: ");

        selectedRoles.add(selectedGroup);

        Boolean anotherGroup = true;

        while (anotherGroup) {

            anotherGroup = textIO.newBooleanInputReader().read("Do you want to add another group ?");
            if(anotherGroup) {
                selectedGroup = textIO.newStringInputReader()
                        .withNumberedPossibleValues(roles)
                        .read("Select group for user: ");
                selectedRoles.add(selectedGroup);
            }

        }

        print(String.format("Creating user with username : %s and following roles:", userName));

        selectedRoles.forEach(x -> print(x));

        Boolean correct = textIO.newBooleanInputReader().read("Is this correct ? ");
        if(correct) {
            String password = textIO.newStringInputReader().read("Password");
            ldapService.addUser(userName,password,selectedRoles);
            print(String.format("User %s added",userName));
            print("");
        }
    }

    public static void print(String txt) {
	    System.out.println(txt);
    }
}
