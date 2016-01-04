package com.futabooo.fileuploadtest;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
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
  private static final int REQUEST_CODE_FROM_JS = 2;

  private WebView mWebView;
  private ValueCallback<Uri> mUploadMessage;
  private ValueCallback<Uri[]> mFilePathCallback;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    mWebView = (WebView) findViewById(R.id.activity_main_webview);
    mWebView.setWebChromeClient(MyWebChromeClient());
    mWebView.addJavascriptInterface(new WebViewJavaScriptInterface(this), "Native");
    mWebView.getSettings().setJavaScriptEnabled(true);

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
    if (requestCode == INPUT_FILE_REQUEST_CODE) {
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
    }else if(requestCode == REQUEST_CODE_FROM_JS){
      Uri selectedImage = data.getData();
      String[] filePathColumn = {MediaStore.Images.Media.DATA};

      // setting file name
      Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
      cursor.moveToFirst();
      int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
//      String imgPath = cursor.getString(columnIndex);
      String imgPath = getPath(this, selectedImage);
      cursor.close();
//      String fileNameSegments[] = imgPath.split("/");
//      String fileName = fileNameSegments[fileNameSegments.length - 1];

      // encode image to string
      BitmapFactory.Options options = null;
      options = new BitmapFactory.Options();
      options.inSampleSize = 3;
      Bitmap bitmap = BitmapFactory.decodeFile(imgPath, options);

      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
      byte[] b = stream.toByteArray();
      String encodedString = Base64.encodeToString(b, Base64.DEFAULT);
//      Map<String, String> params = new HashMap<String, String>();
//      params.put("filename", fileName);
//      params.put("image", encodedString);
      mWebView.loadUrl("javascript:chooseImgResult(" + encodedString + ")");
    }else{
      super.onActivityResult(requestCode,resultCode, data);
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

  public static String getPath(final Context context, final Uri uri) {

    final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

    // DocumentProvider
    if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
      // ExternalStorageProvider
      if (isExternalStorageDocument(uri)) {
        final String docId = DocumentsContract.getDocumentId(uri);
        final String[] split = docId.split(":");
        final String type = split[0];

        if ("primary".equalsIgnoreCase(type)) {
          return Environment.getExternalStorageDirectory() + "/" + split[1];
        }

        // TODO handle non-primary volumes
      }
      // DownloadsProvider
      else if (isDownloadsDocument(uri)) {

        final String id = DocumentsContract.getDocumentId(uri);
        final Uri contentUri = ContentUris.withAppendedId(
                Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

        return getDataColumn(context, contentUri, null, null);
      }
      // MediaProvider
      else if (isMediaDocument(uri)) {
        final String docId = DocumentsContract.getDocumentId(uri);
        final String[] split = docId.split(":");
        final String type = split[0];

        Uri contentUri = null;
        if ("image".equals(type)) {
          contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        } else if ("video".equals(type)) {
          contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        } else if ("audio".equals(type)) {
          contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }

        final String selection = "_id=?";
        final String[] selectionArgs = new String[] {
                split[1]
        };

        return getDataColumn(context, contentUri, selection, selectionArgs);
      }
    }
    // MediaStore (and general)
    else if ("content".equalsIgnoreCase(uri.getScheme())) {

      // Return the remote address
      if (isGooglePhotosUri(uri))
        return uri.getLastPathSegment();

      return getDataColumn(context, uri, null, null);
    }
    // File
    else if ("file".equalsIgnoreCase(uri.getScheme())) {
      return uri.getPath();
    }

    return null;
  }

  /**
   * Get the value of the data column for this Uri. This is useful for
   * MediaStore Uris, and other file-based ContentProviders.
   *
   * @param context The context.
   * @param uri The Uri to query.
   * @param selection (Optional) Filter used in the query.
   * @param selectionArgs (Optional) Selection arguments used in the query.
   * @return The value of the _data column, which is typically a file path.
   */
  public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {

    Cursor cursor = null;
    final String column = "_data";
    final String[] projection = {
            column
    };

    try {
      cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
              null);
      if (cursor != null && cursor.moveToFirst()) {
        final int index = cursor.getColumnIndexOrThrow(column);
        return cursor.getString(index);
      }
    } finally {
      if (cursor != null)
        cursor.close();
    }
    return null;
  }

  /**
   * @param uri The Uri to check.
   * @return Whether the Uri authority is ExternalStorageProvider.
   */
  public static boolean isExternalStorageDocument(Uri uri) {
    return "com.android.externalstorage.documents".equals(uri.getAuthority());
  }

  /**
   * @param uri The Uri to check.
   * @return Whether the Uri authority is DownloadsProvider.
   */
  public static boolean isDownloadsDocument(Uri uri) {
    return "com.android.providers.downloads.documents".equals(uri.getAuthority());
  }

  /**
   * @param uri The Uri to check.
   * @return Whether the Uri authority is MediaProvider.
   */
  public static boolean isMediaDocument(Uri uri) {
    return "com.android.providers.media.documents".equals(uri.getAuthority());
  }

  /**
   * @param uri The Uri to check.
   * @return Whether the Uri authority is Google Photos.
   */
  public static boolean isGooglePhotosUri(Uri uri) {
    return "com.google.android.apps.photos.content".equals(uri.getAuthority());
  }

  /**
   * JavaScript Interface. Web code can access methods in here
   * (as long as they have the @JavascriptInterface annotation)
   */
  public class WebViewJavaScriptInterface {

    private Context context;

    /**
     * Need a reference to the context in order to sent a post message
     */
    public WebViewJavaScriptInterface(Context context) {
      this.context = context;
    }

    @JavascriptInterface
    public void onShowFileChooser() {
      Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
      intent.addCategory(Intent.CATEGORY_OPENABLE);
      intent.setType(TYPE_IMAGE);
      startActivityForResult(intent, REQUEST_CODE_FROM_JS);
    }
  }

}
