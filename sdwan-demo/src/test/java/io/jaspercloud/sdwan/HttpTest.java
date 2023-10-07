//package io.jaspercloud.sdwan;
//
//import okhttp3.OkHttpClient;
//import okhttp3.Request;
//import okhttp3.Response;
//
//public class HttpTest {
//
//    public static void main(String[] args) throws Exception {
//        OkHttpClient okHttpClient = new OkHttpClient();
//        Request request = new Request.Builder()
//                .url("http://192.222.8.153:8848")
//                .build();
//        Response response = okHttpClient.newCall(request).execute();
//        int code = response.code();
//        String text = new String(response.body().bytes());
//        System.out.println();
//    }
//}
