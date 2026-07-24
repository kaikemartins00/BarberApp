package com.kaikeMartins.barberapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.kaikeMartins.barberapp.api.AuthApi;
import com.kaikeMartins.barberapp.api.RetrofitClient;
import com.kaikeMartins.barberapp.models.CadastroRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CadastroClienteActivity extends AppCompatActivity {

    private EditText etNome, etEmail, etTelefone, etSenha, etConfirmarSenha;
    private Button btnCadastrar;
    private ProgressBar progressBar;
    private TextView tvVoltarLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro_cliente);

        etNome = findViewById(R.id.etNome);
        etEmail = findViewById(R.id.etEmail);
        etTelefone = findViewById(R.id.etTelefone);
        etTelefone.addTextChangedListener(new PhoneMask(etTelefone));
        etSenha = findViewById(R.id.etSenha);
        etConfirmarSenha = findViewById(R.id.etConfirmarSenha);
        btnCadastrar = findViewById(R.id.btnCadastrar);
        progressBar = findViewById(R.id.progressBar);
        tvVoltarLogin = findViewById(R.id.tvVoltarLogin);

        btnCadastrar.setOnClickListener(v -> cadastrar());

        tvVoltarLogin.setOnClickListener(v -> {
            startActivity(new Intent(CadastroClienteActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void cadastrar() {
        String nome = etNome.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String telefone = etTelefone.getText().toString().trim();
        String senha = etSenha.getText().toString().trim();
        String confirmarSenha = etConfirmarSenha.getText().toString().trim();

        if (nome.isEmpty() || email.isEmpty() || senha.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos obrigatórios", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!senha.equals(confirmarSenha)) {
            Toast.makeText(this, "As senhas não coincidem", Toast.LENGTH_SHORT).show();
            return;
        }

        if (senha.length() < 6) {
            Toast.makeText(this, "A senha deve ter pelo menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(android.view.View.VISIBLE);
        btnCadastrar.setEnabled(false);

        AuthApi authApi = RetrofitClient.getInstance().create(AuthApi.class);
        CadastroRequest request = new CadastroRequest(nome, email, senha, telefone);

        authApi.cadastro(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                progressBar.setVisibility(android.view.View.GONE);
                btnCadastrar.setEnabled(true);

                if (response.isSuccessful()) {
                    Toast.makeText(CadastroClienteActivity.this, "Conta criada! Verifique seu email.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(CadastroClienteActivity.this, VerificarCodigoActivity.class);
                    intent.putExtra(VerificarCodigoActivity.EXTRA_EMAIL, email);
                    startActivity(intent);
                    finish();
                } else if (response.code() == 400) {
                    Toast.makeText(CadastroClienteActivity.this, "Email já cadastrado", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(CadastroClienteActivity.this, "Erro ao cadastrar (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                progressBar.setVisibility(android.view.View.GONE);
                btnCadastrar.setEnabled(true);
                Toast.makeText(CadastroClienteActivity.this, "Erro de conexão: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }


}