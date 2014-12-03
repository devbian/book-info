package com.example.qr_codescan;


import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.gson.Gson;
import com.pera.model.Book;


public class MainActivity extends Activity {
    private final static int SCANNIN_GREQUEST_CODE = 1;
    private final static int BOOK_JSON_OK = 1;
    /**
     * 显示扫描结果
     */
    private TextView mTextView;
    /**
     * 显示扫描拍的图片
     */
    private ImageView mImageView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = (TextView) findViewById(R.id.result);
        mImageView = (ImageView) findViewById(R.id.qrcode_bitmap);
        Button btnList = (Button) findViewById(R.id.button2);
        //点击按钮跳转到二维码扫描界面，这里用的是startActivityForResult跳转
        //扫描完了之后调到该界面
        Button mButton = (Button) findViewById(R.id.button1);
        mButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, MipcaActivityCapture.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivityForResult(intent, SCANNIN_GREQUEST_CODE);
            }
        });

        btnList.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, BookListActivity.class);
                startActivity(intent);
            }
        });
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BOOK_JSON_OK:
                    Book book = (Book) msg.obj;
                    mTextView.setText(book.getTitle());
                    ImageLoadedCallback callback = new ImageLoadedCallback() {
                        @Override
                        public void loaded(Bitmap bitMap, String url) {
//                            if (url.equals(mImageView.getTag())) {
                                mImageView.setImageBitmap(bitMap);
//                            }
                        }
                    };
                    String imageUrl = "http://www.baidu.com/img/bd_logo1.png";
                    new DownloadImageTask(callback).execute(imageUrl);
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case SCANNIN_GREQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    String isbn = data.getStringExtra("book"); // json
                    // 处理网络
                    HttpUtil.sendHttpRequest(
                            "http://182.92.186.171:8080/book-info/search/" + isbn,
                            new HttpCallbackListener() {
                                @Override
                                public void onFinish(String response) {
                                    Intent resultIntent = new Intent();
                                    resultIntent.putExtra("book", response);
                                    Gson gson = new Gson();
                                    Book book = gson.fromJson(response, Book.class);
                                    Message message = new Message();
                                    message.what = BOOK_JSON_OK;
                                    message.obj = book;
                                    handler.sendMessage(message);
                                }

                                @Override
                                public void onError(Exception e) {

                                }
                            });
                }
                break;
        }
    }
}
