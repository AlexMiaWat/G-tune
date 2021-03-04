package russianapp.tools.guitar_tunings.csp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import org.json.simple.parser.JSONParser;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import russianapp.tools.guitar_tunings.BuildConfig;
import russianapp.tools.guitar_tunings.MainActivity;
import russianapp.tools.guitar_tunings.R;
import russianapp.tools.guitar_tunings.components.AsyncMethods;
import russianapp.tools.guitar_tunings.components.DefaultErrorActivity;
import russianapp.tools.guitar_tunings.components.PreferencesManager;
import russianapp.tools.guitar_tunings.components.UrlBro;

public class CentralServicePortalManager {

    public Activity activity;
    public static String serviceProvider, UrlPath;

    public String application = "Guitar_Tuning";
    public String requestLink;
    Context context;
    public MainActivity mainActivity;

    String cspToken = "", headerDate, ContentType = "text/html; charset=utf-8", content = "";
    String lastVersion = String.valueOf(BuildConfig.VERSION_CODE), exception = "";
    Map<String, String> mapAppInfo = new HashMap<>();
    Map<String, String> mapDeviceInfo = new HashMap<>();

    public static final String CSP_SERVICE_PROVIDER = "CSP_SERVICE_PROVIDER";

    public CentralServicePortalManager(Context context) {
        this.context = context;
        UrlPath = "http://yourebay.ucoz.kz/UKA_PORTAL";
        requestLink = "/CSP/hs/app/connections/";
    }

    public static int AddDataToGo(Context context //, ArrayList<ItemDetails> itemDetails
    ) {

//        Global global = (Global) context.getApplicationContext();
//
//        if (!global.mainActivity.transactionEnded)
//            return 0;
//        else
//            global.mainActivity.transactionEnded = false;
//
//        for (ItemDetails item : itemDetails) {
//            item.table = item.sourceStructure.section;
//
//            try {
//                global.mainActivity.dataToGo.remove(item);
//            } catch (Exception ignored) {
//            }
//
//            // if (item.getSmallImage() != null && (!item.getFullImage().equals("nothing") || !item.getSmallImage().equals("nothing")))
//            global.mainActivity.dataToGo.add(item);
//        }
//
//        global.mainActivity.transactionEnded = true;
//
//        return global.mainActivity.dataToGo.size();
        return 0;
    }

