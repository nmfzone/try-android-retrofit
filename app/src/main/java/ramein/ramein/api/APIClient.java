package ramein.ramein.api;

import ramein.ramein.api.objects.AccessToken;
import ramein.ramein.api.objects.User;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface APIClient
{
    @FormUrlEncoded
    @POST("/login")
    Call<AccessToken> getNewAccessToken(
            @Field("username") String username,
            @Field("password") String password);

    @POST("/refresh-token")
    Call<AccessToken> getRefreshAccessToken();

    @GET("/api/v1/user")
    Call<User> getUser();
}
