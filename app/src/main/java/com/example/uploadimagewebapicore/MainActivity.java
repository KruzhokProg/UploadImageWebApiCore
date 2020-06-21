package com.example.uploadimagewebapicore;

import Model.User;
import adapter.UserAdapter;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_IMAGE_INPUT = 1213;
    private static final int PICKFILE_REQUEST_CODE = 1415;
    private static final int ACTIVITY_CHOOSE_FILE = 1516 ;
    UserAdapter adapter;
    ApiInterface apiService;
    List<User> users;
    RecyclerView rv;
    EditText etName;
    Uri uri;
    File originalFile;
    Bitmap image;
    InputStream stream;
    ArrayList<Uri> fileUris;
    Boolean isMultiple;

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
        Intent intent=new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        String[] mimeTypes = {"image/jpeg", "image/png"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(intent, REQUEST_CODE_IMAGE_INPUT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case REQUEST_CODE_IMAGE_INPUT:
                if(resultCode == Activity.RESULT_OK && data != null){
                    uri = data.getData();
                    String[] filePathColumn = { MediaStore.Images.Media.DATA };
                    Cursor cursor = getContentResolver().query(uri, filePathColumn, null, null, null);
                    try {
                        if( cursor != null &&  cursor.moveToFirst()) {
                            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                            String imgDecodableString = cursor.getString(columnIndex);
                            originalFile = new File(imgDecodableString);
                        }
                    }catch (Exception e){
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }finally {
                        cursor.close();
                    }
                }
                break;
            case PICKFILE_REQUEST_CODE:
                if(resultCode == Activity.RESULT_OK && data != null){
                    uri = data.getData();
                    setOriginalFile(uri);
                    isMultiple = false;
                }
                break;
            case ACTIVITY_CHOOSE_FILE:
                if( data != null && resultCode == Activity.RESULT_OK){
                    fileUris = new ArrayList<>();
                    for(int i = 0; i < data.getClipData().getItemCount(); i++) {
                        uri = data.getClipData().getItemAt(i).getUri();
                        fileUris.add(uri);
                        isMultiple = true;
                    }
                }
        }
    }

    public void setOriginalFile(Uri uri)
    {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                String fileName = cursor.getString(columnIndex);
                String dirPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                String path = dirPath + "/" + fileName;
                originalFile = new File(path);
            }
        }catch (Exception e){
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        finally {
            cursor.close();
        }
    }

    private RequestBody createPartFromString(String descriptionString) {
        return RequestBody.create(MultipartBody.FORM, descriptionString);
    }

    private MultipartBody.Part prepareFilePart(String partName, Uri fileUri) {
        setOriginalFile(fileUri);
        // create RequestBody instance from file
        RequestBody requestFile =
                RequestBody.create(
                        MediaType.parse(getContentResolver().getType(fileUri)),
                        originalFile
                );

        // MultipartBody.Part is used to send also the actual file name
        return MultipartBody.Part.createFormData(partName, originalFile.getName(), requestFile);
    }

    public void btnSend_Click(View view) {

        if(isMultiple){
            List<MultipartBody.Part> parts = new ArrayList<>();
            for (int i=0; i<fileUris.size(); i++){
                parts.add(prepareFilePart("Images", fileUris.get(i)));
            }

            Call<ResponseBody> call = apiService.uploadMultipleFilesDynamic(createPartFromString(etName.getText().toString()), parts);
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    Toast.makeText(MainActivity.this, "Данные успешно сохранены! code: " + response.code(), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Toast.makeText(MainActivity.this, "fail: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
        else {
            RequestBody namePart = RequestBody.create(MultipartBody.FORM, etName.getText().toString());

            //RequestBody filePart = RequestBody.create( okhttp3.MediaType.parse("image/*"), originalFile);
            RequestBody filePart = null;
            try {
                String uri_type = getApplicationContext().getContentResolver().getType(uri);
                MediaType type = MediaType.parse(uri_type);
                filePart = RequestBody.create(type, originalFile);
            } catch (Exception e) {
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
                    Toast.makeText(MainActivity.this, "fail: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public void img_btn_folder_Click(View view) {
        Intent target = new Intent(Intent.ACTION_GET_CONTENT);
        target.setType("*/*");
        //target.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(target, PICKFILE_REQUEST_CODE);
    }

    public void img_btn_multiple_OnClick(View view) {
        Intent chooseFile;
        Intent intent;
        chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
        chooseFile.setType("*/*");
        chooseFile.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent = Intent.createChooser(chooseFile, "Choose a file");
        startActivityForResult(intent, ACTIVITY_CHOOSE_FILE);
    }
}
