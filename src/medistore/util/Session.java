package medistore.util;

import medistore.model.User;

/** Remembers who is logged in, so bills can record who raised them. */
public final class Session {

    private static User currentUser;

    private Session() {
    }

    public static User getUser() { return currentUser; }
    public static void setUser(User user) { currentUser = user; }
    public static void clear() { currentUser = null; }

    public static String userName() {
        return currentUser == null ? "Guest" : currentUser.getFullName();
    }

    public static int userId() {
        return currentUser == null ? 0 : currentUser.getUserId();
    }

    public static boolean isAdmin() {
        return currentUser != null && "Administrator".equalsIgnoreCase(currentUser.getRole());
    }
}
