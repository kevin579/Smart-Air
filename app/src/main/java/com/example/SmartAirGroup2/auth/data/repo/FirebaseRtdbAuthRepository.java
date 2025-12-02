package com.example.SmartAirGroup2.auth.data.repo;

import com.google.firebase.database.*;
import com.google.android.gms.tasks.Tasks;

public class FirebaseRtdbAuthRepository implements AuthRepository {
    private final DatabaseReference usersRoot =
            FirebaseDatabase.getInstance("https://smart-air-group2-default-rtdb.firebaseio.com/")
                    .getReference("categories")
                    .child("users");

    public static final String CHILD    = "Child";
    public static final String PARENT   = "Parent";
    public static final String PROVIDER = "Provider";

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
    private static String refactory(String s) {
        if(s==null) {
            return "";
        }
        return s.trim().toLowerCase();
    }

//    private DataSnapshot findByEmailInGroup(String group, String emailNorm) throws Exception {
//        DatabaseReference ref = usersRoot.child(group);
//
//        DataSnapshot snap = Tasks.await(ref.orderByChild("email").equalTo(emailNorm).get());
//
//        if (!snap.exists() || snap.getChildrenCount() == 0) {
//            snap = Tasks.await(ref.orderByChild("children/email").equalTo(emailNorm).get());
//            Log.d("RTDB", "[deep] group=" + group + ", exists=" + snap.exists() + ", count=" + snap.getChildrenCount());
//        }
//
//        if (!snap.exists() || snap.getChildrenCount() == 0) return null;
//        return snap.getChildren().iterator().next();
//    }

    @Override
    public ProfileCheck Check_if_User_exist(String role, String username, String email) throws Exception {
        String new_email = refactory(email);
        String new_uname = refactory(username);

        if (new_email.trim().isEmpty()) throw new Exception("invalid-email");
        if (new_uname.trim().isEmpty()) throw new Exception("invalid-username");

        DataSnapshot userSnap = findUserByUsername(role, new_uname);
        if (userSnap == null) {
            return new ProfileCheck(new_email, false);
        }

        String emailInDatabse = refactory(
                userSnap.child("email").getValue(String.class)
        );

        boolean existsAndMatch = new_email.equals(emailInDatabse);
        return new ProfileCheck(new_email, existsAndMatch);
    }

    private DataSnapshot findUserByUsername(String role, String usernameNorm) throws Exception {
        String groupNode = roleToGroupNode(role);
        DatabaseReference ref = usersRoot.child(groupNode).child(usernameNorm);

        DataSnapshot snap = Tasks.await(ref.get());
        if (!snap.exists()) {
            return null;
        }
        return snap;
    }

    @Override
    public boolean CheckPassword(String role, String username, String email, String password) throws Exception {
        String new_email = refactory(email);
        String new_uname = refactory(username);

        if (new_email.trim().isEmpty()) throw new Exception("Invalid Email");
        if (new_uname.trim().isEmpty()) throw new Exception("Invalid Username");
        if (password == null || password.trim().isEmpty()) throw new Exception("Invalid Password");

        DataSnapshot userSnap = findUserByUsername(role, new_uname);
        if (userSnap == null) return false;

        String emailInDb = refactory(
                userSnap.child("email").getValue(String.class)
        );
        String pwdInDatabse = String.valueOf(userSnap.child("password").getValue());

        return new_email.equals(emailInDb) && pwdInDatabse != null && pwdInDatabse.equals(password);
    }

    private static String getString(DataSnapshot node, String path) {
        DataSnapshot s = node;
        for (String p : path.split("/")) s = s.child(p);
        Object v = s.getValue();
        return v == null ? null : String.valueOf(v);
    }
}
