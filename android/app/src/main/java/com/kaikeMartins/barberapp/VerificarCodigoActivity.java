package com.kaikeMartins.barberapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.kaikeMartins.barberapp.api.AuthApi;
import com.kaikeMartins.barberapp.api.RetrofitClient;
import com.kaikeMartins.barberapp.models.ValidarCodigoRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VerificarCodigoActivity extends AppCompatActivity {

    public static final String EXTRA_EMAIL = "extra_email";

    private EditText etCodigo;
    private Button btnConfirmar;
    private ProgressBar progressBar;
    private TextView tvReenviar;

    private String email;
    private boolean podeReenviar = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verificar_codigo);

        email = getIntent().getStringExtra(EXTRA_EMAIL);

        etCodigo = findViewById(R.id.etCodigo);
        btnConfirmar = findViewById(R.id.btnConfirmar);
        progressBar = findViewById(R.id.progressBar);
        tvReenviar = findViewById(R.id.tvReenviar);

        btnConfirmar.setOnClickListener(v -> confirmarCodigo());
        tvReenviar.setOnClickListener(v -> reenviarCodigo());
    }

    private void confirmarCodigo() {
        String codigo = etCodigo.getText().toString().trim();

        if (codigo.length() != 6) {
            Toast.makeText(this, "Digite o código de 6 dígitos", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(android.view.View.VISIBLE);
        btnConfirmar.setEnabled(false);

        AuthApi authApi = RetrofitClient.getInstance().create(AuthApi.class);
        ValidarCodigoRequest request = new ValidarCodigoRequest(email, codigo);

        authApi.validarCodigo(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                progressBar.setVisibility(android.view.View.GONE);
                btnConfirmar.setEnabled(true);

                if (response.isSuccessful()) {
                    Toast.makeText(VerificarCodigoActivity.this, "Conta verificada com sucesso!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(VerificarCodigoActivity.this, LoginActivity.class));
                    finish();
                } else if (response.code() == 400) {
                    Toast.makeText(VerificarCodigoActivity.this, "Código inválido ou expirado", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(VerificarCodigoActivity.this, "Erro ao validar (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                progressBar.setVisibility(android.view.View.GONE);
                btnConfirmar.setEnabled(true);
                Toast.makeText(VerificarCodigoActivity.this, "Erro de conexão: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void reenviarCodigo() {
        if (!podeReenviar) {
            Toast.makeText(this, "Aguarde antes de reenviar novamente", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthApi authApi = RetrofitClient.getInstance().create(AuthApi.class);

        authApi.reenviarCodigo(email).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(VerificarCodigoActivity.this, "Novo código enviado!", Toast.LENGTH_SHORT).show();
                    iniciarContagemReenvio();
                } else {
                    Toast.makeText(VerificarCodigoActivity.this, "Erro ao reenviar código", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(VerificarCodigoActivity.this, "Erro de conexão: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void iniciarContagemReenvio() {
        podeReenviar = false;
        tvReenviar.setEnabled(false);

        new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvReenviar.setText("Reenviar código (" + (millisUntilFinished / 1000) + "s)");
            }

            @Override
            public void onFinish() {
                podeReenviar = true;
                tvReenviar.setEnabled(true);
                tvReenviar.setText("Reenviar código");
            }
        }.start();
    }
}