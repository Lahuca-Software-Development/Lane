package com.lahuca.lane.data;

/**
 * A permission key that is built up from a name and identifier.
 * Permissions are assigned by using a key, which remains to be set per plugin. This key has the following format "n-i", where:
 * <ul>
 * <li> p := A name of the plugin/module, this is to be alphanumerical. Maximum length of 32. The "name" part.</li>
 * <li>'-' is the character separating the two</li>
 * <li>i := An alphanumerical 6 character long uppercase random ID. The "identifier" part.</li>
 * </ul>
 * Special keys are the one of the controller "#-#" and for everyone "*-*".
 * For permission keys that allow anyone with the same "name" part, the key is solely "n".
 * @param name the name
 * @param identifier the identifier
 */
public record PermissionKey(String name, String identifier) {

    public static final PermissionKey CONTROLLER = new PermissionKey("#", "#");
    public static final PermissionKey EVERYONE = new PermissionKey("*", "*");

    /**
     * Creates a permission key record from a permission key.
     * Beware that the input string needs to be correctly formatted for it to be interpreted correctly.
     * @param permissionKey the permission key to use.
     * @return the record.
     */
    public static PermissionKey fromString(String permissionKey) {
        if(permissionKey.equals("#-#")) return CONTROLLER;
        if(permissionKey.equals("*-*")) return EVERYONE;
        if(permissionKey.contains("-")) {
            String[] parts = permissionKey.split("-");
            return new PermissionKey(parts[0], parts[1]);
        }
        return new PermissionKey(permissionKey);
    }

    public PermissionKey(String name) {
        this(name, null);
    }

    @Override
    public String toString() {
        if(identifier != null) return name + "-" + identifier;
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        PermissionKey that = (PermissionKey) o;
        return this.toString().equals(that.toString());
    }

    /**
     * Returns whether this permission key object is properly formatted.
     * @return true when it is correctly formatted.
     */
    public boolean isFormattedCorrectly() {
        if(name == null) return false;
        if(name.isEmpty() || name.length() > 32) return false;
        if((identifier != null && name.equals("*") && identifier.equals("*")) || (identifier != null &&name.equals("#") && identifier.equals("#"))) return true;
        if(!name.matches("[a-zA-Z0-9]+")) return false;
        if(identifier == null) return true;
        if(identifier.length() != 6) return false;
        if(!identifier.matches("[a-zA-Z0-9]+")) return false;
        return true;
    }

    /**
     * Returns whether this permission key represents the controller permission key.
     * @return true if it is
     */
    public boolean isController() {
        return equals(CONTROLLER);
    }

    /**
     * Returns whether this permission key represents the everyone permission key.
     * @return true if it is
     */
    public boolean isEveryone() {
        return equals(EVERYONE);
    }

    /**
     * Returns whether this permission key represents a name only key.
     * @return true if it is
     */
    public boolean isNameKey() {
        return isFormattedCorrectly() && identifier == null;
    }

    /**
     * Returns whether this permission key represents an individual key: it has a name and identifier part, and it is not the controller or everyone key.
     * @return true if it is
     */
    public boolean isIndividual() {
        return isFormattedCorrectly() && identifier != null && !isController() && !isEveryone();
    }

    /**
     * Checks whether the given has access to use the key provided by this object.
     * @param key the key to check
     * @return true if the provided key has access
     */
    public boolean checkAccess(PermissionKey key) {
        if(!isFormattedCorrectly() || !key.isFormattedCorrectly()) return false;
        if(isEveryone()) return true;
        if(isController() && key.isController()) return true;
        if(isNameKey() && name.equals(key.name)) return true;
        return toString().equals(key.toString());
    }

}
