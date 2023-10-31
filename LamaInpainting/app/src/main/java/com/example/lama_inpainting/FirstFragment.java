package com.example.lama_inpainting;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.Manifest.permission.MANAGE_EXTERNAL_STORAGE;
import static android.Manifest.permission_group.CAMERA;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.lama_inpainting.databinding.FragmentFirstBinding;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FirstFragment extends Fragment implements View.OnTouchListener {

    private FragmentFirstBinding binding;
    ImageView IVPreviewImage;
    EditText ETInputPhotoUrl;
    private Bitmap bitmap;
    private Canvas canvas;
    private Paint paint;
    private ArrayList<Point> touchPoints;

    private File file_path_1, file_path_2;
    String userChoosenTask = "";
    ApiService apiService;
    private static final int REQUEST_CAMERA = 1888;
    private static final int SELECT_FILE = 1889;
    private static final int PERMISSION_REQUEST_CODE = 200;
    String[] perms = {"android.permission.READ_EXTERNAL_STORAGE", "android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.MANAGE_EXTERNAL_STORAGE"};
    int permsRequestCode = 200;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        setHasOptionsMenu(true);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        IVPreviewImage = view.findViewById(R.id.IVPreviewImage);
        ETInputPhotoUrl = view.findViewById(R.id.et_input_url);
        touchPoints = new ArrayList<>();

        if(!checkPermission()){
            requestPermissions(perms, permsRequestCode);
        }

        binding.buttonUrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(ETInputPhotoUrl.getText().length()>0){
                    new DownloadImageFromInternet().execute(ETInputPhotoUrl.getText().toString());
                }
            }
        });

        IVPreviewImage.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int x = (int) event.getX();
                int y = (int) event.getY();

                // Lưu điểm người dùng chạm vào
                touchPoints.add(new Point(x, y));

                // Vẽ một hình tròn đỏ tại vị trí người dùng chạm
                canvas.drawCircle(x, y, 25, paint);
                IVPreviewImage.setImageBitmap(bitmap);
                return true;
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main, menu); // Sử dụng menu của MainActivity
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_select) {
            selectImage(getContext());
            return true;
        }
        if (id == R.id.action_clear) {
            IVPreviewImage.setImageBitmap(null);
            canvas = null;
            bitmap = null;
            return true;
        }
        if (id == R.id.action_download) {
            if(bitmap!=null) {
                try {
                    saveBitmap(bitmap, "edit");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return true;
        }
        if (id == R.id.action_edit) {
            Bitmap newBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            Canvas newCanvas = new Canvas(newBitmap);

            for (Point point : touchPoints) {
                newCanvas.drawCircle(point.x, point.y, 30, paint);
            }

            try {
                saveBitmap(newBitmap, "color");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            apiService = ApiClient.getClient().create(ApiService.class);
            RequestBody requestFile1 = RequestBody.create(MediaType.parse("multipart/form-data"), file_path_1);
            MultipartBody.Part filePart1 = MultipartBody.Part.createFormData("image_raw", "image_raw.png", requestFile1);

            RequestBody requestFile2 = RequestBody.create(MediaType.parse("multipart/form-data"), file_path_2);
            MultipartBody.Part filePart2 = MultipartBody.Part.createFormData("image_color", "image_color.png", requestFile2);

            Call<ImageResult> call = apiService.editPhoto(filePart1, filePart2);

            call.enqueue(new Callback<ImageResult>() {
                public void onResponse(Call<ImageResult> call, Response<ImageResult> response) {
                    if (response.isSuccessful()) {
                        bitmap = null;
                        // Xử lý kết quả nếu request thành công
                        ImageResult base64String = response.body();
                        Log.e("error1: ", base64String.getImg());
                        Toast.makeText(getContext(), "Loading photo..." , Toast.LENGTH_SHORT).show();
                        byte[] decodedBytes = Base64.decode(base64String.getImg(), Base64.DEFAULT);
                        bitmap =  BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                        IVPreviewImage.setImageBitmap(bitmap);
                    } else {
                        Toast.makeText(getContext(), "Error", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ImageResult> call, Throwable t) {
                    Toast.makeText(getContext(), t.toString(), Toast.LENGTH_SHORT).show();
                }
            });

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveBitmap(Bitmap bitmap, String type_file) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName ="image_" +  type_file +"_" +  timeStamp;

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, imageFileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

        Uri imageUri = getContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        try {
            OutputStream out = getContext().getContentResolver().openOutputStream(imageUri);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            String fullPath = Environment.getExternalStorageDirectory().getAbsolutePath() +"/" + Environment.DIRECTORY_PICTURES + "/" + imageFileName +".png";
            if(type_file=="raw"){
                file_path_1 = new File(fullPath);
            } else if(type_file=="color"){
                file_path_2 = new File(fullPath);
            }
            assert out != null;
            out.close();
            MediaScannerConnection.scanFile(getContext(), new String[]{new File(imageUri.getPath()).toString()}, null, null);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void selectImage(Context context) {
        final CharSequence[] items = { "Take Photo", "Choose from Library",
                "Cancel" };
        new AlertDialog.Builder( context)
        .setTitle("Add Photo!")
        .setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                boolean result=checkPermission();
                if (items[item].equals("Take Photo")) {
                    userChoosenTask="Take Photo";
//                    if(result){
                        cameraIntent();
//                    }
                } else if (items[item].equals("Choose from Library")) {
                    userChoosenTask="Choose from Library";
//                    if(result) {
                        galleryIntent();
//                    }
                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        }).show();
    }

    private void cameraIntent()
    {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra("android.intent.extras.CAMERA_FACING", android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    private void galleryIntent()
    {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);//
        startActivityForResult(Intent.createChooser(intent, "Select File"),SELECT_FILE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == SELECT_FILE){
                onSelectFromGalleryResult(data);
            }
            else if (requestCode == REQUEST_CAMERA) {
                try {
                    onCaptureImageResult(data);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void onSelectFromGalleryResult(Intent data) {
        Bitmap bm=null;
        if (data != null) {
            try {
                bm = MediaStore.Images.Media.getBitmap(getActivity().getApplicationContext().getContentResolver(), data.getData());
                saveBitmap(bm, "raw");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        IVPreviewImage.setImageBitmap(bm);
        this.paintPhoto();
    }

    private String getImagePath(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getActivity().getApplicationContext().getContentResolver().query(uri, projection, null, null, null);

        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(column_index);
            cursor.close();
            return path;
        }

        return null;
    }

    private void onCaptureImageResult(Intent data) throws IOException {
        canvas = null;
        bitmap = null;
        Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.PNG, 100, bytes);
        saveBitmap(thumbnail, "raw");

        IVPreviewImage.setImageBitmap(thumbnail);
        this.paintPhoto();
    }
    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), READ_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), CAMERA);
        int result2 = ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), WRITE_EXTERNAL_STORAGE);
        int result3 = ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), MANAGE_EXTERNAL_STORAGE);

        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED && result2 == PackageManager.PERMISSION_GRANTED && result3 == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0) {

                    boolean locationAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean cameraAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (locationAccepted && cameraAccepted)
                    {
                        if(userChoosenTask.equals("Take Photo")){
                            cameraIntent();
                        }
                        else if(userChoosenTask.equals("Choose from Library")){
                            galleryIntent();
                        }
                    }
                    else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (shouldShowRequestPermissionRationale(READ_EXTERNAL_STORAGE)) {
                                showMessageOKCancel("You need to allow access to both the permissions",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                    requestPermissions(perms, permsRequestCode);
                                                }
                                            }
                                        });
                                return;
                            }
                        }
                    }
                }
                break;
        }
    }
    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(getActivity())
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        return false;
    }

    private class DownloadImageFromInternet extends AsyncTask<String, Void, Bitmap> {
        public DownloadImageFromInternet() {
//            this.imageView=imageView;
            Toast.makeText(getActivity().getApplicationContext(), "Please wait, it may take a few minute...",Toast.LENGTH_SHORT).show();
        }
        protected Bitmap doInBackground(String... urls) {
            String imageURL=urls[0];
            Bitmap bimage=null;
            try {
                InputStream in=new java.net.URL(imageURL).openStream();
                bimage= BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error Message", e.getMessage());
                e.printStackTrace();
            }
            return bimage;
        }
        protected void onPostExecute(Bitmap result) {
            IVPreviewImage.setImageBitmap(result);
            try {
                saveBitmap(result, "raw");
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.paintPhoto();
        }

        private void paintPhoto() {
            canvas = null;
            bitmap = null;
            IVPreviewImage.setDrawingCacheEnabled(true);
            IVPreviewImage.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            IVPreviewImage.layout(0, 0, IVPreviewImage.getMeasuredWidth(), IVPreviewImage.getMeasuredHeight());
            IVPreviewImage.buildDrawingCache(true);
            bitmap = Bitmap.createBitmap(IVPreviewImage.getDrawingCache());
            IVPreviewImage.setDrawingCacheEnabled(false);

            canvas = new Canvas(bitmap);
            paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    private void paintPhoto(){
        canvas = null;
        bitmap = null;
        // Tải ảnh từ ImageView vào bitmap
        IVPreviewImage.setDrawingCacheEnabled(true);
        IVPreviewImage.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        IVPreviewImage.layout(0, 0, IVPreviewImage.getMeasuredWidth(), IVPreviewImage.getMeasuredHeight());
        IVPreviewImage.buildDrawingCache(true);
        bitmap = Bitmap.createBitmap(IVPreviewImage.getDrawingCache());
        IVPreviewImage.setDrawingCacheEnabled(false);

        // Tạo canvas từ bitmap
        canvas = new Canvas(bitmap);

        // Tạo và cấu hình Paint
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.FILL);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}