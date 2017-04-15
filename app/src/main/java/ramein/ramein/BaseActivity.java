package ramein.ramein;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.security.GeneralSecurityException;

import ramein.ramein.api.APIClient;
import ramein.ramein.api.ServiceGenerator;
import ramein.ramein.api.objects.AccessToken;

public class BaseActivity extends AppCompatActivity
{
    protected AccessToken accessToken;

    public AccessToken getAccessToken()
    {
        final SharedPreferences prefs = getSharedPreferences(
                BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);

        accessToken = new AccessToken();
        accessToken.setAccessToken(prefs.getString("oauth.accesstoken", ""));
        accessToken.setTokenType(prefs.getString("oauth.tokentype", ""));
        accessToken.setRefreshToken(prefs.getString("oauth.refreshtoken", ""));

        return accessToken;
    }

    protected <S> S createService(Class<S> serviceClass)
    {
        try {
            return ServiceGenerator.createService(serviceClass, getAccessToken(), getApplicationContext());
        } catch(GeneralSecurityException e) {
            Log.d("General Security Exception", e.toString());
        }

        return ServiceGenerator.createService(serviceClass);
    }
}
