package com.futabooo.fileuploadtest;

import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;

public class MainActivity extends AppCompatActivity {

  private static final String TAG = MainActivity.class.getSimpleName();
  private static final String HTML_FILE_NAME = "sample.html";
  private static final String UTF8 = "UTF-8";
  private static final String TYPE_IMAGE = "image/*";
  private static final int INPUT_FILE_REQUEST_CODE = 1;

  private WebView mWebView;
  private ValueCallback<Uri> mUploadMessage;
  private ValueCallback<Uri[]> mFilePathCallback;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    mWebView = (WebView) findViewById(R.id.activity_main_webview);
    mWebView.setWebChromeClient(MyWebChromeClient());

    // Display the html file in the assets in WebView
    AssetManager assetManager = getAssets();
    try {
      InputStream inputStream = assetManager.open(HTML_FILE_NAME, AssetManager.ACCESS_BUFFER);
      String htmlInString = streamToString(inputStream);
      inputStream.close();
      mWebView.loadDataWithBaseURL(null, htmlInString, "text/html", UTF8, null);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode != INPUT_FILE_REQUEST_CODE) {
      super.onActivityResult(requestCode, resultCode, data);
      return;
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      if (mFilePathCallback == null) {
        super.onActivityResult(requestCode, resultCode, data);
        return;
      }
      Uri[] results = null;

      // Check that the response is a good one
      if (resultCode == RESULT_OK) {
        String dataString = data.getDataString();
        if (dataString != null) {
          results = new Uri[] { Uri.parse(dataString) };
        }
      }

      mFilePathCallback.onReceiveValue(results);
      mFilePathCallback = null;
    } else {
      if (mUploadMessage == null) {
        super.onActivityResult(requestCode, resultCode, data);
        return;
      }

      Uri result = null;

      if (resultCode == RESULT_OK) {
        if (data != null) {
          result = data.getData();
        }
      }

      mUploadMessage.onReceiveValue(result);
      mUploadMessage = null;
    }
  }

  private WebChromeClient MyWebChromeClient() {
    return new WebChromeClient() {
      // For Android < 3.0
      public void openFileChooser(ValueCallback<Uri> uploadFile) {
        openFileChooser(uploadFile, "");
      }

      // For 3.0 <= Android < 4.1
      public void openFileChooser(ValueCallback<Uri> uploadFile, String acceptType) {
        openFileChooser(uploadFile, acceptType, "");
      }

      // For 4.1 <= Android < 5.0
      public void openFileChooser(ValueCallback<Uri> uploadFile, String acceptType, String capture) {
        if(mUploadMessage != null){
          mUploadMessage.onReceiveValue(null);
        }
        mUploadMessage = uploadFile;

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(TYPE_IMAGE);

        startActivityForResult(intent, INPUT_FILE_REQUEST_CODE);
      }

      // For Android 5.0+
      @Override public boolean onShowFileChooser(WebView webView,
          ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
        // Double check that we don't have any existing callbacks
        if (mFilePathCallback != null) {
          mFilePathCallback.onReceiveValue(null);
        }
        mFilePathCallback = filePathCallback;

        // Set up the intent to get an existing image
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(TYPE_IMAGE);
        startActivityForResult(intent, INPUT_FILE_REQUEST_CODE);

        return true;
      }
    };
  }

  private static String streamToString(InputStream in) throws IOException {
    if (in == null) {
      return "";
    }
    Writer writer = new StringWriter();
    char[] buffer = new char[1024];
    try {
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in, UTF8));
      int length = 0;
      while ((length = bufferedReader.read(buffer)) != -1) {
        writer.write(buffer, 0, length);
      }
    } finally {
    }
    return writer.toString();
  }
}
