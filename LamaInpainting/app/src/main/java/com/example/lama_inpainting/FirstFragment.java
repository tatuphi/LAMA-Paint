package com.example.lama_inpainting;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.Manifest.permission_group.CAMERA;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.lama_inpainting.databinding.FragmentFirstBinding;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class FirstFragment extends Fragment implements View.OnTouchListener {

    private FragmentFirstBinding binding;
    ImageView IVPreviewImage;
    private Bitmap bitmap;
    private Canvas canvas;
    private Paint paint;
    private ArrayList<Point> touchPoints;

    EditText ETInputPhotoUrl;
    String userChoosenTask = "";

    private static final int WRITE_FILE = 1887;
    private static final int REQUEST_CAMERA = 1888;
    private static final int SELECT_FILE = 1889;

    private static final int PERMISSION_REQUEST_CODE = 200;


    String[] perms = {"android.permission.READ_EXTERNAL_STORAGE", "android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};

    int permsRequestCode = 200;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        IVPreviewImage = view.findViewById(R.id.IVPreviewImage);
        ETInputPhotoUrl = view.findViewById(R.id.et_input_url);
        touchPoints = new ArrayList<>();

        if(!checkPermission()){
//            requestPermission();
            requestPermissions(perms, permsRequestCode);
        }

        binding.buttonFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Bitmap newBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                    Canvas newCanvas = new Canvas(newBitmap);

                    // Vẽ lại các điểm đã tô màu
                    for (Point point : touchPoints) {
                        newCanvas.drawCircle(point.x, point.y, 30, paint);
                    }

                    saveBitmap(newBitmap);
//                    saveBitmap(bitmap);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        binding.buttonPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectImage(view);
//                NavHostFragment.findNavController(FirstFragment.this)
//                        .navigate(R.id.action_FirstFragment_to_SecondFragment);
            }
        });

        binding.buttonSelect.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                new DownloadImageFromInternet((ImageView) IVPreviewImage).execute(ETInputPhotoUrl.getText().toString());
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
                canvas.drawCircle(x, y, 30, paint);
                IVPreviewImage.setImageBitmap(bitmap);

                return true;
            }
        });

    }

    private void saveBitmap(Bitmap bitmap) throws IOException {
        // Tạo đường dẫn và tên file
        String filePath = Environment.getExternalStorageDirectory() + "/MyAppFolder/";
        String fileName = "colored_image.jpg";

        // Tạo thư mục nếu chưa tồn tại
        File directory = new File(filePath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Tạo đối tượng tệp tin
        File file = new File(filePath + fileName);

        try {
            // Tạo đối tượng FileOutputStream để ghi dữ liệu vào tệp tin
            FileOutputStream fos = new FileOutputStream(file);

            // Nén và ghi dữ liệu bitmap vào tệp tin
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);

            // Đóng luồng
            fos.close();

            // Cập nhật MediaStore để hệ thống biết có một tệp mới
//            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
//            intent.setData(Uri.fromFile(file));
//            sendBroadcast(intent);

            Toast.makeText(getActivity(), "Đã lưu hình ảnh", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "Lỗi khi lưu hình ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    private void selectImage(View v) {
        final CharSequence[] items = { "Take Photo", "Choose from Library",
                "Cancel" };
        new AlertDialog.Builder( v.getContext())
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
                onCaptureImageResult(data);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void onSelectFromGalleryResult(Intent data) {
        Bitmap bm=null;
        if (data != null) {
            try {
                bm = MediaStore.Images.Media.getBitmap(getActivity().getApplicationContext().getContentResolver(), data.getData());

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
//        ivImage.setImageBitmap(bm);
        IVPreviewImage.setImageBitmap(bm);

        Log.e("error", "herhe i am");

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

    private void onCaptureImageResult(Intent data) {
        Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
        File destination = new File(Environment.getExternalStorageDirectory(),
                System.currentTimeMillis() + ".jpg");
        FileOutputStream fo;
        try {
            destination.createNewFile();
            fo = new FileOutputStream(destination);
            fo.write(bytes.toByteArray());
            fo.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
//        ivImage.setImageBitmap(thumbnail);
        IVPreviewImage.setImageBitmap(thumbnail);
    }
    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), READ_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), CAMERA);
        int result2 = ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), WRITE_EXTERNAL_STORAGE);

        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED && result2 == PackageManager.PERMISSION_GRANTED;
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

//                        Snackbar.make(view, "Permission Denied, You cannot access location data and camera.", Snackbar.LENGTH_LONG).show();

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
        ImageView imageView;
        public DownloadImageFromInternet(ImageView imageView) {
            this.imageView=imageView;
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
            imageView.setImageBitmap(result);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}