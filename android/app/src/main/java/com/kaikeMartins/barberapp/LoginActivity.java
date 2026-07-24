package com.kaikeMartins.barberapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.kaikeMartins.barberapp.api.AuthApi;
import com.kaikeMartins.barberapp.api.RetrofitClient;
import com.kaikeMartins.barberapp.models.LoginRequest;
import com.kaikeMartins.barberapp.models.TokenResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etSenha;
    private Button btnLogin;
    private ProgressBar progressBar;
    private TextView tvCriarConta;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager sessionManager = new SessionManager(this);

        // Se já estiver logado, abre a Home
        if (sessionManager.getToken() != null) {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etSenha = findViewById(R.id.etSenha);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);
        tvCriarConta = findViewById(R.id.tvCriarConta);

        btnLogin.setOnClickListener(v -> fazerLogin());

        tvCriarConta.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this,
                    CadastroClienteActivity.class));
        });
    }

    private void fazerLogin() {

        String email = etEmail.getText().toString().trim();
        String senha = etSenha.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError("Informe seu email");
            etEmail.requestFocus();
            return;
        }

        if (senha.isEmpty()) {
            etSenha.setError("Informe sua senha");
            etSenha.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        AuthApi authApi = RetrofitClient.getInstance().create(AuthApi.class);

        LoginRequest request = new LoginRequest(email, senha);

        authApi.login(request).enqueue(new Callback<TokenResponse>() {

            @Override
            public void onResponse(Call<TokenResponse> call,
                                   Response<TokenResponse> response) {

                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {

                    String token = response.body().getToken();

                    SessionManager sessionManager =
                            new SessionManager(LoginActivity.this);

                    sessionManager.saveToken(token);

                    Toast.makeText(LoginActivity.this,
                            "Login realizado com sucesso!",
                            Toast.LENGTH_SHORT).show();

                    Intent intent =
                            new Intent(LoginActivity.this,
                                    HomeActivity.class);

                    startActivity(intent);

                    finish();

                } else {

                    Toast.makeText(LoginActivity.this,
                            "Email ou senha inválidos.",
                            Toast.LENGTH_SHORT).show();

                }

            }

            @Override
            public void onFailure(Call<TokenResponse> call,
                                  Throwable t) {

                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);

                Toast.makeText(LoginActivity.this,
                        "Erro: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();

            }

        });

    }

}