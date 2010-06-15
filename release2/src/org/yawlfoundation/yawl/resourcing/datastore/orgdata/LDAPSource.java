package org.yawlfoundation.yawl.resourcing.datastore.orgdata;


import org.apache.log4j.Logger;
import org.yawlfoundation.yawl.exceptions.YAuthenticationException;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.resourcing.resource.Role;
import org.yawlfoundation.yawl.resourcing.util.Docket;
import org.yawlfoundation.yawl.util.PasswordEncryptor;

import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.ldap.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Author: Michael Adams
 * Creation Date: 5/03/2010
 * Last Date: 15/06/2010
 */
public class LDAPSource extends DataSource {

    private Properties _props = null;
    private Hashtable<String, String> _attributeMap = null;
    private Hashtable<String, Object> _environment = null;
    private Hashtable<String, String> _user2nameMap = null;
    private HashMap<String, Role> _roles = null;
    private Logger _log;

    public LDAPSource() {
        _log = Logger.getLogger(this.getClass());
        loadProperties();
        initMaps();
    }

    private void loadProperties() {
        try {
            _props = new Properties();
            String path = Docket.getPropertiesDir();
            _props.load(new FileInputStream(path + "LDAPSource.properties"));
        }
        catch (Exception e) {
            _log.error("Exception thrown when loading LDAP properties.", e);
            _props = null;           // this will cause a controlled service disablement
        }
    }

    private void initMaps() {
        _roles = new HashMap<String, Role>();
        if (getProperty("delegateauthentication").equalsIgnoreCase("true")) {
            _user2nameMap = new Hashtable<String, String>();
        }
    }

    private String getProperty(String key) {
        return _props.getProperty(key);
    }

    /**
     * Gets the set of attributes we are interested in retrieving for each user
     * from the LDAP server. The map uses a generic key for each attribute and maps it
     * to the actual corresponding attribute name as read from the properties file.
     * @return a populated map of attribute names
     */
    private Hashtable<String, String> getAttributeMap() {
        if (_attributeMap == null) {
            _attributeMap = new Hashtable<String, String>();
            if (_props != null) {

                // these are mandatory
                _attributeMap.put("userid", getProperty("userid"));
                _attributeMap.put("firstname", getProperty("firstname"));
                _attributeMap.put("lastname", getProperty("lastname"));

                // these are optional
                String password = getProperty("password");
                if (isNotNullOrEmpty(password)) {
                   _attributeMap.put("password", password);
                }
                String isAdmin = getProperty("administrator");
                if (isNotNullOrEmpty(isAdmin)) {
                   _attributeMap.put("isAdmin", isAdmin);
                }
                String roles = getProperty("roles");
                if (isNotNullOrEmpty(roles)) {
                   _attributeMap.put("roles", roles); 
                }
            }
        }
        return _attributeMap;
    }


    /**
     * Gets the list of all entries from the LDAP server corresponding to the binding
     * read from the properties file. The list is retrieved in a single call to the
     * server.
     * @return a (String) List of entries
     * @throws NamingException if something goes wrong when reading from the server
     */
    private List<String> getNameList() throws NamingException {
        List<String> nameList = new ArrayList<String>();
        Context ctx = new InitialContext(getEnvironment());
        NamingEnumeration list = ctx.list(getProperty("binding"));

        while (list.hasMore()) {
            NameClassPair nc = (NameClassPair) list.next();
            nameList.add(nc.getName());
        }
        ctx.close();

        return nameList;
    }


    /**
     * Gets the list of all entries from the LDAP server corresponding to the binding
     * read from the properties file. The list is retrieved in multiple calls to the
     * server, each call retrieving maxSize entries (so as not to exceed the max size
     * limit configured on the server).
     * @param maxSize the max size limit configured
     * @return a (String) List of entries
     * @throws NamingException if something goes wrong when reading from the server
     * @throws IOException if there's a problem creating the PagedResultsControl
     */
    private List<String> getControlledNameList(int maxSize) throws NamingException, IOException {
        List<String> nameList = new ArrayList<String>();
        byte[] cookie = null;
        LdapContext ctx = new InitialLdapContext(getEnvironment(), null);
        ctx.setRequestControls(new Control[]{
                new PagedResultsControl(maxSize, Control.CRITICAL) });

        do {
            NamingEnumeration controlledList = ctx.list(getProperty("binding"));
            while (controlledList != null && controlledList.hasMore()) {
                NameClassPair nc = (NameClassPair) controlledList.next();
                nameList.add(nc.getName());
            }

            Control[] controls = ctx.getResponseControls();
            if (controls != null) {
                for (Control control : controls) {
                    if (control instanceof PagedResultsResponseControl) {
                        PagedResultsResponseControl resp = (PagedResultsResponseControl) control;
                        cookie = resp.getCookie();
                    }
                }
            }
            ctx.setRequestControls(new Control[]{
                    new PagedResultsControl(maxSize, cookie, Control.CRITICAL) });

        } while (cookie != null);

        ctx.close();

        return nameList;
    }


