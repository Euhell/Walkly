package com.example.samsungproject.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.samsungproject.BuildConfig;
import com.example.samsungproject.activities.MainActivity;
import com.example.samsungproject.databinding.FragmentLoginBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;


public class LoginFragment extends Fragment {

    public interface OnLoginSuccessListener {
        void onLoginSuccess();

    }
    private OnLoginSuccessListener loginSuccessListener;
    FragmentLoginBinding binding;
    String clientId = BuildConfig.WEB_CLIENT_ID;
    private SharedPreferences prefs;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        boolean isGuest = prefs.getBoolean("is_guest", false);
        if (user != null && !isGuest && loginSuccessListener != null) {
            loginSuccessListener.onLoginSuccess();
            return;
        }
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.setBottomNavigationVisibility(View.GONE);
        }
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(clientId)
                .requestEmail()
                .build();
        GoogleSignInClient client = GoogleSignIn.getClient(requireContext(), gso);
        binding.signInButton.setOnClickListener(v -> {
            Intent signInIntent = client.getSignInIntent();
            startActivityForResult(signInIntent, 1001);
        });
        binding.signInGuest.setOnClickListener(v -> {
            prefs.edit().putBoolean("is_guest", true).apply();
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.setBottomNavigationVisibility(View.VISIBLE);
            }
            if (loginSuccessListener != null) {
                loginSuccessListener.onLoginSuccess();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.w("LoginFragment", "Google sign in failed", e);
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null) {
                            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                            String distanceKey = "total_distance_" + userId;
                            long localDistance = prefs.getLong(distanceKey, 0);
                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("displayName", user.getDisplayName());
                            userData.put("score", localDistance);
                            userData.put("photoUrl", String.valueOf(user.getPhotoUrl()));
                            db.collection("users").document(user.getUid()).set(userData, SetOptions.merge());
                            prefs.edit().putBoolean("is_guest", false).apply();
                            Toast.makeText(requireContext(), "Вы вошли в аккаунт: " + userData.get("displayName"), Toast.LENGTH_SHORT).show();
                        }
                        if (loginSuccessListener != null) {
                            loginSuccessListener.onLoginSuccess();
                        }
                    } else {
                        Log.w("LoginFragment", "signInWithCredential:failure", task.getException());
                    }
                });
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnLoginSuccessListener) {
            loginSuccessListener = (OnLoginSuccessListener) context;
        } else {
            throw new ClassCastException(context + " must implement OnLoginSuccessListener");
        }
    }
}
