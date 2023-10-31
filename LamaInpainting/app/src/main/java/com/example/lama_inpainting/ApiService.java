package com.example.lama_inpainting;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ApiService {

    @Multipart
    @POST("/upload-image")
    Call<ImageResult> editPhoto(@Part MultipartBody.Part image_raw,
                                @Part MultipartBody.Part image_color);

    @GET("/test")
    Call<ResponseBody> getData();


}