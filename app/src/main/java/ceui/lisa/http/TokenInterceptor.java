package ceui.lisa.http;

import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import ceui.lisa.activities.Shaft;
import ceui.lisa.fragments.FragmentL;
import ceui.lisa.models.UserModel;
import ceui.lisa.utils.Common;
import ceui.lisa.utils.Local;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Call;

/**
 * 全局自动刷新Token的拦截器
 */
public class TokenInterceptor implements Interceptor {

    private static final String TOKEN_ERROR = "Error occurred at the OAuth process";

    @NotNull
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response = chain.proceed(request);

        if (isTokenExpired(response)) {
            response.close();
            String newToken = getNewToken();
            Request newRequest = chain.request()
                    .newBuilder()
                    .header("Authorization", newToken)
                    .build();
            return chain.proceed(newRequest);
        }
        return response;
    }


    /**
     * 根据Response，判断Token是否失效
     *
     * @param response
     * @return
     */
    private boolean isTokenExpired(Response response) {
        return response.code() == 400 &&
                Common.getResponseBody(response).contains(TOKEN_ERROR);
    }

    /**
     * 同步请求方式，获取最新的Token
     *
     * @return
     */
    private String getNewToken() throws IOException {
        UserModel userModel = Local.getUser();
        Call<UserModel> call = Retro.getAccountApi().refreshToken(
                FragmentL.CLIENT_ID,
                FragmentL.CLIENT_SECRET,
                "refresh_token",
                userModel.getResponse().getRefresh_token(),
                userModel.getResponse().getDevice_token(),
                true,
                true);
        UserModel newUser = call.execute().body();
        if (newUser != null) {
            newUser.getResponse().getUser().setPassword(
                    Shaft.sUserModel.getResponse().getUser().getPassword()
            );
            newUser.getResponse().getUser().setIs_login(true);
        }
        Local.saveUser(newUser);
        if (newUser != null && newUser.getResponse() != null) {
            return newUser.getResponse().getAccess_token();
        } else {
            return "ERROR ON GET TOKEN";
        }
    }
}