    private String[] getAttributeIDNames() {
        return getAttributeMap().values().toArray(new String[0]);
    }


    /**
     * Gets the environment attribute values for server connections. The value for
     * each attribute is read from the properties file.
     * @return a populated map of environment values.
     */
    private Hashtable<String, Object> getEnvironment() {
        if (_environment == null) {
            _environment = new Hashtable<String, Object>();
            if (_props != null) {
                String url = String.format("ldap://%s:%s", getProperty("host"), getProperty("port"));
                _environment.put(Context.PROVIDER_URL, url);
                _environment.put(Context.INITIAL_CONTEXT_FACTORY, getProperty("contextfactory"));
                _environment.put(Context.SECURITY_AUTHENTICATION, getProperty("authentication"));
                _environment.put(Context.SECURITY_PRINCIPAL, getProperty("adminusername"));
                _environment.put(Context.SECURITY_CREDENTIALS, getProperty("adminpassword"));
            }
        }
        return _environment;
    }


    /**
     * Loads org data from LDAP server and uses it to create the corresponding
     * participants.
     * @return a map of ids and Participants
     * @throws NamingException if something goes wrong when reading from the server
     * @throws IOException if there's a problem creating the PagedResultsControl
     */
    private HashMap<String, Participant> loadParticipants() throws NamingException, IOException {
        HashMap<String, Participant> map = new HashMap<String, Participant>();
        String[] attrIDs = getAttributeIDNames();
        DirContext ctx = new InitialDirContext(getEnvironment());

        // if a max size limit is set, use it to read entries
        int maxSize = getMaxSizeLimit();
        List<String> nameList = (maxSize > 0) ? getControlledNameList(maxSize) : getNameList();

        String binding = getProperty("binding");
        for (String name : nameList) {
            Attributes attributes = ctx.getAttributes(name + "," + binding, attrIDs);
            Participant p = createParticipant(name, attributes);
            if (p != null) {
                map.put(p.getID(), p);
            }
            else {
                _log.error("unable to create participant from LDAP entry: " + name);
            }
        }
        ctx.close();
        return map;
    }


    /**
     * Creates a Participant from an entry name and its attributes
     * @param name the LDAP entry name
     * @param attributes the entries attributes
     * @return an instantiated Participant
     * @throws NamingException if something goes wrong when reading role info from the server
     */
    private Participant createParticipant(String name, Attributes attributes) throws NamingException {
        Participant p = null;
        String lastname = getStringValue(attributes, "lastname");
        String firstname = getStringValue(attributes, "firstname");
        String userid = getStringValue(attributes, "userid");
        if (allNotNullOrEmpty(lastname, firstname, userid)) {
            p = new Participant(lastname, firstname, userid);
            p.setID("U_" + userid);

            // if authentication is done via LDAP, keep the LDAP name - userid mapping
            if (_user2nameMap != null) {
                _user2nameMap.put(userid, name);
            }
            else {
                p.setPassword(loadUserPassword(attributes));
            }

            // set the roles for the particpant - may be enum or csv list
            if (hasEnumeratedRoles()) {
                setRoles(p, attributes);
            }
            else {
                setRoles(p, getStringValue(attributes, "roles"));
            }
        }
        return p;
    }


    /**
     * Sets the Roles, provided as a set of attributes, for a participant
     * @param p the Participant to set the Roles for
     * @param attributes the names of the roles the Participant is to be given
     * @throws NamingException if something goes wrong when reading role info from the server
     */
    private void setRoles(Participant p, Attributes attributes) throws NamingException {
        if (attributes != null) {
            Attribute roles = attributes.get(getProperty("roles"));
            if (roles != null) {
                NamingEnumeration e = roles.getAll();
                while (e.hasMoreElements()) {
                    addToRole(p, String.valueOf(e.next()));
                }
            }
        }
    }


    /**
     * Sets the Roles, provided as a comma separated value string, for a participant
     * @param p the Participant to set the Roles for
     * @param rolesCSV the names of the roles the Participant is to be given
     */
    private void setRoles(Participant p, String rolesCSV) {
        if (isNotNullOrEmpty(rolesCSV)) {
            String[] roleArray = rolesCSV.split("\\s*,\\s*");
            for (String roleName : roleArray) {
                addToRole(p, roleName);
            }
        }
    }


