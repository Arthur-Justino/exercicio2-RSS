package br.ufpe.cin.if1001.rss.services;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import br.ufpe.cin.if1001.rss.db.SQLiteRSSHelper;
import br.ufpe.cin.if1001.rss.domain.ItemRSS;
import br.ufpe.cin.if1001.rss.util.ParserRSS;

public class DownloadXmlService extends IntentService {

    SQLiteRSSHelper db;
    public static final String DOWNLOAD_COMPLETE = "br.ufpe.cin.if1001.rss.action.DOWNLOAD_COMPLETE";
    public static final String NEW_REPORT_AVAILABLE = "br.ufpe.cin.if1001.rss.NEW_REPORTS";

    public DownloadXmlService() {
        super("DownloadXmlService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        db = SQLiteRSSHelper.getInstance(getApplicationContext());
        boolean flag = false;
        List<ItemRSS> items = null;
        try {
            String feed = getRssFeed(intent.getStringExtra("url"));
            items = ParserRSS.parse(feed);
            for (ItemRSS i : items) {
                Log.d("DB", "Buscando o link: " + i.getLink());
                ItemRSS item = db.getItemRSS(i.getLink());
                if (item == null) {
                    //broadcast para noticia nova
                    sendBroadcast(new Intent(NEW_REPORT_AVAILABLE));
                    Log.d("DB", "Encontrado pela primeira vez: " + i.getTitle());
                    //insere no banco
                    db.insertItem(i);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            flag = true;
        } catch (XmlPullParserException e) {
            e.printStackTrace();
            flag = true;
        }

        if (flag) {
            Log.d("FEED", "Erro ao tentar carregar o feed");
        } else {
            Log.d("FEED", "Sucesso");

            //broadcast
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(DOWNLOAD_COMPLETE));
        }
    }
    private String getRssFeed(String feed) throws IOException {
        InputStream in = null;
        String rssFeed = "";
        try {
            URL url = new URL(feed);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            in = connection.getInputStream();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            for (int i; (i = in.read(buffer)) != -1; ) {
                output.write(buffer, 0, i);
            }
            byte[] answer = output.toByteArray();
            rssFeed = new String(answer, "UTF-8");
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return rssFeed;
    }
}