    public static void getLastPackage(Context context, String packageName) {

        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(Intent.ACTION_VIEW);
        Uri uri = Uri.parse("market://details?id=" + packageName);
        intent.setData(uri);
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Log.println(Log.ERROR, "CSP", e.toString());
            e.printStackTrace();

            uri = Uri.parse("https://play.google.com/store/apps/details?id=" + packageName);
            intent.setData(uri);
            context.startActivity(intent);
        }
    }

    static String isNull(String o, String f) {
        if (o == null)
            return f;
        else
            return o;
    }

    static String getISODate(Date d) {
        if (d == null) {
            Calendar now = Calendar.getInstance();
            d = now.getTime();
        }
        String pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'";
        DateFormat df = new SimpleDateFormat(pattern, Locale.UK);
        return df.format(d);
    }

    // mdd5 for Item
    public static String mdd5(String s, String source) {
        return source + "_" + mdd5(s);
    }

    // hash function
    public static String mdd5(String st) {
        MessageDigest messageDigest;
        byte[] digest;

        try {
            messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.reset();
            messageDigest.update(st.getBytes());
            digest = messageDigest.digest();
        } catch (NoSuchAlgorithmException e) {
            // тут можно обработать ошибку
            // возникает она если в передаваемый алгоритм в getInstance(,,,) не существует
            e.printStackTrace();
            return st;
        }

        BigInteger bigInt = new BigInteger(1, digest);
        StringBuilder md5Hex = new StringBuilder(bigInt.toString(16));

        while (md5Hex.length() < 32) {
            md5Hex.insert(0, "0");
        }

        return md5Hex.toString();
    }

    public static String SetCSP_Properties(Context context) {
        final UrlBro ub = new UrlBro();
        String fileName = "csp_service.xml";
        int xmlResName = R.xml.csp_service;
        XmlPullParser parser;

        try {
            // Загружаем категории
            // Проверяем дату файла и скачиваем категории 1 раз в 5 дней
            File file = new File(context.getCacheDir() + "/" + fileName);
            Date lastModDate = new Date(file.lastModified());
            long diff = System.currentTimeMillis() - lastModDate.getTime();
            long seconds = diff / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;

            if ((!file.exists() || (days >= 5)))
                ub.ShortDownloadFileFromURL(
                        UrlPath + "/" + fileName,
                        context.getCacheDir() + "/" + fileName);

            File myXML = new File(fileName); // give proper path
            if (myXML.length() > 150) {
                FileInputStream fis = new FileInputStream(myXML);

                // from file
                XmlPullParserFactory pullParserFactory = XmlPullParserFactory.newInstance();
                pullParserFactory.setNamespaceAware(true);
                parser = pullParserFactory.newPullParser();
                parser.setInput(fis, null);
            } else
                // from resource
                parser = context.getResources().getXml(xmlResName);

            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.getEventType() != XmlPullParser.START_TAG)
                    continue;

                String name = parser.getName();
                if (name != null) {
                    if (name.equals("csp"))
                        while (parser.next() != XmlPullParser.END_TAG) {
                            if (parser.getEventType() == XmlPullParser.START_TAG) {
                                name = parser.getName();
                                if (name != null) {
                                    switch (name) {
                                        case "PATH":
                                            parser.require(XmlPullParser.START_TAG, null, "PATH");
                                            if (parser.next() == XmlPullParser.TEXT) {
                                                String CSP_serviceProvider = parser.getText();
                                                PreferencesManager.setStringPreference(context, CSP_SERVICE_PROVIDER, CSP_serviceProvider);
                                            }
                                            break;

                                        case "abs":
                                            break;
                                    }
                                    parser.nextTag();
                                }
                            }
                        }
                }
            }

        } catch (Exception ignored) {
        }

        return PreferencesManager.getStringPreference(context, CSP_SERVICE_PROVIDER, "https://alexmia-rusappclub.vpnki.ru");
    }

    public void doServiceTask(String action, String comment) {
        doTask doTask = new doTask();
        // parallel executing
        AsyncMethods.execute(doTask, true, action, comment);
    }

    public void firstConnectionStart(String comment) {
        try {
            String currentLink = requestLink + "firstConnection?application=" + application;
            currentLink += addParamsToHttpString();
            currentLink += "&comment=" + comment;

            String result = firstConnection(currentLink);

            // Update app
            ((MainActivity) context).isItLastAppVersion = Integer.parseInt(lastVersion) == Integer.parseInt(mapAppInfo.get("appVersion"));

        } catch (Exception e) {
            exception = e.getMessage();
            Log.println(Log.ERROR, "CSP", e.toString());
            e.printStackTrace();
        }
    }

    public void errorRegistryStart(String comment) {
        try {
            String currentLink = requestLink + "errorRegistry?application=" + application;
            currentLink += addParamsToHttpString();
            currentLink += "&comment=" + "no comment";

            String result = errorRegistry(currentLink, comment);

        } catch (Exception e) {
            exception = e.getMessage();
            Log.println(Log.ERROR, "CSP", e.toString());
            e.printStackTrace();
        }
    }

    public void putDataStart(String data, String comment) {
        try {
            String currentLink = requestLink + "putData?application=" + application;
            currentLink += addParamsToHttpString();
            currentLink += "&token=" + ((MainActivity) context).token;
            currentLink += "&comment=" + "putting data: " + comment;

            String result = putData(currentLink, data);

            if (result.contains("putData | error"))
                firstConnectionStart("reconnection");

        } catch (Exception e) {
            exception = e.getMessage();
            Log.println(Log.ERROR, "CSP", e.toString());
            e.printStackTrace();
        }
    }

    public String getData(String requestLink) {

        boolean result = false;
        try {
            Connection.Response response = Jsoup.connect(requestLink)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36")
                    .ignoreContentType(true)
                    .method(Connection.Method.GET)
                    .header("cspToken", mdd5(cspToken))
                    .execute();

            response.charset("UTF-8");
            result = (response.statusCode() == 200);
            headerDate = response.header("Date");
            ContentType = response.contentType();
            content = response.body();

            if (result && ContentType.contains("json")) {
                JSONParser parser = new JSONParser();
                org.json.simple.JSONObject item = (org.json.simple.JSONObject) parser.parse(content);

                result = Boolean.parseBoolean(Objects.requireNonNull(item.get("result")).toString());
            }
        } catch (Exception e) {
            Log.println(Log.ERROR, "CSP", e.toString());
            e.printStackTrace();
        }

        if (result)
            return content;
        else
            return "getData | error:" + content;
    }

    String putData(String requestLink, String postingData) {

        boolean result = false;
        try {
            Connection.Response response = Jsoup.connect(requestLink)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36")
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .method(Connection.Method.POST)
                    .header("cspToken", mdd5(cspToken))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .requestBody(postingData)
                    .maxBodySize(1_000_000 * 5) // 5 mb ~
                    .execute();

            response.charset("UTF-8");
            result = (response.statusCode() == 200);
            headerDate = response.header("Date");
            ContentType = response.contentType();
            content = response.body();

            if (result && ContentType.contains("json")) {
                JSONParser parser = new JSONParser();
                org.json.simple.JSONObject item = (org.json.simple.JSONObject) parser.parse(content);

                result = Boolean.parseBoolean(Objects.requireNonNull(item.get("result")).toString());
                //context.token = Objects.requireNonNull(Objects.requireNonNull(item.get("token"))).toString();
            }
        } catch (Exception e) {
            Log.println(Log.ERROR, "CSP", e.toString());
            e.printStackTrace();
        }

        if (result)
            return content;
        else
            return "putData | error:" + content;
    }

    String postAction(String requestLink) {

        String postingData = "";

        boolean result = false;
        try {
            Connection.Response response = Jsoup.connect(requestLink)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36")
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .method(Connection.Method.POST)
                    .header("cspToken", mdd5(cspToken))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .requestBody(postingData)
                    .maxBodySize(1_000_000 * 5) // 5 mb ~
                    .execute();

            response.charset("UTF-8");
            result = (response.statusCode() == 200);
            headerDate = response.header("Date");
            ContentType = response.contentType();
            content = response.body();

            if (result && ContentType.contains("json")) {
                JSONParser parser = new JSONParser();
                org.json.simple.JSONObject item = (org.json.simple.JSONObject) parser.parse(content);

                result = Boolean.parseBoolean(Objects.requireNonNull(item.get("result")).toString());
                //context.token = Objects.requireNonNull(Objects.requireNonNull(item.get("token"))).toString();
            }
        } catch (Exception e) {
            Log.println(Log.ERROR, "CSP", e.toString());
            e.printStackTrace();
        }

        if (result)
            return content;
        else
            return "postAction | error:" + content;
    }

    String getCspToken(Map<String, String> map0, Map<String, String> map) {

        String result = "";
        result += map0.get("serverHour");
        result += map.get("sdk");
        result += map.get("release");
        result += map.get("brand");
        result += map.get("device");
        result += map.get("model");
        result += map.get("manufacturer");
        result += map.get("product");
        result += map.get("board");
        result += map.get("display");
        result += map.get("hardware");
        result += map.get("host");
        result += map.get("id");
        return result;
    }

    String firstConnection(String requestLink) {

        if (mainActivity.token.length() == 0) {
            serviceProvider = SetCSP_Properties(mainActivity);
        }

        String Link = serviceProvider + requestLink;

        boolean result = false;
        try {
            Connection.Response response = Jsoup.connect(Link)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36")
                    .ignoreContentType(true)
                    .method(Connection.Method.GET)
                    .header("cspToken", mdd5(cspToken))
                    .header("cspTokenDebug", cspToken)
                    .execute();

            response.charset("UTF-8");
            result = (response.statusCode() == 200);
            headerDate = response.header("Date");
            ContentType = response.contentType();
            content = response.body();

            if (result && ContentType.contains("json")) {
                JSONParser parser = new JSONParser();
                org.json.simple.JSONObject item = (org.json.simple.JSONObject) parser.parse(content);

                result = Boolean.parseBoolean(Objects.requireNonNull(item.get("result")).toString());
                String action = Objects.requireNonNull(Objects.requireNonNull(item.get("action"))).toString();
                String serverDate = Objects.requireNonNull(Objects.requireNonNull(item.get("serverDate"))).toString();
                ((MainActivity) context).token = Objects.requireNonNull(Objects.requireNonNull(item.get("token"))).toString();
                lastVersion = Objects.requireNonNull(Objects.requireNonNull(item.get("lastVersion"))).toString();
            }
        } catch (Exception e) {
            Log.println(Log.ERROR, "CSP", e.toString());
            e.printStackTrace();
        }

        if (result)
            return "firstConnection | token:" + ((MainActivity) context).token + " | lastVersion: " + lastVersion;
        else return "firstConnection | error:" + content;
    }

    String errorRegistry(String requestLink, String postingData) {

        boolean result = false;

        serviceProvider = PreferencesManager.getStringPreference(context, CSP_SERVICE_PROVIDER, "https://alexmia-rusappclub.vpnki.ru");
        String Link = serviceProvider + requestLink;

        try {
            Connection.Response response = Jsoup.connect(Link)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36")
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .method(Connection.Method.PUT)
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .requestBody(postingData)
                    .maxBodySize(1_000_000 * 5) // 5 mb ~
                    .execute();

            response.charset("UTF-8");
            result = (response.statusCode() == 200);
            headerDate = response.header("Date");
            ContentType = response.contentType();
            content = response.body();

            if (result && ContentType.contains("json")) {
                JSONParser parser = new JSONParser();
                org.json.simple.JSONObject item = (org.json.simple.JSONObject) parser.parse(content);

                result = Boolean.parseBoolean(Objects.requireNonNull(item.get("result")).toString());
                String action = Objects.requireNonNull(Objects.requireNonNull(item.get("action"))).toString();
                String serverDate = Objects.requireNonNull(Objects.requireNonNull(item.get("serverDate"))).toString();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        DefaultErrorActivity.startApp(activity);

        if (result)
            return "errorRegistry OK!";
        else return "errorRegistry | error:" + content;
    }

    public String addParamsToHttpString() {
        String currentLink = "";

        mapAppInfo = IdentificationData.getAppInfo(context);
        currentLink += IdentificationData.paramsToString(mapAppInfo);

        mapDeviceInfo = IdentificationData.getDeviceInfo();
        currentLink += IdentificationData.paramsToString(mapDeviceInfo);

        cspToken = getCspToken(mapAppInfo, mapDeviceInfo);

        return currentLink;
    }

    public String generatePostingData(int countToSent) {
//        ArrayList<ItemDetails> dataToGo = new ArrayList<>();
//
//        JSONObject jsonObject = new JSONObject();
//        jsonObject.put("object", "material");
//
//        JSONArray data = new JSONArray();
//        for (ItemDetails item : ((MainActivity) context).dataToGo) {
//
//            dataToGo.add(item);
//
//            JSONObject jsonLine = new JSONObject();
//            jsonLine.put("table", isNull(item.table, "Ads"));
//            jsonLine.put("id", item.getId().trim());
//            jsonLine.put("user", "");
//            jsonLine.put("itemDescription", isNull(item.getItemDescription(), ""));
//            jsonLine.put("itemPreview", isNull(item.itemPreview, ""));
//            jsonLine.put("itemTitle", isNull(item.itemTitle, ""));
//            jsonLine.put("itemLink", isNull(item.itemLink, ""));
//            jsonLine.put("price", isNull(item.getPrice(), ""));
//            jsonLine.put("phone", isNull(item.getPhone(), ""));
//            jsonLine.put("date", getISODate(item.getDate()));
//            jsonLine.put("smallImage", isNull(item.getSmallImage(), ""));
//            jsonLine.put("fullImage", isNull(item.getFullImage(), ""));
//            jsonLine.put("Category_id", item.parent);
//            jsonLine.put("source", isNull(item.source, ""));
//            jsonLine.put("enabled", true);
//            jsonLine.put("favorites", false);
//            jsonLine.put("favoriteDate", getISODate(item.favoriteDate));
//            jsonLine.put("tags", isNull(item.tags, ""));
//
//            data.add(jsonLine);
//
//            if (dataToGo.size() > 200)
//                break;
//        }
//
//        if (dataToGo.size() < countToSent)
//            return "";
//
//        jsonObject.put("data", data);
//
//        ((MainActivity) context).dataToGo.removeAll(dataToGo);
//        return jsonObject.toJSONString();

        return "";
    }

    @SuppressLint("StaticFieldLeak")
    class doTask extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            String action = params[0];
            String comment = params[1];
            content = "";
            exception = "";
            double startTime = System.nanoTime();

            if (action.contains("firstConnection"))
                firstConnectionStart(comment);

            if (action.contains("tuneString") || action.contains("tuneProperties") || action.contains("languageSelected")) {
                String currentLink = serviceProvider + requestLink + "postAction?application=" + application;
                currentLink += addParamsToHttpString();
                currentLink += "&token=" + ((MainActivity) context).token;
                currentLink += "&comment=" + "Action: " + comment;
                currentLink += "&action=" + action;
                postAction(currentLink);
            }

            if (action.contains("errorRegistry"))
                errorRegistryStart(comment);

            double endTime = System.nanoTime();
            double duration = (endTime - startTime) / 1000000 / 1000;
            DecimalFormat df = new DecimalFormat("#.####");
            //return String.format("%s processing time: %s seconds", action, df.format(duration));

            return action;
        }

        protected void onPostExecute(String action) {
        }

    }

}