package usi2011.domain;

import static org.apache.commons.lang.builder.EqualsBuilder.reflectionEquals;
import static org.apache.commons.lang.builder.ToStringBuilder.reflectionToString;
import static org.apache.commons.lang.builder.ToStringStyle.MULTI_LINE_STYLE;
import static usi2011.util.Json.jsonEscape;
import static usi2011.util.Specifications.USER_EMAIL;
import static usi2011.util.Specifications.USER_FIRSTNAME;
import static usi2011.util.Specifications.USER_LASTNAME;
import static usi2011.util.Specifications.USER_PASSWORD;
import usi2011.exception.UserException;
import usi2011.util.JSONBackedObject;
import usi2011.util.Json;


public final class User extends JSONBackedObject {
    private final String email;
    private final String firstName;
    private final String lastName;
    private final String password;
    private final String emailJsonEscaped;
    private final String firstNameJsonEscaped;
    private final String lastNameJsonEscaped;
    private final String passwordJsonEscaped;
    //le timestamp du dernier jeu sur lequel le user s'est loggé...
    private final String loggedInGame;

    public User(String json) {
        this(new Json(json));
    }

    public User(String email, String firstName, String lastName, String password) {
        this.email = assertEmail(email);
        this.firstName = assertLength(USER_FIRSTNAME, firstName);
        this.lastName = assertLength(USER_LASTNAME, lastName);
        this.password = assertLength(USER_PASSWORD, password);
        this.emailJsonEscaped = jsonEscape(email);
        this.firstNameJsonEscaped = jsonEscape(firstName);
        this.lastNameJsonEscaped = jsonEscape(lastName);
        this.passwordJsonEscaped = jsonEscape(password);
        this.loggedInGame = null;
    }

    public User(Json json) {
        this.email = getEmail(json, USER_EMAIL);
        this.firstName = getAndAssertString(json, USER_FIRSTNAME);
        this.lastName = getAndAssertString(json, USER_LASTNAME);
        this.password = getAndAssertString(json, USER_PASSWORD);
        this.emailJsonEscaped = jsonEscape(email);
        this.firstNameJsonEscaped = jsonEscape(firstName);
        this.lastNameJsonEscaped = jsonEscape(lastName);
        this.passwordJsonEscaped = jsonEscape(password);
        this.loggedInGame = null;
    }

    public User(String email, String emailJsonEscaped, String firstName, String firstNameJsonEscaped, String lastName, String lastNameJsonEscaped,
            String password, String passwordJsonEscaped, String loggedInGame) {
        this.email = email;
        this.emailJsonEscaped = emailJsonEscaped == null ? email : emailJsonEscaped;
        this.firstName = firstName;
        this.firstNameJsonEscaped = firstNameJsonEscaped == null ? firstName : firstNameJsonEscaped;;
        this.lastName = lastName;
        this.lastNameJsonEscaped = lastNameJsonEscaped == null ? lastName : lastNameJsonEscaped;
        this.password = password;
        this.passwordJsonEscaped = passwordJsonEscaped == null ? password : passwordJsonEscaped;
        this.loggedInGame = loggedInGame;
    }

    public String getEmail() {
        return email;
    }

    public String getEmailJsonEscaped() {
        return emailJsonEscaped;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getFirstNameJsonEscaped() {
        return firstNameJsonEscaped;
    }

    public String getLastName() {
        return lastName;
    }

    public String getLastNameJsonEscaped() {
        return lastNameJsonEscaped;
    }

    public String getPassword() {
        return password;
    }

    public String getPasswordJsonEscaped() {
        return passwordJsonEscaped;
    }
    
    public String getLoggedInGame() {
        return loggedInGame;
    }


    @Override
    public RuntimeException buildException(String message) {
        return new UserException(message);
    }

    @Override
    public String toString() {
        return reflectionToString(this, MULTI_LINE_STYLE);
    }

    @Override
    public boolean equals(Object other) {
        return reflectionEquals(this, other);
    }
}