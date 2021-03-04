package russianapp.tools.guitar_tunings.components;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Objects;

public class UrlBro {

    // Progress Dialog
    private ProgressDialog pDialog;
    private static final int progress_bar_type = 0;
    public Context context;

    private void onCreateDialog(int id) {
        if (id == progress_bar_type) { // we set this to 0
            pDialog = new ProgressDialog(context);
            pDialog.setMessage("Downloading file. Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setMax(100);
            pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            pDialog.setCancelable(true);
            pDialog.show();
        }
    }

    public static boolean exists(String URLName) {
        try {
            HttpURLConnection.setFollowRedirects(false);
            HttpURLConnection con = (HttpURLConnection) new URL(URLName).openConnection();
            //con.setInstanceFollowRedirects(false);
            con.setRequestMethod("HEAD");
            con.setConnectTimeout(5000); //set timeout to 5 seconds
            con.setReadTimeout(5000);
            con.setRequestProperty("Accept-Encoding", "");
            return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean ShortDownloadFileFromURL(String from, String to) {
        int count;
        // 1
//        try {
//            URL url = new URL(from);
//
//            URLConnection connection = url.openConnection();
//            connection.setConnectTimeout(2000);
//            connection.setReadTimeout(3000);
//            connection.connect();
//            InputStream input = connection.getInputStream();
//
//            //InputStream input = new BufferedInputStream(url.openStream(), 1024);
//            // Output stream
//            OutputStream output = new FileOutputStream(to);
//            byte[] data = new byte[1024];
//            while ((count = input.read(data)) != -1) {
//                output.write(data, 0, count);
//            }
//            output.flush();
//            output.close();
//            input.close();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }

        // 2
//        try (BufferedInputStream in = new BufferedInputStream(new URL(from).openStream());
//             FileOutputStream fileOutputStream = new FileOutputStream(to)) {
//            byte[] dataBuffer = new byte[1024];
//            int bytesRead;
//            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
//                fileOutputStream.write(dataBuffer, 0, bytesRead);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }

        // 3. Загрузка с известным типом файла
        try {
            Connection.Response response = Jsoup.connect(from)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36")
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .method(Connection.Method.GET)
                    .header("Content-Type", "application/xml")
                    .maxBodySize(1_000_000 * 5) // 5 mb ~
                    .execute();

            String body = response.body();

            OutputStream output = new FileOutputStream(to);
            output.write(body.getBytes(Charset.forName("UTF-8")));
            output.flush();
            output.close();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    //Background Async Task to download file
    class DownloadFileFromURL extends AsyncTask<String, String, String> {

        public DownloadFileFromURL() {

        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            onCreateDialog(progress_bar_type);
        }

        // Downloading file in background thread
        @Override
        protected String doInBackground(String... f_url) {
            int count;
            try {
                URL url = new URL(f_url[0]);
                URLConnection conection = url.openConnection();
                conection.connect();

                // this will be useful so that you can show a tipical 0-100%
                // progress bar
                int lenghtOfFile = conection.getContentLength();

                // download the file
                InputStream input = new BufferedInputStream(url.openStream(),
                        8192);

                // Output stream
                OutputStream output = new FileOutputStream(Environment
                        .getExternalStorageDirectory().toString()
                        + "/2011.kml");

                byte[] data = new byte[1024];

                long total = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                    publishProgress("" + (int) ((total * 100) / lenghtOfFile));

                    // writing data to file
                    output.write(data, 0, count);
                }

                // flushing output
                output.flush();

                // closing streams
                output.close();
                input.close();

            } catch (Exception e) {
                Log.e("Error: ", Objects.requireNonNull(e.getMessage()));
            }

            return null;
        }

        // Updating progress bar
        protected void onProgressUpdate(String... progress) {
            // setting progress percentage
            pDialog.setProgress(Integer.parseInt(progress[0]));
        }

        //After completing background task Dismiss the progress dialog
        @Override
        protected void onPostExecute(String file_url) {
            // dismiss the dialog after the file was downloaded
            pDialog.dismiss();
        }
    }
}
