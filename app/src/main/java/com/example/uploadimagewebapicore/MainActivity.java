package com.example.uploadimagewebapicore;

import Model.User;
import adapter.UserAdapter;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import in.mayanknagwanshi.imagepicker.ImageSelectActivity;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import rest.ApiClient;
import rest.ApiInterface;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.commons.io.FileUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_IMAGE_INPUT = 1213;
    private static final int PICKFILE_REQUEST_CODE = 1415;
    UserAdapter adapter;
    ApiInterface apiService;
    List<User> users;
    RecyclerView rv;
    EditText etName;
    Uri uri;
    File originalFile;
    Bitmap image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etName = findViewById(R.id.etName);
        rv = findViewById(R.id.rv);
        apiService = ApiClient.getClient().create(ApiInterface.class);

        Call<List<User>> call = apiService.getUsers();
        call.enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                users = response.body();
                adapter = new UserAdapter(getApplicationContext(), users);
                rv.setAdapter(adapter);
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                Toast.makeText(MainActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void btnUploadImage(View view) {
        Intent intent = new Intent(this, ImageSelectActivity.class);
        intent.putExtra(ImageSelectActivity.FLAG_COMPRESS, false);
        intent.putExtra(ImageSelectActivity.FLAG_CAMERA, true);
        intent.putExtra(ImageSelectActivity.FLAG_GALLERY, true);
        startActivityForResult(intent, 1213);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case REQUEST_CODE_IMAGE_INPUT:
                if(resultCode == Activity.RESULT_OK && data != null){
                    String filePath = data.getStringExtra(ImageSelectActivity.RESULT_FILE_PATH);
                    uri = Uri.parse(filePath);
                    originalFile = new File(filePath);
                    //image = BitmapFactory.decodeFile(filePath);
//                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
//                    image.compress(Bitmap.CompressFormat.JPEG, 100, stream);
//                    imageBytes = stream.toByteArray();
                }
                break;
            case PICKFILE_REQUEST_CODE:
                if(resultCode == Activity.RESULT_OK && data != null){
                    String filePath = data.getData().getPath();
                    Toast.makeText(this, filePath, Toast.LENGTH_SHORT).show();
                    //uri = Uri.parse(filePath);
                    uri = data.getData();
                    originalFile = new File(filePath);
                    // with changes 2
                }
        }
    }

    public void btnSend_Click(View view) {
        RequestBody namePart = RequestBody.create(MultipartBody.FORM, etName.getText().toString());

        //RequestBody filePart = RequestBody.create( okhttp3.MediaType.parse("image/*"), originalFile);
        RequestBody filePart=null;
        try {
            String uri_type = getApplicationContext().getContentResolver().getType(uri);
            MediaType type = MediaType.parse(uri_type);
            filePart = RequestBody.create(type, originalFile);
        }catch (Exception e){
            Log.d("ERROR", e.getMessage());
        }

        MultipartBody.Part file = MultipartBody.Part.createFormData("Image", originalFile.getName(), filePart);

        Call<ResponseBody> call = apiService.saveUser(namePart, file);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Toast.makeText(MainActivity.this, "Данные успешно сохранены! code: " + response.code(), Toast.LENGTH_SHORT).show();
                adapter.notifyItemInserted(users.size());
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(MainActivity.this, "fail: "+t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void img_btn_folder_Click(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, PICKFILE_REQUEST_CODE);
    }
}
