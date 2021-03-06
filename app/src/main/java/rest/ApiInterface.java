package rest;

import java.util.List;

import Model.User;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ApiInterface {

    @Multipart
    @POST("user")
    Call<ResponseBody> saveUser(@Part("Name") RequestBody name, @Part MultipartBody.Part image);
    @GET("user")
    Call<List<User>> getUsers();
}