    /**
     * Adds a Participant to a Role, and vice versa
     * @param p the Participant to add to the Role
     * @param roleName the name of the Role to give to the Participant
     */
    private void addToRole(Participant p, String roleName) {
        Role r = _roles.get(roleName);
        if (r == null) {
            r = new Role(roleName);
            r.setID(roleName);
            _roles.put(roleName, r);
        }
        r.addResource(p);
        p.addRole(r);
    }


    /**
     * Loads the LDAP server-stored password for a Participant, if specified to do so
     * via the properties file.
     * @param attributes the LDAP attributes for a user entry
     * @return an encypted password, or null if configured not to load password into
     * each Participant object.
     */
    private String loadUserPassword(Attributes attributes) {
        String password = null;
        if (getAttributeMap().get("password") != null) {
            try {
                byte[] pwBytes = getByteValue(attributes, "password");
                password = PasswordEncryptor.encrypt(new String(pwBytes));
            }
            catch (Exception e) {
                // do nothing - null will be returned
            }
        }
        return password;
    }


    private String getStringValue(Attributes attributes, String attributeName)
            throws NamingException {
        Attribute attr = getAttribute(attributes, attributeName);
        return (attr != null) ? (String) attr.get() : null;
    }

    private byte[] getByteValue(Attributes attributes, String attributeName)
            throws NamingException {
        Attribute attr = getAttribute(attributes, attributeName);
        return (attr != null) ? (byte[]) attr.get() : null;
    }

    private Attribute getAttribute(Attributes attributes, String attributeName)
            throws NamingException {
        String attrID = getAttributeMap().get(attributeName);
        return attributes.get(attrID);       
    }

    private boolean isNotNullOrEmpty(String s) {
        return (s != null) && (s.length() > 0) ;
    }

    private boolean allNotNullOrEmpty(String... values) {
        for (String value : values) {
            if (! isNotNullOrEmpty(value)) return false;
        }
        return true;
    }

    private boolean hasEnumeratedRoles() {
        String roleFormat = getProperty("roleformat");
        return (roleFormat != null) && roleFormat.equalsIgnoreCase("enumeration");
    }

    private int getMaxSizeLimit() {
        String limit = getProperty("maxSizeLimit");
        if (limit != null) {
            try {
                return new Integer(limit.trim());
            }
            catch (NumberFormatException nfe) {
                _log.warn("Ignoring invalid max size limit in LDAP properties: " + limit);
            }
        }
        return 0;
    }

    
    // BASE CLASS IMPLEMENTATIONS //

    public ResourceDataSet loadResources() {
        initMaps();                                   // (re)initialise data structures
        ResourceDataSet rds = new ResourceDataSet(this);
        if (_props != null) {
            try {
                rds.setParticipants(loadParticipants(), this);
                if (! _roles.isEmpty()) {
                    rds.setRoles(_roles, this);
                }
            }
            catch (NamingException ne) {
                // thrown by loadParticipants(); nothing to do, as an empty rds will
                // be returned, initialising a controlled service disablement
                _log.error(
                   "Naming Exception thrown when attempting to retrieve org data from LDAP.", ne);
            }
            catch (IOException ioe) {
                // as above
                _log.error(
                   "IO Exception thrown when attempting to retrieve org data from LDAP.", ioe);
            }
        }    
        return rds;
    }

    public void update(Object obj) {

    }

    public void delete(Object obj) {

    }

    public String insert(Object obj) {
        return null;
    }

    public void importObj(Object obj) {

    }

    public int execUpdate(String query) {
        return -1;
    }

    public boolean authenticate(String userid, String password) throws
            YAuthenticationException {

        if (_user2nameMap == null) {
            throw new YAuthenticationException(
                    "Cannot authenticate user: LDAP Authentication disabled");
        }
        if (! _user2nameMap.containsKey(userid)) {
            throw new YAuthenticationException("Unknown userid");
        }

        Hashtable<String,Object> env = getEnvironment() ;
        String userBinding = _user2nameMap.get(userid) + "," + getProperty("binding");
        Object prevID = env.put(Context.SECURITY_PRINCIPAL, userBinding);
        Object prevPW = env.put(Context.SECURITY_CREDENTIALS, password);
        try {
            new InitialDirContext(env);     // will throw exception if credentials wrong
            return true;
        }
        catch (AuthenticationException ae) {
            return false;                       // bad password
        }
        catch (NamingException ne) {
            throw new YAuthenticationException(
                    "Cannot authenticate user: LDAP Authentication exception.", ne);
        }
        finally {
            env.put(Context.SECURITY_PRINCIPAL, prevID);
            env.put(Context.SECURITY_CREDENTIALS, prevPW);
        }

    }
    
}
