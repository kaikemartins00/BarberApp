package com.kaikeMartins.barberapp.api;

import com.kaikeMartins.barberapp.models.CadastroRequest;
import com.kaikeMartins.barberapp.models.LoginRequest;
import com.kaikeMartins.barberapp.models.TokenResponse;
import com.kaikeMartins.barberapp.models.ValidarCodigoRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface AuthApi {

    @POST("auth/cadastro")
    Call<Void> cadastro(@Body CadastroRequest request);

    @POST("auth/login")
    Call<TokenResponse> login(@Body LoginRequest request);

    @POST("auth/validar-codigo")
    Call<Void> validarCodigo(@Body ValidarCodigoRequest request);

    @POST("auth/reenviar-codigo")
    Call<Void> reenviarCodigo(@Query("email") String email);
}
