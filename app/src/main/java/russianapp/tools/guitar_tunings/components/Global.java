package russianapp.tools.guitar_tunings.components;

import android.app.Application;
import android.content.Context;
import android.os.StrictMode;

import androidx.multidex.MultiDex;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import russianapp.tools.guitar_tunings.MainActivity;

public class Global extends Application {

    public static Global instance;

    public MainActivity mainActivity;

    public static Global getInstance() {
        return instance;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");

        // Initialize UCE_Handler Library
        new UCEHandler.Builder(this)
                .setTrackActivitiesEnabled(true)
                .setBackgroundModeEnabled(true)
                .setUCEHEnabled(true)
                .build();
    }

    @Override
    public Context getApplicationContext() {
        return super.getApplicationContext();
    }

    public OkHttpClient getUnsafeOkHttpClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            TrustAllManager x509TrustManager = new TrustAllManager();
            sslContext.init(null, new TrustManager[]{x509TrustManager},
                    new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
         /*   final SSLSocketFactory sslSocketFactory = sslContext
                    .getSocketFactory();*/
            final SSLSocketFactory sslSocketFactory = new Tls12SocketFactory(sslContext
                    .getSocketFactory());

            return new OkHttpClient()
                    .newBuilder()
                    .sslSocketFactory(sslSocketFactory, x509TrustManager)
                    .hostnameVerifier((hostname, session) -> true)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class TrustAllManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    public static class Tls12SocketFactory extends SSLSocketFactory {
        private final String[] TLS_SUPPORT_VERSION = {"TLSv1", "TLSv1.1", "TLSv1.2"};
        private final String[] TLS_SUPPORT_VERSION_OLD = {"TLSv1"};

        final SSLSocketFactory delegate;

        Tls12SocketFactory(SSLSocketFactory delegate) {
            this.delegate = delegate;
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return delegate.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return delegate.getSupportedCipherSuites();
        }

        @Override
        public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
            return patch(delegate.createSocket(s, host, port, autoClose));
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            return patch(delegate.createSocket(host, port));
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
            return patch(delegate.createSocket(host, port, localHost, localPort));
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            return patch(delegate.createSocket(host, port));
        }

        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
            return patch(delegate.createSocket(address, port, localAddress, localPort));
        }

        private Socket patch(Socket s) {
            if (s instanceof SSLSocket)
                try {
                    ((SSLSocket) s).setEnabledProtocols(TLS_SUPPORT_VERSION);
                } catch (Exception e) {
                    ((SSLSocket) s).setEnabledProtocols(TLS_SUPPORT_VERSION_OLD);
                }
            return s;
        }
    }
}