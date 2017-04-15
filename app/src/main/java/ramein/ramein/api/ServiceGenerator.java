package ramein.ramein.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.X509TrustManager;

import okhttp3.CertificatePinner;
import okhttp3.CipherSuite;
import okhttp3.ConnectionSpec;
import okhttp3.TlsVersion;
import ramein.ramein.BuildConfig;
import ramein.ramein.api.objects.AccessToken;
import okhttp3.Authenticator;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ServiceGenerator
{

    public static final String API_BASE_URL = "https://api.ramein.id";

    private static OkHttpClient.Builder httpClient;

    private static Retrofit.Builder builder;

    private static Context mContext;

    public static <S> S createService(Class<S> serviceClass)
    {
        httpClient = new OkHttpClient.Builder();
        builder = new Retrofit.Builder()
                .baseUrl(API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create());

        OkHttpClient client = httpClient.build();
        Retrofit retrofit = builder.client(client).build();

        return retrofit.create(serviceClass);
    }

    public static <S> S createService(Class<S> serviceClass, AccessToken accessToken, Context c) throws GeneralSecurityException
    {
        httpClient = new OkHttpClient.Builder();

        builder = new Retrofit.Builder()
                .baseUrl(API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create());

//        ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
//                .tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_1)
//                .build();
//
//        if (Build.VERSION.SDK_INT < 21) {
//            X509TrustManager trustManager = TLSPatch.systemDefaultTrustManager();
//            httpClient.sslSocketFactory(new TLSPatch.TLSSocketFactory(), trustManager);
//        }
//
//        httpClient.connectionSpecs(Collections.singletonList(spec))
//            .readTimeout(60, TimeUnit.SECONDS);

        if (accessToken != null) {
            mContext = c;

            final AccessToken token = accessToken;

            httpClient.addInterceptor(new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request original = chain.request();

                    Request.Builder requestBuilder = original.newBuilder()
                            .header("Accept", "application/json")
                            .header("Content-type", "application/json")
                            .header("Authorization",
                                    token.getTokenType() + " " + token.getAccessToken())
                            .method(original.method(), original.body());

                    Request request = requestBuilder.build();

                    return chain.proceed(request);
                }
            });

            httpClient.authenticator(new Authenticator() {
                @Override
                public Request authenticate(Route route, Response response) throws IOException {
                    if (responseCount(response) >= 2) {
                        // If both the original call and the call with refreshed token failed,
                        // it will probably keep failing, so don't try again.
                        return null;
                    }

                    // We need a new client, since we don't want to make another call using our client with access token
                    APIClient tokenClient = createService(APIClient.class);
                    Call<AccessToken> call = tokenClient.getRefreshAccessToken();

                    try {
                        retrofit2.Response<AccessToken> tokenResponse = call.execute();

                        if (tokenResponse.code() == 200) {
                            AccessToken newToken = tokenResponse.body();
                            SharedPreferences prefs = mContext.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);
                            prefs.edit().putBoolean("oauth.loggedin", true).apply();
                            prefs.edit().putString("oauth.accesstoken", newToken.getAccessToken()).apply();
                            prefs.edit().putString("oauth.refreshtoken", newToken.getRefreshToken()).apply();
                            prefs.edit().putString("oauth.tokentype", newToken.getTokenType()).apply();

                            return response.request().newBuilder()
                                    .header("Authorization", newToken.getTokenType() + " " + newToken.getAccessToken())
                                    .build();
                        } else {
                            return null;
                        }
                    } catch (IOException e) {
                        return null;
                    }
                }
            });
        }

        OkHttpClient client = httpClient.build();

        Retrofit retrofit = builder.client(client).build();

        return retrofit.create(serviceClass);
    }

    private static int responseCount(Response response)
    {
        int result = 1;

        while ((response = response.priorResponse()) != null) result++;

        return result;
    }
}