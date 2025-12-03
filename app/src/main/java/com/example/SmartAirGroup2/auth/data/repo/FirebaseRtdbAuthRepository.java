package com.example.SmartAirGroup2.auth.data.repo;

import com.google.firebase.database.*;
import com.google.android.gms.tasks.Tasks;


/**
 * Implements the {@link AuthRepository} interface using the Firebase Realtime Database (RTDB).
 *
 * This class handles all synchronization operations (user lookups, existence checks,
 * and password verification) against the RTDB under the "categories/users" path.
 * Note: Since Firebase RTDB operations are asynchronous, synchronous blocking is
 * used via {@code Tasks.await()} to fulfill the synchronous contract required by
 * the {@code LoginPresenter} thread execution model.
 */
public class FirebaseRtdbAuthRepository implements AuthRepository {

    // Base reference to the 'users' node in the Firebase Realtime Database.
    private final DatabaseReference usersRoot =
            FirebaseDatabase.getInstance("https://smart-air-group2-default-rtdb.firebaseio.com/")
                    .getReference("categories")
                    .child("users");

    // Constants for standard user roles, used for mapping to database group nodes.
    public static final String CHILD    = "Child";
    public static final String PARENT   = "Parent";
    public static final String PROVIDER = "Provider";

    /**
     * Maps the human-readable user role string to the corresponding Firebase database group node name.
     *
     * @param role The user role (e.g., "Child", "Parent", "Provider").
     * @return The corresponding Firebase node name (e.g., "children", "parents", "provider").
     * @throws Exception If the provided role string is not recognized.
     */
    private String roleToGroupNode(String role) throws Exception{
        if (CHILD.equals(role)) {
            return "children";
        } else if (PARENT.equals(role)) {
            return "parents";
        } else if (PROVIDER.equals(role)) {
            return "provider";
        }
        throw new Exception("Unknown role: " + role);
    }

    /**
     * Cleans and normalizes a string for database lookup and comparison.
     *
     * Converts the string to lowercase and removes leading/trailing whitespace.
     * Returns an empty string if the input is null.
     *
     * @param s The input string (e.g., email or username).
     * @return The normalized string.
     */
    private static String refactory(String s) {
        if(s==null) {
            return "";
        }
        return s.trim().toLowerCase();
    }


    /**
     * Checks if a user profile exists in the database and if the provided email matches
     * the stored email for that user/role.
     *
     * @param role The role of the user.
     * @param username The username to check for existence.
     * @param email The email to check for match against the existing profile.
     * @return A {@link ProfileCheck} object containing the normalized email and the match result.
     * @throws Exception If email or username inputs are empty or invalid.
     */
    @Override
    public ProfileCheck Check_if_User_exist(String role, String username, String email) throws Exception {
        String new_email = refactory(email);
        String new_uname = refactory(username);

        if (new_email.trim().isEmpty()) throw new Exception("invalid-email");
        if (new_uname.trim().isEmpty()) throw new Exception("invalid-username");

        DataSnapshot userSnap = findUserByUsername(role, new_uname);
        if (userSnap == null) {
            // User does not exist, but return the email for the ProfileCheck wrapper.
            return new ProfileCheck(new_email, false);
        }

        String emailInDatabse = refactory(
                userSnap.child("email").getValue(String.class)
        );

        boolean existsAndMatch = new_email.equals(emailInDatabse);
        return new ProfileCheck(new_email, existsAndMatch);
    }

    /**
     * Synchronously attempts to retrieve a user's data snapshot from Firebase based on
     * role and normalized username.
     *
     * Uses {@code Tasks.await()} to block the calling thread until the Firebase operation completes.
     *
     * @param role The role of the user.
     * @param usernameNorm The normalized username (lowercase, trimmed).
     * @return The DataSnapshot of the user node if it exists, otherwise null.
     * @throws Exception Thrown if {@code roleToGroupNode} fails or if the synchronous task execution fails.
     */
    private DataSnapshot findUserByUsername(String role, String usernameNorm) throws Exception {
        String groupNode = roleToGroupNode(role);
        // Path example: categories/users/children/normalized_username
        DatabaseReference ref = usersRoot.child(groupNode).child(usernameNorm);

        // Synchronously wait for the Firebase GET request to complete
        DataSnapshot snap = Tasks.await(ref.get());
        if (!snap.exists()) {
            return null;
        }
        return snap;
    }

    /**
     * Checks if the provided email and password match the data stored in the database
     * for the given role and username.
     *
     * @param role The role of the user.
     * @param username The username provided by the user.
     * @param email The email provided by the user.
     * @param password The password provided by the user.
     * @return True if the user exists and the email/password pair matches the database records.
     * @throws Exception If input fields are invalid or empty.
     */
    @Override
    public boolean CheckPassword(String role, String username, String email, String password) throws Exception {
        String new_email = refactory(email);
        String new_uname = refactory(username);

        // Input validation
        if (new_email.trim().isEmpty()) throw new Exception("Invalid Email");
        if (new_uname.trim().isEmpty()) throw new Exception("Invalid Username");
        if (password == null || password.trim().isEmpty()) throw new Exception("Invalid Password");

        // Retrieve user data
        DataSnapshot userSnap = findUserByUsername(role, new_uname);
        if (userSnap == null) return false; // User not found

        // Extract stored credentials
        String emailInDb = refactory(
                userSnap.child("email").getValue(String.class)
        );
        // Note: Password retrieval assumes it's stored as plain text (Security Risk, recommend hashing in production)
        String pwdInDatabse = String.valueOf(userSnap.child("password").getValue());

        // Compare inputs against stored, normalized values
        return new_email.equals(emailInDb) && pwdInDatabse != null && pwdInDatabse.equals(password);
    }

    /**
     * Utility method to safely traverse a DataSnapshot using a path string (e.g., "user/name/first").
     *
     * @param node The starting DataSnapshot.
     * @param path The path string to traverse, separated by "/".
     * @return The String value at the end of the path, or null if the path doesn't exist or the value is null.
     */
    private static String getString(DataSnapshot node, String path) {
        DataSnapshot s = node;
        for (String p : path.split("/")) s = s.child(p);
        Object v = s.getValue();
        return v == null ? null : String.valueOf(v);
    }
}
